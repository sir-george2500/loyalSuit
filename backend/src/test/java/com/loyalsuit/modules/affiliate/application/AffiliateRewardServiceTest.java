package com.loyalsuit.modules.affiliate.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.affiliate.application.dto.AffiliateMeResponse;
import com.loyalsuit.modules.affiliate.domain.Affiliate;
import com.loyalsuit.modules.affiliate.domain.AffiliateReward;
import com.loyalsuit.modules.affiliate.domain.RewardStatus;
import com.loyalsuit.modules.affiliate.domain.port.AffiliateRepository;
import com.loyalsuit.modules.affiliate.domain.port.AffiliateRewardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class AffiliateRewardServiceTest {

    @Mock private AffiliateRepository affiliateRepository;
    @Mock private AffiliateRewardRepository rewardRepository;

    @InjectMocks private AffiliateRewardService service;

    private UUID tenantId;
    private UUID affiliateId;
    private UUID userId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        affiliateId = UUID.randomUUID();
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    private Affiliate affiliate(String rate) {
        Affiliate affiliate = new Affiliate(tenantId, userId, "RILEY", new BigDecimal(rate));
        ReflectionTestUtils.setField(affiliate, "id", affiliateId);
        return affiliate;
    }

    @Test
    void earnForOrder_computesTheReward_atTheAffiliatesRate() {
        // Arrange — 5% of a $200 order = $10
        when(rewardRepository.existsByOrderId(orderId)).thenReturn(false);
        when(affiliateRepository.findByIdAndTenantId(affiliateId, tenantId)).thenReturn(Optional.of(affiliate("5.00")));

        // Act
        service.earnForOrder(tenantId, affiliateId, orderId, "ORD-1", new BigDecimal("200.00"));

        // Assert
        ArgumentCaptor<AffiliateReward> reward = ArgumentCaptor.forClass(AffiliateReward.class);
        verify(rewardRepository).save(reward.capture());
        assertThat(reward.getValue().getRewardAmount()).isEqualByComparingTo("10.00");
        assertThat(reward.getValue().getStatus()).isEqualTo(RewardStatus.EARNED);
    }

    @Test
    void earnForOrder_isIdempotentPerOrder() {
        // Arrange
        when(rewardRepository.existsByOrderId(orderId)).thenReturn(true);

        // Act
        service.earnForOrder(tenantId, affiliateId, orderId, "ORD-1", new BigDecimal("200.00"));

        // Assert
        verify(rewardRepository, never()).save(any());
    }

    @Test
    void earnForOrder_withoutAnAffiliate_isANoOp() {
        // Act — order had no referral
        service.earnForOrder(tenantId, null, orderId, "ORD-1", new BigDecimal("200.00"));

        // Assert
        verify(rewardRepository, never()).existsByOrderId(any());
        verify(rewardRepository, never()).save(any());
    }

    @Test
    void reverseForOrder_reversesTheEarnedReward() {
        // Arrange
        AffiliateReward earned = new AffiliateReward(tenantId, affiliateId, orderId, "ORD-1",
                new BigDecimal("200.00"), new BigDecimal("5.00"));
        when(rewardRepository.findByOrderId(orderId)).thenReturn(List.of(earned));
        when(rewardRepository.save(any(AffiliateReward.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        service.reverseForOrder(tenantId, orderId);

        // Assert
        assertThat(earned.getStatus()).isEqualTo(RewardStatus.REVERSED);
        verify(rewardRepository).save(earned);
    }

    @Test
    void me_returnsTheAffiliatesEarnings() {
        // Arrange
        when(affiliateRepository.findByUserId(userId)).thenReturn(Optional.of(affiliate("5.00")));
        when(rewardRepository.sumRewardAmount(tenantId, affiliateId, RewardStatus.EARNED)).thenReturn(new BigDecimal("42.00"));
        when(rewardRepository.sumRewardAmount(tenantId, affiliateId, RewardStatus.REVERSED)).thenReturn(BigDecimal.ZERO);

        // Act
        AffiliateMeResponse me = service.me(tenantId, userId);

        // Assert
        assertThat(me.code()).isEqualTo("RILEY");
        assertThat(me.earned()).isEqualByComparingTo("42.00");
    }

    @Test
    void me_whenTheUserIsNotAnAffiliate_is404() {
        // Arrange
        when(affiliateRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.me(tenantId, userId)).isInstanceOf(NotFoundException.class);
    }
}
