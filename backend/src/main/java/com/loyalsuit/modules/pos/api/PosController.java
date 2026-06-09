package com.loyalsuit.modules.pos.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.pos.application.PosReceiptService;
import com.loyalsuit.modules.pos.application.PosReceiptService.RenderedReceipt;
import com.loyalsuit.modules.pos.application.PosService;
import com.loyalsuit.modules.pos.application.dto.CompleteSaleRequest;
import com.loyalsuit.modules.pos.application.dto.PosProductResponse;
import com.loyalsuit.modules.pos.application.dto.PosSaleResponse;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * The in-store POS terminal API: catalog lookup for the product grid / barcode scan,
 * ringing up a cash sale, and the recent-sales feed. Open to the store-side roles that
 * staff a register; the backend enforces tenant scope from the JWT on every call.
 */
@RestController
@RequestMapping("/api/v1/pos")
@RequiredArgsConstructor
@Tag(name = "POS terminal", description = "In-store point-of-sale")
public class PosController {

    private final PosService posService;
    private final PosReceiptService receiptService;

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "Search active products (name, SKU, or barcode) for the terminal")
    public ResponseEntity<ApiResponse<PageResponse<PosProductResponse>>> products(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 24) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                posService.searchProducts(principal.getTenantId(), q, pageable)));
    }

    @PostMapping("/sales")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "Ring up and complete a cash sale (idempotent on clientSaleId)")
    public ResponseEntity<ApiResponse<PosSaleResponse>> completeSale(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CompleteSaleRequest request) {
        PosSaleResponse sale = posService.completeSale(
                principal.getTenantId(), principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(sale));
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "List recent in-store sales (most recent first)")
    public ResponseEntity<ApiResponse<PageResponse<PosSaleResponse>>> sales(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                posService.listSales(principal.getTenantId(), pageable)));
    }

    @GetMapping("/sales/{id}/receipt")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "Download a sale's receipt as a PDF")
    public ResponseEntity<byte[]> receipt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        RenderedReceipt pdf = receiptService.renderReceipt(principal.getTenantId(), id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename(pdf.fileName()).build());
        return new ResponseEntity<>(pdf.content(), headers, HttpStatus.OK);
    }
}
