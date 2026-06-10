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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The pickup-and-zones network. A store runs one or more pickup points, and <b>each point
 * defines its own delivery zones</b> (area + fee) — so two points can price the same city
 * differently. Admins configure the network; the storefront reads the active points and
 * zones to price shipping at checkout. Tenant-scoped throughout.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PickupPointService {

    private final PickupPointRepository pointRepository;
    private final DeliveryZoneRepository zoneRepository;
    private final TenantRepository tenantRepository;

    // ---- pickup points ------------------------------------------------------

    @Transactional
    public PickupPointResponse createPoint(UUID tenantId, PickupPointRequest request) {
        PickupPoint point = new PickupPoint(tenantId, request.getName().trim());
        apply(point, request);
        return PickupPointResponse.from(pointRepository.save(point), List.of());
    }

    @Transactional
    public PickupPointResponse updatePoint(UUID id, UUID tenantId, PickupPointRequest request) {
        PickupPoint point = loadPoint(id, tenantId);
        apply(point, request);
        return withZones(pointRepository.save(point));
    }

    @Transactional
    public PickupPointResponse setPointActive(UUID id, UUID tenantId, boolean active) {
        PickupPoint point = loadPoint(id, tenantId);
        point.setActive(active);
        return withZones(pointRepository.save(point));
    }

    public PageResponse<PickupPointResponse> listPoints(UUID tenantId, Pageable pageable) {
        Map<UUID, List<DeliveryZone>> zonesByPoint = zoneRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.groupingBy(DeliveryZone::getPickupPointId));
        return new PageResponse<>(pointRepository.findByTenantId(tenantId, pageable)
                .map(point -> PickupPointResponse.from(point, zonesByPoint.getOrDefault(point.getId(), List.of()))));
    }

    // ---- zones (under a point) ----------------------------------------------

    @Transactional
    public DeliveryZoneResponse addZone(UUID pointId, UUID tenantId, DeliveryZoneRequest request) {
        loadPoint(pointId, tenantId); // the zone must hang off a point of this tenant
        DeliveryZone zone = new DeliveryZone(tenantId, pointId, request.getName().trim(), request.getFee());
        return DeliveryZoneResponse.from(zoneRepository.save(zone));
    }

    @Transactional
    public DeliveryZoneResponse updateZone(UUID zoneId, UUID tenantId, DeliveryZoneRequest request) {
        DeliveryZone zone = loadZone(zoneId, tenantId);
        zone.setName(request.getName().trim());
        zone.setFee(request.getFee());
        return DeliveryZoneResponse.from(zoneRepository.save(zone));
    }

    @Transactional
    public DeliveryZoneResponse setZoneActive(UUID zoneId, UUID tenantId, boolean active) {
        DeliveryZone zone = loadZone(zoneId, tenantId);
        zone.setActive(active);
        return DeliveryZoneResponse.from(zoneRepository.save(zone));
    }

    @Transactional
    public void deleteZone(UUID zoneId, UUID tenantId) {
        zoneRepository.delete(loadZone(zoneId, tenantId));
    }

    // ---- storefront (public) ------------------------------------------------

    /** Active pickup points and their active zones, for a storefront's checkout. */
    public List<PickupPointResponse> publicPoints(String storeSlug) {
        Tenant tenant = tenantRepository.findBySlug(storeSlug)
                .filter(Tenant::isActive)
                .orElseThrow(() -> new NotFoundException("Store", storeSlug));

        Map<UUID, List<DeliveryZone>> activeZones = zoneRepository.findActiveByTenantId(tenant.getId()).stream()
                .collect(Collectors.groupingBy(DeliveryZone::getPickupPointId));
        return pointRepository.findActiveByTenantId(tenant.getId()).stream()
                .map(point -> PickupPointResponse.from(point, activeZones.getOrDefault(point.getId(), List.of())))
                .toList();
    }

    // ---- helpers ------------------------------------------------------------

    private PickupPointResponse withZones(PickupPoint point) {
        return PickupPointResponse.from(point,
                zoneRepository.findByPickupPointIdAndTenantId(point.getId(), point.getTenantId()));
    }

    private static void apply(PickupPoint point, PickupPointRequest request) {
        point.setName(request.getName().trim());
        point.setAddress(trimToNull(request.getAddress()));
        point.setPhone(trimToNull(request.getPhone()));
    }

    private PickupPoint loadPoint(UUID id, UUID tenantId) {
        return pointRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Pickup point", id));
    }

    private DeliveryZone loadZone(UUID id, UUID tenantId) {
        return zoneRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Delivery zone", id));
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
