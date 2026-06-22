package com.loyalsuit.modules.marketplace.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.media.MediaStorage;
import com.loyalsuit.modules.marketplace.application.dto.ApplyVendorRequest;
import com.loyalsuit.modules.marketplace.application.dto.UpdateStorefrontRequest;
import com.loyalsuit.modules.marketplace.application.dto.VendorResponse;
import com.loyalsuit.modules.marketplace.domain.Vendor;
import com.loyalsuit.modules.marketplace.domain.VendorStatus;
import com.loyalsuit.modules.marketplace.domain.port.VendorRepository;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    @Mock private VendorRepository vendorRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private MediaStorage mediaStorage;

    @InjectMocks private VendorService vendorService;

    private UUID tenantId;
    private UUID userId;
    private UUID vendorId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
    }

    private Vendor vendor(VendorStatus status) {
        Vendor v = new Vendor(tenantId, userId, "Acme Goods", "acme-goods");
        v.setStatus(status);
        ReflectionTestUtils.setField(v, "id", vendorId);
        return v;
    }

    private AppUser user(UUID userTenantId) {
        return new AppUser(userTenantId, "seller@acme.dev", "HASH", "Seller", UserRole.CUSTOMER);
    }

    private ApplyVendorRequest applyRequest() {
        var r = new ApplyVendorRequest();
        r.setStoreName("Acme Goods");
        return r;
    }

    // ---- apply --------------------------------------------------------------

    @Test
    void apply_createsPendingVendor() {
        // Arrange
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(vendorRepository.existsBySlug(ArgumentMatchers.anyString())).thenReturn(false);
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        VendorResponse response = vendorService.apply(tenantId, userId, applyRequest());

        // Assert
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.storeName()).isEqualTo("Acme Goods");
    }

    @Test
    void apply_rejectsASecondApplication() {
        // Arrange
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor(VendorStatus.PENDING)));

        // Act & Assert
        assertThatThrownBy(() -> vendorService.apply(tenantId, userId, applyRequest()))
                .isInstanceOf(ConflictException.class);
        verify(vendorRepository, never()).save(any());
    }

    // ---- approve (status + role grant) --------------------------------------

    @Test
    void approve_activatesVendor_andGrantsVendorRole() {
        // Arrange
        AppUser applicant = user(tenantId);
        when(vendorRepository.findByIdAndTenantId(vendorId, tenantId)).thenReturn(Optional.of(vendor(VendorStatus.PENDING)));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(applicant));

        // Act
        VendorResponse response = vendorService.approve(vendorId, tenantId);

        // Assert
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(applicant.getRole()).isEqualTo(UserRole.VENDOR);
        verify(appUserRepository).save(applicant);
    }

    @Test
    void approve_rejectsInvalidTransition() {
        // Arrange — an already-active vendor can't be approved again
        when(vendorRepository.findByIdAndTenantId(vendorId, tenantId)).thenReturn(Optional.of(vendor(VendorStatus.ACTIVE)));

        // Act & Assert
        assertThatThrownBy(() -> vendorService.approve(vendorId, tenantId))
                .isInstanceOf(BusinessException.class);
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void approve_rejectsWhenApplicantBelongsToAnotherTenant() {
        // Arrange — defensive: never elevate a user from a different tenant
        when(vendorRepository.findByIdAndTenantId(vendorId, tenantId)).thenReturn(Optional.of(vendor(VendorStatus.PENDING)));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user(UUID.randomUUID())));

        // Act & Assert
        assertThatThrownBy(() -> vendorService.approve(vendorId, tenantId))
                .isInstanceOf(BusinessException.class);
        verify(appUserRepository, never()).save(any());
    }

    // ---- lifecycle ----------------------------------------------------------

    @Test
    void reject_marksRejected() {
        when(vendorRepository.findByIdAndTenantId(vendorId, tenantId)).thenReturn(Optional.of(vendor(VendorStatus.PENDING)));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(vendorService.reject(vendorId, tenantId).status()).isEqualTo("REJECTED");
    }

    @Test
    void suspend_thenReinstate_movesActiveVendor() {
        // suspend
        when(vendorRepository.findByIdAndTenantId(vendorId, tenantId)).thenReturn(Optional.of(vendor(VendorStatus.ACTIVE)));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(vendorService.suspend(vendorId, tenantId).status()).isEqualTo("SUSPENDED");

        // reinstate
        when(vendorRepository.findByIdAndTenantId(vendorId, tenantId)).thenReturn(Optional.of(vendor(VendorStatus.SUSPENDED)));
        assertThat(vendorService.reinstate(vendorId, tenantId).status()).isEqualTo("ACTIVE");
    }

    @Test
    void suspend_rejectsPendingVendor() {
        when(vendorRepository.findByIdAndTenantId(vendorId, tenantId)).thenReturn(Optional.of(vendor(VendorStatus.PENDING)));

        assertThatThrownBy(() -> vendorService.suspend(vendorId, tenantId))
                .isInstanceOf(BusinessException.class);
        verify(vendorRepository, never()).save(any());
    }

    @Test
    void setCommission_updatesRate() {
        when(vendorRepository.findByIdAndTenantId(vendorId, tenantId)).thenReturn(Optional.of(vendor(VendorStatus.ACTIVE)));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

        VendorResponse response = vendorService.setCommission(vendorId, tenantId, BigDecimal.valueOf(15));

        assertThat(response.commissionRate()).isEqualByComparingTo("15");
    }

    // ---- self-service profile editing (Seller Settings) ---------------------

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    private UpdateStorefrontRequest storefront(String name, String description) {
        var r = new UpdateStorefrontRequest();
        r.setStoreName(name);
        r.setDescription(description);
        return r;
    }

    @Test
    void updateMine_changesNameAndDescription_butNeverTheSlug() {
        // Arrange
        Vendor existing = vendor(VendorStatus.ACTIVE); // slug "acme-goods"
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        VendorResponse response = vendorService.updateMine(userId, storefront("  Acme Spare Parts  ", "  We sell parts  "));

        // Assert — trimmed name/description, slug untouched (storefront URL stays stable)
        assertThat(response.storeName()).isEqualTo("Acme Spare Parts");
        assertThat(response.description()).isEqualTo("We sell parts");
        assertThat(response.slug()).isEqualTo("acme-goods");
    }

    @Test
    void updateMine_blankDescriptionBecomesNull() {
        Vendor existing = vendor(VendorStatus.ACTIVE);
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

        VendorResponse response = vendorService.updateMine(userId, storefront("Acme", "   "));

        assertThat(response.description()).isNull();
    }

    @Test
    void updateMine_rejectsWhenNoVendorProfile() {
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendorService.updateMine(userId, storefront("Acme", null)))
                .isInstanceOf(NotFoundException.class);
        verify(vendorRepository, never()).save(any());
    }

    @Test
    void updateLogo_storesAssetUnderVendorFolder_andSetsUrlAndPublicId() {
        // Arrange
        Vendor existing = vendor(VendorStatus.ACTIVE);
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(mediaStorage.upload(any(byte[].class), any(String.class)))
                .thenReturn(new MediaStorage.StoredAsset("pid-new", "https://cdn/logo.png"));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        VendorResponse response = vendorService.updateLogo(userId, PNG);

        // Assert
        ArgumentCaptor<String> folder = ArgumentCaptor.forClass(String.class);
        verify(mediaStorage).upload(any(byte[].class), folder.capture());
        assertThat(folder.getValue()).isEqualTo("loyalsuit/" + tenantId + "/vendors/" + vendorId);
        assertThat(response.logoUrl()).isEqualTo("https://cdn/logo.png");
        assertThat(existing.getLogoPublicId()).isEqualTo("pid-new");
    }

    @Test
    void updateLogo_deletesPreviousAssetOnReplace() {
        // Arrange — vendor already has a logo
        Vendor existing = vendor(VendorStatus.ACTIVE);
        existing.setLogoPublicId("pid-old");
        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(mediaStorage.upload(any(byte[].class), any(String.class)))
                .thenReturn(new MediaStorage.StoredAsset("pid-new", "https://cdn/new.png"));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        vendorService.updateLogo(userId, PNG);

        // Assert — old asset cleaned up, never the freshly stored one
        verify(mediaStorage).delete("pid-old");
    }

    @Test
    void updateLogo_rejectsNonImageBytes_beforeAnyUpload() {
        byte[] notAnImage = "definitely not an image".getBytes();

        assertThatThrownBy(() -> vendorService.updateLogo(userId, notAnImage))
                .isInstanceOf(BusinessException.class);
        verify(mediaStorage, never()).upload(any(), any());
        verify(vendorRepository, never()).save(any());
    }
}
