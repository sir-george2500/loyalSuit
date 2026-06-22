package com.loyalsuit.modules.customers.application;

import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.customers.application.dto.CustomerResponse;
import com.loyalsuit.modules.orders.domain.CustomerOrderStat;
import com.loyalsuit.modules.orders.domain.port.OrderRepository;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read model over a tenant's storefront customers (app users with the CUSTOMER role),
 * enriched with each customer's order count and lifetime spend.
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final AppUserRepository appUserRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> list(UUID tenantId, String search, Pageable pageable) {
        String term = (search == null || search.isBlank()) ? null : search.trim();
        Page<AppUser> page = appUserRepository.findCustomers(tenantId, term, pageable);

        List<UUID> ids = page.getContent().stream().map(AppUser::getId).toList();
        Map<UUID, CustomerOrderStat> stats = orderRepository.aggregateOrdersByCustomer(tenantId, ids).stream()
                .collect(Collectors.toMap(CustomerOrderStat::customerId, Function.identity()));

        return new PageResponse<>(page.map(u -> {
            CustomerOrderStat s = stats.get(u.getId());
            return new CustomerResponse(
                    u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.isActive(), u.getCreatedAt(),
                    s == null ? 0L : s.orderCount(),
                    s == null ? BigDecimal.ZERO : s.totalSpent());
        }));
    }
}
