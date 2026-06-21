package com.loyalsuit.modules.reviews.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import com.loyalsuit.modules.orders.application.PurchaseQueryService;
import com.loyalsuit.modules.orders.application.dto.PurchasedItem;
import com.loyalsuit.modules.reviews.application.dto.CreateReviewRequest;
import com.loyalsuit.modules.reviews.application.dto.ProductReviewsResponse;
import com.loyalsuit.modules.reviews.application.dto.ReviewEligibility;
import com.loyalsuit.modules.reviews.application.dto.ReviewResponse;
import com.loyalsuit.modules.reviews.application.dto.VendorReviewsResponse;
import com.loyalsuit.modules.reviews.domain.Review;
import com.loyalsuit.modules.reviews.domain.ReviewStats;
import com.loyalsuit.modules.reviews.domain.port.ReviewRepository;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private PurchaseQueryService purchaseQueryService;
    @Mock private AppUserRepository appUserRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks private ReviewService service;

    private UUID tenantId;
    private UUID customerId;
    private UUID productId;
    private UUID vendorId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
    }

    private CreateReviewRequest request(int rating, String comment) {
        var r = new CreateReviewRequest();
        r.setProductId(productId);
        r.setRating(rating);
        r.setComment(comment);
        return r;
    }

    private AppUser customer(String fullName) {
        return new AppUser(tenantId, "jane@buyer.dev", "HASH", fullName, UserRole.CUSTOMER);
    }

    // ---- create -------------------------------------------------------------

    @Test
    void create_storesReview_snapshotsVendor_andPrivacyName() {
        // Arrange — delivered purchase from a vendor, never reviewed before
        when(reviewRepository.existsByTenantIdAndCustomerIdAndProductId(tenantId, customerId, productId))
                .thenReturn(false);
        when(purchaseQueryService.findDeliveredPurchase(tenantId, customerId, productId))
                .thenReturn(Optional.of(new PurchasedItem(productId, vendorId)));
        when(appUserRepository.findById(customerId)).thenReturn(Optional.of(customer("Jane Doe")));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ReviewResponse response = service.create(tenantId, customerId, request(5, "  Great part  "));

        // Assert
        ArgumentCaptor<Review> saved = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(saved.capture());
        assertThat(saved.getValue().getVendorId()).isEqualTo(vendorId);
        assertThat(saved.getValue().getRating()).isEqualTo(5);
        assertThat(saved.getValue().getComment()).isEqualTo("Great part"); // trimmed
        assertThat(response.authorName()).isEqualTo("Jane D.");            // privacy-minimised
    }

    @Test
    void create_rejectsSecondReviewOfSameProduct() {
        when(reviewRepository.existsByTenantIdAndCustomerIdAndProductId(tenantId, customerId, productId))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(tenantId, customerId, request(4, null)))
                .isInstanceOf(ConflictException.class);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void create_rejectsWhenNoDeliveredPurchase() {
        when(reviewRepository.existsByTenantIdAndCustomerIdAndProductId(tenantId, customerId, productId))
                .thenReturn(false);
        when(purchaseQueryService.findDeliveredPurchase(tenantId, customerId, productId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(tenantId, customerId, request(4, "nice")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("delivered");
        verify(reviewRepository, never()).save(any());
    }

    // ---- eligibility --------------------------------------------------------

    @Test
    void eligibility_true_whenPurchasedAndNotReviewed() {
        when(reviewRepository.existsByTenantIdAndCustomerIdAndProductId(tenantId, customerId, productId))
                .thenReturn(false);
        when(purchaseQueryService.findDeliveredPurchase(tenantId, customerId, productId))
                .thenReturn(Optional.of(new PurchasedItem(productId, vendorId)));

        ReviewEligibility result = service.eligibility(tenantId, customerId, productId);

        assertThat(result.canReview()).isTrue();
        assertThat(result.alreadyReviewed()).isFalse();
    }

    @Test
    void eligibility_cannotReviewAgain_whenAlreadyReviewed() {
        when(reviewRepository.existsByTenantIdAndCustomerIdAndProductId(tenantId, customerId, productId))
                .thenReturn(true);
        when(purchaseQueryService.findDeliveredPurchase(tenantId, customerId, productId))
                .thenReturn(Optional.of(new PurchasedItem(productId, vendorId)));

        ReviewEligibility result = service.eligibility(tenantId, customerId, productId);

        assertThat(result.canReview()).isFalse();
        assertThat(result.alreadyReviewed()).isTrue();
    }

    // ---- reads --------------------------------------------------------------

    @Test
    void forProduct_returnsRoundedSummaryAndPage() {
        Review r = new Review(tenantId, productId, customerId, vendorId, "Jane D.", 4, "ok");
        Page<Review> page = new PageImpl<>(List.of(r), PageRequest.of(0, 10), 1);
        when(reviewRepository.findByProductId(productId, PageRequest.of(0, 10))).thenReturn(page);
        when(reviewRepository.statsForProduct(productId)).thenReturn(new ReviewStats(4.3333, 3));

        ProductReviewsResponse response = service.forProduct(productId, PageRequest.of(0, 10));

        assertThat(response.summary().averageRating()).isEqualTo(4.3); // rounded to 1dp
        assertThat(response.summary().count()).isEqualTo(3);
        assertThat(response.reviews().getContent()).hasSize(1);
    }

    @Test
    void forVendor_resolvesProductNames_andSummary() {
        Review r = new Review(tenantId, productId, customerId, vendorId, "Jane D.", 5, "great");
        Product product = new Product(tenantId, "Brake Pad", "brake-pad", BigDecimal.TEN);
        ReflectionTestUtils.setField(product, "id", productId);
        Page<Review> page = new PageImpl<>(List.of(r), PageRequest.of(0, 20), 1);
        when(reviewRepository.findByTenantIdAndVendorId(tenantId, vendorId, PageRequest.of(0, 20))).thenReturn(page);
        when(productRepository.findByIdInAndTenantId(List.of(productId), tenantId)).thenReturn(List.of(product));
        when(reviewRepository.statsForVendor(tenantId, vendorId)).thenReturn(new ReviewStats(5.0, 1));

        VendorReviewsResponse response = service.forVendor(tenantId, vendorId, PageRequest.of(0, 20));

        assertThat(response.summary().averageRating()).isEqualTo(5.0);
        assertThat(response.reviews().getContent()).hasSize(1);
        assertThat(response.reviews().getContent().get(0).productName()).isEqualTo("Brake Pad");
    }
}
