package com.loyalsuit.modules.catalog.api;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.catalog.application.MediaUploadService;
import com.loyalsuit.modules.catalog.application.dto.MediaResponse;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog/products/{productId}/media")
@RequiredArgsConstructor
@Tag(name = "Product Media", description = "Product image gallery")
public class ProductMediaController {

    private final MediaUploadService mediaUploadService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF', 'VENDOR')")
    @Operation(summary = "List a product's images")
    public ResponseEntity<ApiResponse<List<MediaResponse>>> list(
            @PathVariable UUID productId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(mediaUploadService.list(productId, principal.getTenantId())));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "Upload a product image")
    public ResponseEntity<ApiResponse<MediaResponse>> upload(
            @PathVariable UUID productId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("No file was uploaded");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Could not read the uploaded file");
        }
        MediaResponse response = mediaUploadService.upload(productId, principal.getTenantId(), data);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @DeleteMapping("/{mediaId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "Delete a product image")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID productId,
            @PathVariable UUID mediaId,
            @AuthenticationPrincipal UserPrincipal principal) {
        mediaUploadService.delete(productId, mediaId, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.message("Image deleted"));
    }
}
