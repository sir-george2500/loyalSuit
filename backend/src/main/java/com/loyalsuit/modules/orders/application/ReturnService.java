package com.loyalsuit.modules.orders.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.inventory.application.StockService;
import com.loyalsuit.modules.orders.application.dto.CreateReturnRequest;
import com.loyalsuit.modules.orders.application.dto.ReturnResponse;
import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.OrderItem;
import com.loyalsuit.modules.orders.domain.OrderStatus;
import com.loyalsuit.modules.orders.domain.PaymentStatus;
import com.loyalsuit.modules.orders.domain.ReturnRequest;
import com.loyalsuit.modules.orders.domain.ReturnStatus;
import com.loyalsuit.modules.orders.domain.port.OrderItemRepository;
import com.loyalsuit.modules.orders.domain.port.OrderRepository;
import com.loyalsuit.modules.orders.domain.port.ReturnRequestRepository;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Returns / refunds. A guest requests a return against a delivered order, verified
 * by order number + the checkout email (enumeration-safe, like tracking). An admin
 * approves — which refunds the order (REFUNDED, payment REFUNDED) and restocks the
 * items in one transaction — or rejects with a note.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReturnService {

    private final TenantRepository tenantRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReturnRequestRepository returnRepository;
    private final StockService stockService;

    @Transactional
    public ReturnResponse requestReturn(String storeSlug, String orderNumber, CreateReturnRequest request) {
        Tenant tenant = tenantRepository.findBySlug(storeSlug)
                .filter(Tenant::isActive)
                .orElseThrow(() -> new NotFoundException("Order", orderNumber));

        Order order = orderRepository.findByOrderNumberAndTenantId(orderNumber, tenant.getId())
                .filter(o -> emailMatches(o, request.getEmail()))
                .orElseThrow(() -> new NotFoundException("Order", orderNumber));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException("Only delivered orders can be returned");
        }
        if (returnRepository.existsByOrderIdAndStatus(order.getId(), ReturnStatus.REQUESTED)) {
            throw new ConflictException("A return is already pending for this order");
        }

        ReturnRequest ret = new ReturnRequest(
                tenant.getId(), order.getId(), order.getOrderNumber(),
                request.getReason().trim(), order.getCustomerEmail());
        return ReturnResponse.from(returnRepository.save(ret));
    }

    public PageResponse<ReturnResponse> list(UUID tenantId, ReturnStatus status, Pageable pageable) {
        var page = status == null
                ? returnRepository.findByTenantId(tenantId, pageable)
                : returnRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        return new PageResponse<>(page.map(ReturnResponse::from));
    }

    @Transactional
    public ReturnResponse approve(UUID returnId, UUID tenantId) {
        ReturnRequest ret = loadPending(returnId, tenantId);

        Order order = orderRepository.findByIdAndTenantId(ret.getOrderId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Order", ret.getOrderId()));
        if (!order.getStatus().canTransitionTo(OrderStatus.REFUNDED)) {
            throw new BusinessException("This order can no longer be refunded");
        }

        order.setStatus(OrderStatus.REFUNDED);
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        orderRepository.save(order);

        // Returned goods go back into stock.
        for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
            stockService.release(tenantId, item.getProductId(), item.getVariantId(), item.getQuantity());
        }

        ret.setStatus(ReturnStatus.APPROVED);
        return ReturnResponse.from(returnRepository.save(ret));
    }

    @Transactional
    public ReturnResponse reject(UUID returnId, UUID tenantId, String note) {
        ReturnRequest ret = loadPending(returnId, tenantId);
        ret.setStatus(ReturnStatus.REJECTED);
        ret.setResolutionNote(note);
        return ReturnResponse.from(returnRepository.save(ret));
    }

    private ReturnRequest loadPending(UUID returnId, UUID tenantId) {
        ReturnRequest ret = returnRepository.findByIdAndTenantId(returnId, tenantId)
                .orElseThrow(() -> new NotFoundException("Return", returnId));
        if (ret.getStatus() != ReturnStatus.REQUESTED) {
            throw new ConflictException("This return has already been " + ret.getStatus());
        }
        return ret;
    }

    private static boolean emailMatches(Order order, String email) {
        return order.getCustomerEmail() != null && email != null
                && order.getCustomerEmail().trim().equalsIgnoreCase(email.trim());
    }
}
