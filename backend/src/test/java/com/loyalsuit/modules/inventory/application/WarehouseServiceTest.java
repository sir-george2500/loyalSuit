package com.loyalsuit.modules.inventory.application;

import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.inventory.application.dto.WarehouseRequest;
import com.loyalsuit.modules.inventory.application.dto.WarehouseResponse;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock private WarehouseRepository warehouseRepository;
    @Mock private StockRepository stockRepository;
    @InjectMocks private WarehouseService warehouseService;

    private UUID tenantId;
    private UUID warehouseId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
    }

    private WarehouseRequest request(String name, boolean isDefault) {
        var r = new WarehouseRequest();
        r.setName(name);
        r.setDefault(isDefault);
        return r;
    }

    private Warehouse warehouse(String name, boolean isDefault, UUID id) {
        var w = new Warehouse(tenantId, name, null, isDefault);
        ReflectionTestUtils.setField(w, "id", id);
        return w;
    }

    // ---- create -------------------------------------------------------------

    @Test
    void create_makesFirstWarehouseDefault_evenIfNotRequested() {
        // Arrange — no warehouses yet
        when(warehouseRepository.existsByTenantId(tenantId)).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        WarehouseResponse response = warehouseService.create(request("Main", false), tenantId);

        // Assert
        assertThat(response.isDefault()).isTrue();
    }

    @Test
    void create_asDefault_clearsThePreviousDefault() {
        // Arrange — an existing default warehouse. (existsByTenantId isn't stubbed:
        // request.isDefault()=true short-circuits before it would be called.)
        Warehouse existing = warehouse("Old", true, UUID.randomUUID());
        when(warehouseRepository.findByTenantIdOrderByName(tenantId)).thenReturn(List.of(existing));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        WarehouseResponse response = warehouseService.create(request("New", true), tenantId);

        // Assert — exactly one default remains
        assertThat(response.isDefault()).isTrue();
        assertThat(existing.isDefault()).isFalse();
        verify(warehouseRepository).save(existing);
    }

    @Test
    void create_nonDefault_whenOthersAlreadyExist() {
        // Arrange
        when(warehouseRepository.existsByTenantId(tenantId)).thenReturn(true);
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        WarehouseResponse response = warehouseService.create(request("Annex", false), tenantId);

        // Assert
        assertThat(response.isDefault()).isFalse();
    }

    // ---- update -------------------------------------------------------------

    @Test
    void update_promotingToDefault_demotesTheOther() {
        // Arrange
        Warehouse target = warehouse("Annex", false, warehouseId);
        Warehouse currentDefault = warehouse("Main", true, UUID.randomUUID());
        when(warehouseRepository.findByIdAndTenantId(warehouseId, tenantId)).thenReturn(Optional.of(target));
        when(warehouseRepository.findByTenantIdOrderByName(tenantId)).thenReturn(List.of(currentDefault, target));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        WarehouseResponse response = warehouseService.update(warehouseId, request("Annex", true), tenantId);

        // Assert
        assertThat(response.isDefault()).isTrue();
        assertThat(currentDefault.isDefault()).isFalse();
    }

    @Test
    void getById_throwsNotFound_whenNotInTenant() {
        // Arrange
        when(warehouseRepository.findByIdAndTenantId(warehouseId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> warehouseService.getById(warehouseId, tenantId))
                .isInstanceOf(NotFoundException.class);
    }

    // ---- delete -------------------------------------------------------------

    @Test
    void delete_isBlockedForTheDefaultWarehouse() {
        // Arrange
        when(warehouseRepository.findByIdAndTenantId(warehouseId, tenantId))
                .thenReturn(Optional.of(warehouse("Main", true, warehouseId)));

        // Act & Assert
        assertThatThrownBy(() -> warehouseService.delete(warehouseId, tenantId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("default");
        verify(warehouseRepository, never()).deleteById(any());
    }

    @Test
    void delete_isBlockedWhenItIsTheLastWarehouse() {
        // Arrange
        when(warehouseRepository.findByIdAndTenantId(warehouseId, tenantId))
                .thenReturn(Optional.of(warehouse("Annex", false, warehouseId)));
        when(warehouseRepository.countByTenantId(tenantId)).thenReturn(1);

        // Act & Assert
        assertThatThrownBy(() -> warehouseService.delete(warehouseId, tenantId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("at least one");
        verify(warehouseRepository, never()).deleteById(any());
    }

    @Test
    void delete_succeedsForNonDefaultWhenOthersRemain() {
        // Arrange
        when(warehouseRepository.findByIdAndTenantId(warehouseId, tenantId))
                .thenReturn(Optional.of(warehouse("Annex", false, warehouseId)));
        when(warehouseRepository.countByTenantId(tenantId)).thenReturn(3);
        when(stockRepository.existsByWarehouseId(warehouseId)).thenReturn(false);

        // Act
        warehouseService.delete(warehouseId, tenantId);

        // Assert
        verify(warehouseRepository).deleteById(warehouseId);
    }

    @Test
    void delete_isBlockedWhenWarehouseStillHoldsStock() {
        // Arrange
        when(warehouseRepository.findByIdAndTenantId(warehouseId, tenantId))
                .thenReturn(Optional.of(warehouse("Annex", false, warehouseId)));
        when(warehouseRepository.countByTenantId(tenantId)).thenReturn(3);
        when(stockRepository.existsByWarehouseId(warehouseId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> warehouseService.delete(warehouseId, tenantId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("stock");
        verify(warehouseRepository, never()).deleteById(any());
    }

    @Test
    void list_returnsAllForTenant() {
        // Arrange
        when(warehouseRepository.findByTenantIdOrderByName(tenantId))
                .thenReturn(List.of(warehouse("A", true, UUID.randomUUID()), warehouse("B", false, UUID.randomUUID())));

        // Act
        List<WarehouseResponse> result = warehouseService.list(tenantId);

        // Assert
        assertThat(result).hasSize(2);
    }
}
