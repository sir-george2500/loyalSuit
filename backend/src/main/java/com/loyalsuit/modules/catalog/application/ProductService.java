package com.loyalsuit.modules.catalog.application;

import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.catalog.application.dto.CreateProductRequest;
import com.loyalsuit.modules.catalog.application.dto.ProductResponse;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.ProductStatus;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public PageResponse<ProductResponse> listByTenant(UUID tenantId, Pageable pageable) {
        return new PageResponse<>(
                productRepository.findByTenantId(tenantId, pageable)
                        .map(ProductResponse::new)
        );
    }

    public PageResponse<ProductResponse> listActiveByTenant(UUID tenantId, Pageable pageable) {
        return new PageResponse<>(
                productRepository.findByTenantIdAndStatus(tenantId, ProductStatus.ACTIVE, pageable)
                        .map(ProductResponse::new)
        );
    }

    public PageResponse<ProductResponse> listByCategory(UUID categoryId, UUID tenantId, Pageable pageable) {
        return new PageResponse<>(
                productRepository.findByCategoryIdAndTenantId(categoryId, tenantId, pageable)
                        .map(ProductResponse::new)
        );
    }

    public ProductResponse getById(UUID id, UUID tenantId) {
        return productRepository.findByIdAndTenantId(id, tenantId)
                .map(ProductResponse::new)
                .orElseThrow(() -> new NotFoundException("Product", id));
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request, UUID tenantId) {
        if (productRepository.existsBySlugAndTenantId(request.getSlug(), tenantId)) {
            throw new ConflictException("Product slug already exists: " + request.getSlug());
        }

        Product product = new Product(tenantId, request.getName(), request.getSlug(), request.getPrice());
        product.setDescription(request.getDescription());
        product.setCompareAtPrice(request.getCompareAtPrice());
        product.setSku(request.getSku());
        product.setBarcode(request.getBarcode());
        product.setCategoryId(request.getCategoryId());
        product.setVendorId(request.getVendorId());
        product.setDigital(request.isDigital());

        return new ProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse publish(UUID id, UUID tenantId) {
        Product product = productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Product", id));
        product.activate();
        return new ProductResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(UUID id, UUID tenantId) {
        productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Product", id));
        productRepository.deleteById(id);
    }
}
