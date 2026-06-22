package com.loyalsuit.modules.marketplace.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.media.ImageBytes;
import com.loyalsuit.common.media.MediaStorage;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.marketplace.application.dto.ApplyVendorRequest;
import com.loyalsuit.modules.marketplace.application.dto.UpdateStorefrontRequest;
import com.loyalsuit.modules.marketplace.application.dto.VendorResponse;
import com.loyalsuit.modules.marketplace.domain.Vendor;
import com.loyalsuit.modules.marketplace.domain.VendorStatus;
import com.loyalsuit.modules.marketplace.domain.port.VendorRepository;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

/**
 * Vendor onboarding and the admin approval lifecycle. A user applies once; an admin
 * runs a guarded status machine. Approving a vendor grants the user the VENDOR role
 * so they can start selling — a deliberate, admin-only capability grant, applied
 * only to a user who belongs to the same tenant.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorService {

    private final VendorRepository vendorRepository;
    private final AppUserRepository appUserRepository;
    private final MediaStorage mediaStorage;

    @Transactional
    public VendorResponse apply(UUID tenantId, UUID userId, ApplyVendorRequest request) {
        if (vendorRepository.findByUserId(userId).isPresent()) {
            throw new ConflictException("You already have a vendor application");
        }
        Vendor vendor = new Vendor(tenantId, userId, request.getStoreName().trim(),
                uniqueSlug(request.getStoreName()));
        vendor.setDescription(trimToNull(request.getDescription()));
        return VendorResponse.from(vendorRepository.save(vendor));
    }

    public VendorResponse getMine(UUID userId) {
        return vendorRepository.findByUserId(userId)
                .map(VendorResponse::from)
                .orElseThrow(() -> new NotFoundException("Vendor", userId));
    }

    /**
     * A vendor edits their own storefront profile (display name + description). The slug
     * is deliberately left untouched so the public storefront URL stays stable. Allowed
     * for any vendor that owns a profile (including PENDING) — it changes nothing a
     * shopper sees until the vendor is ACTIVE.
     */
    @Transactional
    public VendorResponse updateMine(UUID userId, UpdateStorefrontRequest request) {
        Vendor vendor = requireOwnProfile(userId);
        vendor.setStoreName(request.getStoreName().trim());
        vendor.setDescription(trimToNull(request.getDescription()));
        return VendorResponse.from(vendorRepository.save(vendor));
    }

    /**
     * A vendor replaces their storefront logo. Bytes are validated as a real image
     * (magic number + size cap) before upload. On success the previously stored asset is
     * deleted best-effort so replacing a logo doesn't orphan files in the media store.
     */
    @Transactional
    public VendorResponse updateLogo(UUID userId, byte[] data) {
        ImageBytes.validate(data);
        Vendor vendor = requireOwnProfile(userId);
        String folder = "loyalsuit/" + vendor.getTenantId() + "/vendors/" + vendor.getId();
        MediaStorage.StoredAsset asset = mediaStorage.upload(data, folder);

        String previousPublicId = vendor.getLogoPublicId();
        vendor.setLogoUrl(asset.secureUrl());
        vendor.setLogoPublicId(asset.publicId());
        Vendor saved = vendorRepository.save(vendor);

        if (previousPublicId != null && !previousPublicId.equals(asset.publicId())) {
            mediaStorage.delete(previousPublicId);
        }
        return VendorResponse.from(saved);
    }

    private Vendor requireOwnProfile(UUID userId) {
        return vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Vendor", userId));
    }

    /**
     * Collaboration API: is this user an active (approved, not suspended) vendor?
     * Used by catalog to block a suspended/pending vendor from selling.
     */
    public boolean isActiveVendor(UUID userId) {
        return vendorRepository.findByUserId(userId)
                .map(v -> v.getStatus() == VendorStatus.ACTIVE)
                .orElse(false);
    }

    /**
     * The acting vendor's id (the Vendor PK — not the user id), used to scope their catalogue for
     * reads. Throws if the user has no vendor profile.
     */
    public UUID requireVendorId(UUID userId) {
        return vendorRepository.findByUserId(userId)
                .map(Vendor::getId)
                .orElseThrow(() -> new BusinessException("No vendor profile for this account"));
    }

    /**
     * The acting vendor's id, only when their account is ACTIVE — gates create/edit so a
     * pending/suspended seller can't change the catalogue.
     */
    public UUID requireActiveVendorId(UUID userId) {
        return vendorRepository.findByUserId(userId)
                .filter(v -> v.getStatus() == VendorStatus.ACTIVE)
                .map(Vendor::getId)
                .orElseThrow(() -> new BusinessException("Your vendor account is not active"));
    }

    public PageResponse<VendorResponse> list(UUID tenantId, VendorStatus status, Pageable pageable) {
        var page = status == null
                ? vendorRepository.findByTenantId(tenantId, pageable)
                : vendorRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        return new PageResponse<>(page.map(VendorResponse::from));
    }

    @Transactional
    public VendorResponse approve(UUID id, UUID tenantId) {
        Vendor vendor = transition(id, tenantId, VendorStatus.ACTIVE);
        grantVendorRole(vendor.getUserId(), tenantId);
        return VendorResponse.from(vendor);
    }

    @Transactional
    public VendorResponse reject(UUID id, UUID tenantId) {
        return VendorResponse.from(transition(id, tenantId, VendorStatus.REJECTED));
    }

    @Transactional
    public VendorResponse suspend(UUID id, UUID tenantId) {
        return VendorResponse.from(transition(id, tenantId, VendorStatus.SUSPENDED));
    }

    @Transactional
    public VendorResponse reinstate(UUID id, UUID tenantId) {
        return VendorResponse.from(transition(id, tenantId, VendorStatus.ACTIVE));
    }

    @Transactional
    public VendorResponse setCommission(UUID id, UUID tenantId, BigDecimal rate) {
        Vendor vendor = load(id, tenantId);
        vendor.setCommissionRate(rate);
        return VendorResponse.from(vendorRepository.save(vendor));
    }

    private Vendor transition(UUID id, UUID tenantId, VendorStatus target) {
        Vendor vendor = load(id, tenantId);
        if (!vendor.getStatus().canTransitionTo(target)) {
            throw new BusinessException("Can't move a vendor from " + vendor.getStatus() + " to " + target);
        }
        vendor.setStatus(target);
        return vendorRepository.save(vendor);
    }

    /** Elevate the applicant to the VENDOR role. Only a user of this tenant is touched. */
    private void grantVendorRole(UUID userId, UUID tenantId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));
        if (!tenantId.equals(user.getTenantId())) {
            throw new BusinessException("That user does not belong to this store");
        }
        user.setRole(UserRole.VENDOR);
        appUserRepository.save(user);
    }

    private Vendor load(UUID id, UUID tenantId) {
        return vendorRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Vendor", id));
    }

    private String uniqueSlug(String base) {
        String slug = Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            slug = "vendor";
        }
        String candidate = slug;
        while (vendorRepository.existsBySlug(candidate)) {
            candidate = slug + "-" + UUID.randomUUID().toString().substring(0, 6);
        }
        return candidate;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
