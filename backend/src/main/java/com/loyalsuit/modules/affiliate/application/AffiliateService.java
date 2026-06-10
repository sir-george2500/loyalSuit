package com.loyalsuit.modules.affiliate.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.affiliate.application.dto.AffiliateResponse;
import com.loyalsuit.modules.affiliate.application.dto.RegisterAffiliateRequest;
import com.loyalsuit.modules.affiliate.application.dto.UpdateAffiliateRequest;
import com.loyalsuit.modules.affiliate.domain.Affiliate;
import com.loyalsuit.modules.affiliate.domain.port.AffiliateRepository;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The affiliate registry: an admin enrols a tenant user as an affiliate with a unique
 * referral code and a reward rate. Codes are normalised and unique per tenant; one profile
 * per user. Names/emails are read from the user (resolved in batch when listing). The reward
 * ledger and attribution are layered on in the next slice.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AffiliateService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final AffiliateRepository affiliateRepository;
    private final AppUserRepository userRepository;

    @Transactional
    public AffiliateResponse register(UUID tenantId, RegisterAffiliateRequest request) {
        AppUser user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new BusinessException("No user found with that email"));
        if (!tenantId.equals(user.getTenantId())) {
            throw new BusinessException("That user does not belong to this store");
        }
        if (affiliateRepository.existsByUserId(user.getId())) {
            throw new ConflictException("That user is already an affiliate");
        }
        String code = Affiliate.normalize(request.getCode());
        if (affiliateRepository.existsByTenantIdAndCode(tenantId, code)) {
            throw new ConflictException("An affiliate with code " + code + " already exists");
        }
        validateRate(request.getRewardRate());

        Affiliate affiliate = affiliateRepository.save(
                new Affiliate(tenantId, user.getId(), code, request.getRewardRate()));
        return AffiliateResponse.from(affiliate, displayName(user), user.getEmail());
    }

    @Transactional
    public AffiliateResponse update(UUID id, UUID tenantId, UpdateAffiliateRequest request) {
        Affiliate affiliate = load(id, tenantId);
        String code = Affiliate.normalize(request.getCode());
        if (!code.equals(affiliate.getCode()) && affiliateRepository.existsByTenantIdAndCode(tenantId, code)) {
            throw new ConflictException("An affiliate with code " + code + " already exists");
        }
        validateRate(request.getRewardRate());
        affiliate.setCode(code);
        affiliate.setRewardRate(request.getRewardRate());
        return toResponse(affiliateRepository.save(affiliate));
    }

    @Transactional
    public AffiliateResponse setActive(UUID id, UUID tenantId, boolean active) {
        Affiliate affiliate = load(id, tenantId);
        affiliate.setActive(active);
        return toResponse(affiliateRepository.save(affiliate));
    }

    public PageResponse<AffiliateResponse> list(UUID tenantId, Pageable pageable) {
        Page<Affiliate> page = affiliateRepository.findByTenantId(tenantId, pageable);
        Map<UUID, AppUser> users = userRepository.findByIdInAndTenantId(
                page.map(Affiliate::getUserId).toList(), tenantId).stream()
                .collect(Collectors.toMap(AppUser::getId, Function.identity()));
        return new PageResponse<>(page.map(affiliate -> {
            AppUser user = users.get(affiliate.getUserId());
            return AffiliateResponse.from(affiliate,
                    user == null ? null : displayName(user),
                    user == null ? null : user.getEmail());
        }));
    }

    /**
     * Collaboration API for checkout: the affiliate id a referral code attributes an order
     * to — empty if the code is unknown/inactive or would be a self-referral (the buyer is
     * the affiliate). A bad code simply isn't attributed; it never fails the checkout.
     */
    public java.util.Optional<UUID> attributableAffiliateId(UUID tenantId, String code, UUID buyerUserId) {
        if (!StringUtils.hasText(code)) {
            return java.util.Optional.empty();
        }
        return affiliateRepository.findByTenantIdAndCode(tenantId, Affiliate.normalize(code))
                .filter(Affiliate::isActive)
                .filter(affiliate -> !affiliate.getUserId().equals(buyerUserId))
                .map(Affiliate::getId);
    }

    private AffiliateResponse toResponse(Affiliate affiliate) {
        return userRepository.findById(affiliate.getUserId())
                .map(user -> AffiliateResponse.from(affiliate, displayName(user), user.getEmail()))
                .orElseGet(() -> AffiliateResponse.from(affiliate, null, null));
    }

    private static void validateRate(BigDecimal rate) {
        if (rate.signum() <= 0 || rate.compareTo(HUNDRED) > 0) {
            throw new BusinessException("The reward rate must be between 0 and 100");
        }
    }

    private Affiliate load(UUID id, UUID tenantId) {
        return affiliateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Affiliate", id));
    }

    private static String displayName(AppUser user) {
        return StringUtils.hasText(user.getFullName()) ? user.getFullName() : user.getEmail();
    }
}
