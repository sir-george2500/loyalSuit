package com.loyalsuit.modules.inventory.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.catalog.application.ProductVariantService;
import com.loyalsuit.modules.inventory.application.dto.SetStockRequest;
import com.loyalsuit.modules.inventory.application.dto.StockResponse;
import com.loyalsuit.modules.inventory.domain.Stock;
import com.loyalsuit.modules.inventory.domain.Warehouse;
import com.loyalsuit.modules.inventory.domain.port.StockRepository;
import com.loyalsuit.modules.inventory.domain.port.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock private StockRepository stockRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private ProductVariantService productVariantService;

    @InjectMocks private StockService stockService;

    private UUID tenantId;
    private UUID productId;
    private UUID warehouseId;
    private UUID stockId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        productId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
        stockId = UUID.randomUUID();
    }

    private SetStockRequest setRequest(int quantity) {
        var r = new SetStockRequest();
        r.setProductId(productId);
        r.setWarehouseId(warehouseId);
        r.setQuantity(quantity);
        r.setLowStockThreshold(5);
        return r;
    }

    private Warehouse warehouse() {
        return new Warehouse(tenantId, "Main", null, true);
    }

    private Stock stock(int quantity) {
        var s = new Stock(tenantId, productId, null, warehouseId);
        s.setQuantity(quantity);
        ReflectionTestUtils.setField(s, "id", stockId);
        return s;
    }

    // ---- setLevel -----------------------------------------------------------

    @Test
    void setLevel_createsRow_whenNoneExists() {
        // Arrange
        when(warehouseRepository.findByIdAndTenantId(warehouseId, tenantId)).thenReturn(Optional.of(warehouse()));
        when(stockRepository.findExisting(productId, null, warehouseId, tenantId)).thenReturn(Optional.empty());
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        StockResponse response = stockService.setLevel(setRequest(40), tenantId);

        // Assert
        assertThat(response.quantity()).isEqualTo(40);
        assertThat(response.lowStockThreshold()).isEqualTo(5);
    }

    @Test
    void setLevel_updatesExistingRow() {
        // Arrange
        when(warehouseRepository.findByIdAndTenantId(warehouseId, tenantId)).thenReturn(Optional.of(warehouse()));
        when(stockRepository.findExisting(productId, null, warehouseId, tenantId)).thenReturn(Optional.of(stock(10)));
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        StockResponse response = stockService.setLevel(setRequest(99), tenantId);

        // Assert
        assertThat(response.quantity()).isEqualTo(99);
    }

    @Test
    void setLevel_rejectsProductNotInTenant() {
        // Arrange — catalog validation fails
        doThrow(new NotFoundException("Product", productId))
                .when(productVariantService).assertProductAndVariant(any(), any(), any());

        // Act & Assert
        assertThatThrownBy(() -> stockService.setLevel(setRequest(10), tenantId))
                .isInstanceOf(NotFoundException.class);
        verify(stockRepository, never()).save(any());
    }

    @Test
    void setLevel_rejectsWarehouseNotInTenant() {
        // Arrange — product passes, warehouse doesn't resolve in tenant
        when(warehouseRepository.findByIdAndTenantId(warehouseId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> stockService.setLevel(setRequest(10), tenantId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Warehouse does not exist");
        verify(stockRepository, never()).save(any());
    }

    // ---- adjust -------------------------------------------------------------

    @Test
    void adjust_appliesDeltaAtomically() {
        // Arrange — row exists; the atomic update affects one row; re-read shows result
        when(stockRepository.findByIdAndTenantId(stockId, tenantId)).thenReturn(Optional.of(stock(15)));
        when(stockRepository.applyDelta(stockId, tenantId, 5)).thenReturn(1);

        // Act
        StockResponse response = stockService.adjust(stockId, 5, tenantId);

        // Assert
        assertThat(response.quantity()).isEqualTo(15);
        verify(stockRepository).applyDelta(stockId, tenantId, 5);
    }

    @Test
    void adjust_rejectsWhenDeltaWouldGoNegative() {
        // Arrange — the atomic guard updates zero rows (insufficient stock)
        when(stockRepository.findByIdAndTenantId(stockId, tenantId)).thenReturn(Optional.of(stock(2)));
        when(stockRepository.applyDelta(stockId, tenantId, -5)).thenReturn(0);

        // Act & Assert
        assertThatThrownBy(() -> stockService.adjust(stockId, -5, tenantId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Not enough stock");
    }

    @Test
    void adjust_rejectsStockNotInTenant() {
        // Arrange
        when(stockRepository.findByIdAndTenantId(stockId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> stockService.adjust(stockId, 5, tenantId))
                .isInstanceOf(NotFoundException.class);
        verify(stockRepository, never()).applyDelta(any(), any(), anyInt());
    }

    // ---- reads --------------------------------------------------------------

    @Test
    void listForProduct_returnsRows() {
        // Arrange
        when(stockRepository.findByProductIdAndTenantId(productId, tenantId)).thenReturn(List.of(stock(10)));

        // Act
        List<StockResponse> result = stockService.listForProduct(productId, tenantId);

        // Assert
        assertThat(result).hasSize(1);
        verify(productVariantService).assertProductAndVariant(productId, null, tenantId);
    }

    @Test
    void lowStock_returnsRowsAtOrBelowThreshold() {
        // Arrange
        when(stockRepository.findLowStock(tenantId)).thenReturn(List.of(stock(1)));

        // Act
        List<StockResponse> result = stockService.lowStock(tenantId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).lowStock()).isTrue();
    }

    // ---- reserve (checkout) -------------------------------------------------

    private Warehouse defaultWarehouse() {
        Warehouse w = warehouse();
        ReflectionTestUtils.setField(w, "id", warehouseId);
        return w;
    }

    @Test
    void reserve_decrementsDefaultWarehouseStock() {
        // Arrange
        when(warehouseRepository.findDefault(tenantId)).thenReturn(Optional.of(defaultWarehouse()));
        when(stockRepository.findExisting(productId, null, warehouseId, tenantId))
                .thenReturn(Optional.of(stock(10)));
        when(stockRepository.applyDelta(stockId, tenantId, -3)).thenReturn(1);

        // Act
        stockService.reserve(tenantId, productId, null, 3);

        // Assert
        verify(stockRepository).applyDelta(stockId, tenantId, -3);
    }

    @Test
    void reserve_throwsWhenStockIsInsufficient() {
        // Arrange — the atomic decrement updates zero rows
        when(warehouseRepository.findDefault(tenantId)).thenReturn(Optional.of(defaultWarehouse()));
        when(stockRepository.findExisting(productId, null, warehouseId, tenantId))
                .thenReturn(Optional.of(stock(1)));
        when(stockRepository.applyDelta(stockId, tenantId, -3)).thenReturn(0);

        // Act & Assert
        assertThatThrownBy(() -> stockService.reserve(tenantId, productId, null, 3))
                .isInstanceOf(com.loyalsuit.common.exception.BusinessException.class);
    }

    @Test
    void reserve_isNoOpWhenStoreHasNoDefaultWarehouse() {
        // Arrange — store isn't tracking stock at all
        when(warehouseRepository.findDefault(tenantId)).thenReturn(Optional.empty());

        // Act
        stockService.reserve(tenantId, productId, null, 3);

        // Assert
        verify(stockRepository, never()).applyDelta(any(), any(), anyInt());
    }

    @Test
    void reserve_isNoOpWhenItemIsNotStocked() {
        // Arrange — default warehouse exists but no stock row for the item
        when(warehouseRepository.findDefault(tenantId)).thenReturn(Optional.of(defaultWarehouse()));
        when(stockRepository.findExisting(productId, null, warehouseId, tenantId)).thenReturn(Optional.empty());

        // Act
        stockService.reserve(tenantId, productId, null, 3);

        // Assert
        verify(stockRepository, never()).applyDelta(any(), any(), anyInt());
    }
}
