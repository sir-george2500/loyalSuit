package com.loyalsuit.modules.notifications.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.notifications.domain.Notification;
import com.loyalsuit.modules.notifications.domain.NotificationType;
import com.loyalsuit.modules.notifications.domain.port.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;

    @InjectMocks private NotificationService service;

    private UUID tenantId;
    private UUID recipientId;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        recipientId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
    }

    private Notification notification(UUID recipient) {
        Notification n = new Notification(tenantId, recipient, NotificationType.ORDER_PLACED, "Order placed", "body", "/x");
        ReflectionTestUtils.setField(n, "id", notificationId);
        return n;
    }

    @Test
    void notify_createsAnInboxRow() {
        // Act
        service.notify(tenantId, recipientId, NotificationType.PAYOUT_PAID, "Payout paid", "body", "/seller/payouts");

        // Assert
        ArgumentCaptor<Notification> n = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(n.capture());
        assertThat(n.getValue().getRecipientId()).isEqualTo(recipientId);
        assertThat(n.getValue().getType()).isEqualTo(NotificationType.PAYOUT_PAID);
    }

    @Test
    void notify_withNoRecipient_isANoOp() {
        // Act — e.g. a guest order
        service.notify(tenantId, null, NotificationType.ORDER_PLACED, "x", "y", null);

        // Assert
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markRead_marksTheCallersOwnNotification() {
        // Arrange
        Notification n = notification(recipientId);
        when(notificationRepository.findByIdAndTenantId(notificationId, tenantId)).thenReturn(Optional.of(n));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        service.markRead(notificationId, tenantId, recipientId);

        // Assert
        assertThat(n.isRead()).isTrue();
    }

    @Test
    void markRead_someoneElsesNotification_is404() {
        // Arrange — the notification belongs to another user
        when(notificationRepository.findByIdAndTenantId(notificationId, tenantId))
                .thenReturn(Optional.of(notification(UUID.randomUUID())));

        // Act & Assert — not leaked: a 404, not a 403
        assertThatThrownBy(() -> service.markRead(notificationId, tenantId, recipientId))
                .isInstanceOf(NotFoundException.class);
        verify(notificationRepository, never()).save(any());
    }
}
