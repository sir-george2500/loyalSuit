package com.loyalsuit.modules.catalog.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.catalog.application.dto.MediaResponse;
import com.loyalsuit.modules.catalog.application.port.MediaStorage;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.ProductMedia;
import com.loyalsuit.modules.catalog.domain.port.ProductMediaRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaUploadServiceTest {

    @Mock private ProductMediaRepository mediaRepository;
    @Mock private ProductRepository productRepository;
    @Mock private MediaStorage mediaStorage;

    @InjectMocks private MediaUploadService service;

    private UUID tenantId;
    private UUID productId;
    private Product product;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        productId = UUID.randomUUID();
        product = new Product(tenantId, "Shoe", "shoe", BigDecimal.valueOf(50));
    }

    private static byte[] pngBytes() throws IOException {
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    // ---- access control & validation ---------------------------------------

    @Test
    void upload_rejectsWhenProductNotInTenant() throws Exception {
        // Arrange
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.upload(productId, tenantId, pngBytes()))
                .isInstanceOf(NotFoundException.class);
        verify(mediaStorage, never()).upload(any(), any());
    }

    @Test
    void upload_rejectsEmptyFile() {
        // Arrange
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThatThrownBy(() -> service.upload(productId, tenantId, new byte[0]))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("empty");
        verify(mediaStorage, never()).upload(any(), any());
    }

    @Test
    void upload_rejectsOversizeFile() {
        // Arrange
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        byte[] tooBig = new byte[5 * 1024 * 1024 + 1];

        // Act & Assert
        assertThatThrownBy(() -> service.upload(productId, tenantId, tooBig))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("too large");
        verify(mediaStorage, never()).upload(any(), any());
    }

    @Test
    void upload_rejectsNonImageBytes_evenIfNotEmpty() {
        // Arrange — a text payload, not a real image (magic bytes won't match)
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        byte[] notAnImage = "this is definitely not an image".getBytes(StandardCharsets.UTF_8);

        // Act & Assert
        assertThatThrownBy(() -> service.upload(productId, tenantId, notAnImage))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("images are allowed");
        verify(mediaStorage, never()).upload(any(), any());
    }

    // ---- happy paths --------------------------------------------------------

    @Test
    void upload_storesImageInTenantFolder_andMarksFirstAsPrimary() throws Exception {
        // Arrange
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(mediaRepository.countByProductId(productId)).thenReturn(0);
        when(mediaStorage.upload(any(byte[].class), any(String.class)))
                .thenReturn(new MediaStorage.StoredAsset("pid-1", "https://cdn/img1.png"));
        when(mediaRepository.save(any(ProductMedia.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        MediaResponse response = service.upload(productId, tenantId, pngBytes());

        // Assert — stored under a per-tenant/per-product folder, first image is primary
        ArgumentCaptor<String> folder = ArgumentCaptor.forClass(String.class);
        verify(mediaStorage).upload(any(byte[].class), folder.capture());
        assertThat(folder.getValue()).isEqualTo("loyalsuit/" + tenantId + "/products/" + productId);
        assertThat(response.url()).isEqualTo("https://cdn/img1.png");
        assertThat(response.primary()).isTrue();
        assertThat(response.sortOrder()).isEqualTo(0);
    }

    @Test
    void upload_secondImage_isNotPrimary_andSortsAfter() throws Exception {
        // Arrange
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(mediaRepository.countByProductId(productId)).thenReturn(1);
        when(mediaStorage.upload(any(byte[].class), any(String.class)))
                .thenReturn(new MediaStorage.StoredAsset("pid-2", "https://cdn/img2.png"));
        when(mediaRepository.save(any(ProductMedia.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        MediaResponse response = service.upload(productId, tenantId, pngBytes());

        // Assert
        assertThat(response.primary()).isFalse();
        assertThat(response.sortOrder()).isEqualTo(1);
    }

    // ---- delete -------------------------------------------------------------

    @Test
    void delete_removesAssetAndRow_andPromotesNextPrimary() {
        // Arrange — deleting the primary image; another remains to be promoted
        UUID mediaId = UUID.randomUUID();
        ProductMedia primary = new ProductMedia(tenantId, productId, "pid-1", "https://cdn/img1.png");
        primary.setPrimary(true);
        ProductMedia next = new ProductMedia(tenantId, productId, "pid-2", "https://cdn/img2.png");
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(primary));
        when(mediaRepository.findByProductIdOrderBySortOrderAsc(productId)).thenReturn(List.of(next));

        // Act
        service.delete(productId, mediaId, tenantId);

        // Assert
        verify(mediaStorage).delete("pid-1");
        verify(mediaRepository).deleteById(mediaId);
        assertThat(next.isPrimary()).isTrue();
        verify(mediaRepository).save(next);
    }

    @Test
    void delete_rejectsImageBelongingToAnotherProduct() {
        // Arrange
        UUID mediaId = UUID.randomUUID();
        ProductMedia other = new ProductMedia(tenantId, UUID.randomUUID(), "pid-x", "https://cdn/x.png");
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(other));

        // Act & Assert
        assertThatThrownBy(() -> service.delete(productId, mediaId, tenantId))
                .isInstanceOf(NotFoundException.class);
        verify(mediaStorage, never()).delete(any());
        verify(mediaRepository, never()).deleteById(any());
    }

    @Test
    void list_returnsImages_whenProductOwned() {
        // Arrange
        ProductMedia m = new ProductMedia(tenantId, productId, "pid-1", "https://cdn/img1.png");
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(mediaRepository.findByProductIdOrderBySortOrderAsc(productId)).thenReturn(List.of(m));

        // Act
        List<MediaResponse> result = service.list(productId, tenantId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).url()).isEqualTo("https://cdn/img1.png");
    }

    @Test
    void list_rejectsWhenProductNotInTenant() {
        // Arrange
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.list(productId, tenantId))
                .isInstanceOf(NotFoundException.class);
        verify(mediaRepository, never()).findByProductIdOrderBySortOrderAsc(eq(productId));
    }
}
