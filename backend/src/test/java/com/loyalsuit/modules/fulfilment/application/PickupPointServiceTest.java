package com.loyalsuit.modules.fulfilment.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.fulfilment.application.dto.DeliveryZoneRequest;
import com.loyalsuit.modules.fulfilment.application.dto.DeliveryZoneResponse;
import com.loyalsuit.modules.fulfilment.application.dto.PickupPointRequest;
import com.loyalsuit.modules.fulfilment.application.dto.PickupPointResponse;
import com.loyalsuit.modules.fulfilment.domain.DeliveryZone;
import com.loyalsuit.modules.fulfilment.domain.PickupPoint;
import com.loyalsuit.modules.fulfilment.domain.port.DeliveryZoneRepository;
import com.loyalsuit.modules.fulfilment.domain.port.PickupPointRepository;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
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
class PickupPointServiceTest {

    @Mock private PickupPointRepository pointRepository;
    @Mock private DeliveryZoneRepository zoneRepository;
    @Mock private TenantRepository tenantRepository;

    @InjectMocks private PickupPointService service;

    private UUID tenantId;
    private UUID pointId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        pointId = UUID.randomUUID();
    }

    private PickupPoint point(UUID id, String name) {
        PickupPoint p = new PickupPoint(tenantId, name);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    private DeliveryZone zone(UUID pointFk, String name, String fee) {
        DeliveryZone z = new DeliveryZone(tenantId, pointFk, name, new BigDecimal(fee));
        ReflectionTestUtils.setField(z, "id", UUID.randomUUID());
        return z;
    }

    private PickupPointRequest pointRequest(String name) {
        PickupPointRequest r = new PickupPointRequest();
        r.setName(name);
        return r;
    }

    private DeliveryZoneRequest zoneRequest(String name, String fee) {
        DeliveryZoneRequest r = new DeliveryZoneRequest();
        r.setName(name);
        r.setFee(new BigDecimal(fee));
        return r;
    }

    // ---- points -------------------------------------------------------------

    @Test
    void createPoint_savesWithNoZonesYet() {
        // Arrange
        when(pointRepository.save(any(PickupPoint.class))).thenAnswer(inv -> {
            PickupPoint p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", pointId);
            return p;
        });

        // Act
        PickupPointResponse point = service.createPoint(tenantId, pointRequest("Downtown"));

        // Assert
        assertThat(point.name()).isEqualTo("Downtown");
        assertThat(point.zones()).isEmpty();
    }

    @Test
    void listPoints_groupsEachPointsOwnZones() {
        // Arrange — two points, each with its own zones (the core requirement)
        UUID p2 = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        when(pointRepository.findByTenantId(tenantId, pageable))
                .thenReturn(new PageImpl<>(List.of(point(pointId, "Downtown"), point(p2, "Suburb"))));
        when(zoneRepository.findByTenantId(tenantId)).thenReturn(List.of(
                zone(pointId, "City centre", "5.00"),
                zone(pointId, "Inner ring", "8.00"),
                zone(p2, "Suburb local", "3.00")));

        // Act
        PageResponse<PickupPointResponse> page = service.listPoints(tenantId, pageable);

        // Assert — zones are partitioned per point, not shared
        PickupPointResponse downtown = page.getContent().stream().filter(p -> p.id().equals(pointId)).findFirst().orElseThrow();
        PickupPointResponse suburb = page.getContent().stream().filter(p -> p.id().equals(p2)).findFirst().orElseThrow();
        assertThat(downtown.zones()).extracting(DeliveryZoneResponse::name).containsExactlyInAnyOrder("City centre", "Inner ring");
        assertThat(suburb.zones()).extracting(DeliveryZoneResponse::name).containsExactly("Suburb local");
    }

    // ---- zones --------------------------------------------------------------

    @Test
    void addZone_attachesItToThePoint() {
        // Arrange
        when(pointRepository.findByIdAndTenantId(pointId, tenantId)).thenReturn(Optional.of(point(pointId, "Downtown")));
        when(zoneRepository.save(any(DeliveryZone.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        DeliveryZoneResponse zone = service.addZone(pointId, tenantId, zoneRequest("City centre", "5.00"));

        // Assert
        assertThat(zone.pickupPointId()).isEqualTo(pointId);
        assertThat(zone.fee()).isEqualByComparingTo("5.00");
        assertThat(zone.active()).isTrue();
    }

    @Test
    void addZone_toAMissingPoint_is404() {
        // Arrange
        when(pointRepository.findByIdAndTenantId(pointId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.addZone(pointId, tenantId, zoneRequest("City centre", "5.00")))
                .isInstanceOf(NotFoundException.class);
        verify(zoneRepository, never()).save(any());
    }

    @Test
    void deleteZone_removesIt() {
        // Arrange
        DeliveryZone zone = zone(pointId, "City centre", "5.00");
        UUID zoneId = (UUID) ReflectionTestUtils.getField(zone, "id");
        when(zoneRepository.findByIdAndTenantId(zoneId, tenantId)).thenReturn(Optional.of(zone));

        // Act
        service.deleteZone(zoneId, tenantId);

        // Assert
        verify(zoneRepository).delete(zone);
    }

    // ---- storefront ---------------------------------------------------------

    @Test
    void publicPoints_returnsActivePointsWithTheirActiveZones() {
        // Arrange
        Tenant tenant = new Tenant("Acme", "acme");
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(tenant));
        when(pointRepository.findActiveByTenantId(tenantId)).thenReturn(List.of(point(pointId, "Downtown")));
        when(zoneRepository.findActiveByTenantId(tenantId)).thenReturn(List.of(zone(pointId, "City centre", "5.00")));

        // Act
        List<PickupPointResponse> points = service.publicPoints("acme");

        // Assert
        assertThat(points).singleElement().satisfies(p ->
                assertThat(p.zones()).singleElement().satisfies(z -> assertThat(z.name()).isEqualTo("City centre")));
    }

    @Test
    void publicPoints_unknownStore_is404() {
        // Arrange
        when(tenantRepository.findBySlug("nope")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.publicPoints("nope")).isInstanceOf(NotFoundException.class);
    }
}
