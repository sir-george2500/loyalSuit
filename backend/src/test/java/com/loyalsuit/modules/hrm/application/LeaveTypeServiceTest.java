package com.loyalsuit.modules.hrm.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.modules.hrm.application.dto.LeaveTypeRequest;
import com.loyalsuit.modules.hrm.application.dto.LeaveTypeResponse;
import com.loyalsuit.modules.hrm.domain.LeaveType;
import com.loyalsuit.modules.hrm.domain.port.LeaveTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveTypeServiceTest {

    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private HrmAccess hrmAccess;

    @InjectMocks private LeaveTypeService service;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

    private LeaveTypeRequest request() {
        LeaveTypeRequest request = new LeaveTypeRequest();
        request.setName("Annual");
        request.setAnnualAllowanceDays(20);
        return request;
    }

    @Test
    void create_addsALeaveType_defaultingToPaidAndActive() {
        // Arrange — paid/active omitted in the request
        when(leaveTypeRepository.save(any(LeaveType.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        LeaveTypeResponse type = service.create(tenantId, request());

        // Assert
        assertThat(type.name()).isEqualTo("Annual");
        assertThat(type.annualAllowanceDays()).isEqualTo(20);
        assertThat(type.paid()).isTrue();
        assertThat(type.active()).isTrue();
    }

    @Test
    void create_isBlocked_whenHrmIsNotOnThePlan() {
        // Arrange
        doThrow(new BusinessException("upgrade", HttpStatus.FORBIDDEN)).when(hrmAccess).require(tenantId);

        // Act & Assert
        assertThatThrownBy(() -> service.create(tenantId, request())).isInstanceOf(BusinessException.class);
        verify(leaveTypeRepository, never()).save(any());
    }
}
