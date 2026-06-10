package com.loyalsuit.modules.cart.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.cart.application.dto.AddItemRequest;
import com.loyalsuit.modules.cart.application.dto.CartItemView;
import com.loyalsuit.modules.cart.application.dto.CartView;
import com.loyalsuit.modules.cart.application.dto.UpdateItemRequest;
import com.loyalsuit.modules.cart.domain.Cart;
import com.loyalsuit.modules.cart.domain.CartItem;
import com.loyalsuit.modules.cart.domain.port.CartRepository;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.ProductMedia;
import com.loyalsuit.modules.catalog.domain.ProductStatus;
import com.loyalsuit.modules.catalog.domain.ProductVariant;
import com.loyalsuit.modules.catalog.domain.port.ProductMediaRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductVariantRepository;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Shopping cart, resolved by store slug for anonymous and authenticated shoppers
 * alike. The cart stores only product/variant references and quantities — line
 * prices are <b>always recomputed</b> from current catalog data when the cart is
 * viewed, so a stale or tampered cart can never determine what a shopper is
 * charged. Only ACTIVE products can be added, and lines that later become invalid
 * (deleted or unpublished product/variant) are dropped on the next view.
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final TenantRepository tenantRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductMediaRepository mediaRepository;
    private final CartRepository cartRepository;

    public CartView view(String storeSlug, String token) {
        Tenant tenant = requireStore(storeSlug);
        return recompute(tenant, loadCart(tenant.getId(), token), token);
    }

    public CartView addItem(String storeSlug, String token, AddItemRequest request) {
        Tenant tenant = requireStore(storeSlug);
        // Validate against the live catalog before the line enters the cart.
        validateActiveLine(tenant.getId(), request.getProductId(), request.getVariantId());

        Cart cart = loadCart(tenant.getId(), token);
        cart.addOrIncrement(request.getProductId(), request.getVariantId(), request.getQuantity());
        cartRepository.save(tenant.getId(), token, cart);
        return recompute(tenant, cart, token);
    }

    public CartView updateItem(String storeSlug, String token, UpdateItemRequest request) {
        Tenant tenant = requireStore(storeSlug);
        Cart cart = loadCart(tenant.getId(), token);
        cart.setQuantity(request.getProductId(), request.getVariantId(), request.getQuantity());
        cartRepository.save(tenant.getId(), token, cart);
        return recompute(tenant, cart, token);
    }

    public CartView removeItem(String storeSlug, String token, UUID productId, UUID variantId) {
        Tenant tenant = requireStore(storeSlug);
        Cart cart = loadCart(tenant.getId(), token);
        cart.remove(productId, variantId);
        cartRepository.save(tenant.getId(), token, cart);
        return recompute(tenant, cart, token);
    }

    public void clear(String storeSlug, String token) {
        Tenant tenant = requireStore(storeSlug);
        cartRepository.delete(tenant.getId(), token);
    }

    /** Rebuilds the cart view from current catalog data, dropping invalid lines. */
    private CartView recompute(Tenant tenant, Cart cart, String token) {
        Map<UUID, String> primaryImages = mediaRepository
                .findByProductIdInAndPrimaryTrue(cart.getItems().stream().map(CartItem::getProductId).toList())
                .stream()
                .collect(Collectors.toMap(ProductMedia::getProductId, ProductMedia::getUrl, (a, b) -> a));

        List<CartItemView> views = new ArrayList<>();
        List<CartItem> kept = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int itemCount = 0;

        for (CartItem item : cart.getItems()) {
            Product product = productRepository.findByIdAndTenantId(item.getProductId(), tenant.getId())
                    .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                    .orElse(null);
            if (product == null) {
                continue; // product gone or unpublished — drop the line
            }

            // A product's flash-deal price applies to its base line; variants keep their own price.
            BigDecimal unitPrice = product.effectivePrice(java.time.Instant.now());
            String variantName = null;
            if (item.getVariantId() != null) {
                ProductVariant variant = variantRepository.findById(item.getVariantId())
                        .filter(v -> v.getProductId().equals(product.getId()))
                        .orElse(null);
                if (variant == null) {
                    continue; // variant gone — drop the line
                }
                unitPrice = variant.getPrice();
                variantName = variant.getName();
            }

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            views.add(new CartItemView(
                    product.getId(), item.getVariantId(), product.getName(), product.getSlug(),
                    variantName, primaryImages.get(product.getId()), unitPrice, item.getQuantity(), lineTotal));
            kept.add(item);
            subtotal = subtotal.add(lineTotal);
            itemCount += item.getQuantity();
        }

        // Persist the cleaned cart if any stale lines were dropped.
        if (kept.size() != cart.getItems().size()) {
            cart.setItems(kept);
            cartRepository.save(tenant.getId(), token, cart);
        }

        return new CartView(views, subtotal, itemCount, tenant.getCurrency());
    }

    private void validateActiveLine(UUID tenantId, UUID productId, UUID variantId) {
        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("That product is not available"));
        if (variantId != null) {
            variantRepository.findById(variantId)
                    .filter(v -> v.getProductId().equals(product.getId()))
                    .orElseThrow(() -> new BusinessException("That product option is not available"));
        }
    }

    private Cart loadCart(UUID tenantId, String token) {
        return cartRepository.find(tenantId, token).orElseGet(() -> new Cart(tenantId));
    }

    private Tenant requireStore(String storeSlug) {
        return tenantRepository.findBySlug(storeSlug)
                .filter(Tenant::isActive)
                .orElseThrow(() -> new NotFoundException("Store", storeSlug));
    }
}
