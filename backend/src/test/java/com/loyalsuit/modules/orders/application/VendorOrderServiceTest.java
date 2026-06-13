package com.loyalsuit.modules.orders.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import com.loyalsuit.modules.orders.application.dto.VendorOrderResponse;
import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.OrderItem;
import com.loyalsuit.modules.orders.domain.port.OrderItemRepository;
import com.loyalsuit.modules.orders.domain.port.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorOrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductRepository productRepository;

    private VendorOrderService service;

    private UUID tenantId;
    private UUID vendorId;
    private UUID orderId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        service = new VendorOrderService(orderRepository, orderItemRepository, productRepository);
        tenantId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    private Order order() {
        Order o = new Order();
        ReflectionTestUtils.setField(o, "id", orderId);
        o.setOrderNumber("LS-1001");
        o.setCustomerName("Casey Customer");
        o.setCurrency("RWF");
        return o;
    }

    private OrderItem line(BigDecimal total) {
        OrderItem i = new OrderItem();
        i.setOrderId(orderId);
        i.setProductId(productId);
        i.setVendorId(vendorId);
        i.setQuantity(2);
        i.setUnitPrice(BigDecimal.valueOf(5000));
        i.setTotal(total);
        return i;
    }

    private Product product() {
        Product p = new Product(tenantId, "Brake Pad", "brake-pad", BigDecimal.valueOf(5000));
        ReflectionTestUtils.setField(p, "id", productId);
        return p;
    }

    @Test
    void list_returnsOnlyTheVendorsLines_withNameAndSubtotal() {
        // Arrange
        when(orderRepository.findOrdersForVendor(eq(tenantId), eq(vendorId), any()))
                .thenReturn(new PageImpl<>(List.of(order())));
        when(orderItemRepository.findByOrderIdInAndVendorId(anyCollection(), eq(vendorId)))
                .thenReturn(List.of(line(BigDecimal.valueOf(10000))));
        when(productRepository.findByIdInAndTenantId(any(), eq(tenantId))).thenReturn(List.of(product()));

        // Act
        List<VendorOrderResponse> result = service.list(tenantId, vendorId, PageRequest.of(0, 20)).getContent();

        // Assert
        assertThat(result).hasSize(1);
        VendorOrderResponse o = result.get(0);
        assertThat(o.orderNumber()).isEqualTo("LS-1001");
        assertThat(o.vendorSubtotal()).isEqualByComparingTo("10000");
        assertThat(o.items()).singleElement()
                .satisfies(i -> assertThat(i.productName()).isEqualTo("Brake Pad"));
    }

    @Test
    void get_returnsTheOrder_whenTheVendorHasLinesOnIt() {
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order()));
        when(orderItemRepository.findByOrderIdInAndVendorId(List.of(orderId), vendorId))
                .thenReturn(List.of(line(BigDecimal.valueOf(10000))));
        when(productRepository.findByIdInAndTenantId(any(), eq(tenantId))).thenReturn(List.of(product()));

        assertThat(service.get(tenantId, vendorId, orderId).orderNumber()).isEqualTo("LS-1001");
    }

    @Test
    void get_404s_whenTheVendorHasNoLinesOnTheOrder() {
        // The order exists but holds nothing this vendor sold — must read as not-found.
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order()));
        when(orderItemRepository.findByOrderIdInAndVendorId(List.of(orderId), vendorId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.get(tenantId, vendorId, orderId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void get_404s_whenTheOrderDoesNotExist() {
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(tenantId, vendorId, orderId)).isInstanceOf(NotFoundException.class);
    }
}
