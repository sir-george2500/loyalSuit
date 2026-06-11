package com.loyalsuit.modules.hrm.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.hrm.application.dto.AttendanceRequest;
import com.loyalsuit.modules.hrm.application.dto.AttendanceResponse;
import com.loyalsuit.modules.hrm.application.dto.TimesheetResponse;
import com.loyalsuit.modules.hrm.domain.Attendance;
import com.loyalsuit.modules.hrm.domain.AttendanceStatus;
import com.loyalsuit.modules.hrm.domain.Employee;
import com.loyalsuit.modules.hrm.domain.EmploymentType;
import com.loyalsuit.modules.hrm.domain.port.AttendanceRepository;
import com.loyalsuit.modules.hrm.domain.port.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock private AttendanceRepository attendanceRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private HrmAccess hrmAccess;

    @InjectMocks private AttendanceService service;

    private UUID tenantId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
    }

    private void employeeExists() {
        when(employeeRepository.findByIdAndTenantId(employeeId, tenantId))
                .thenReturn(Optional.of(new Employee(tenantId, "Casey Clerk", "Cashier",
                        EmploymentType.FULL_TIME, LocalDate.of(2026, 1, 1), new BigDecimal("2500.00"))));
    }

    private AttendanceRequest request(Instant checkIn, Instant checkOut) {
        AttendanceRequest request = new AttendanceRequest();
        request.setEmployeeId(employeeId);
        request.setWorkDate(LocalDate.of(2026, 6, 1));
        request.setStatus(AttendanceStatus.PRESENT);
        request.setCheckIn(checkIn);
        request.setCheckOut(checkOut);
        return request;
    }

    @Test
    void record_createsADay_andDerivesHours() {
        // Arrange — no record for that day yet
        employeeExists();
        Instant in = Instant.parse("2026-06-01T08:00:00Z");
        Instant out = in.plus(8, ChronoUnit.HOURS);
        when(attendanceRepository.findByTenantIdAndEmployeeIdAndWorkDate(tenantId, employeeId, LocalDate.of(2026, 6, 1)))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        AttendanceResponse response = service.record(tenantId, request(in, out));

        // Assert
        assertThat(response.status()).isEqualTo("PRESENT");
        assertThat(response.hoursWorked()).isEqualByComparingTo("8.00");
    }

    @Test
    void record_rejectsCheckOutBeforeCheckIn() {
        // Arrange
        employeeExists();
        Instant in = Instant.parse("2026-06-01T17:00:00Z");
        Instant out = Instant.parse("2026-06-01T08:00:00Z");

        // Act & Assert
        assertThatThrownBy(() -> service.record(tenantId, request(in, out)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Check-out must be after check-in");
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void clockIn_stampsTheEmployeeIn() {
        // Arrange
        employeeExists();
        when(attendanceRepository.findByTenantIdAndEmployeeIdAndWorkDate(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        AttendanceResponse response = service.clockIn(tenantId, employeeId);

        // Assert
        assertThat(response.checkIn()).isNotNull();
        assertThat(response.checkOut()).isNull();
    }

    @Test
    void clockIn_isRejected_whenAlreadyClockedIn() {
        // Arrange — today's record already has a check-in
        employeeExists();
        Attendance existing = new Attendance(tenantId, employeeId, LocalDate.now(), AttendanceStatus.PRESENT);
        existing.setCheckIn(Instant.now());
        when(attendanceRepository.findByTenantIdAndEmployeeIdAndWorkDate(any(), any(), any()))
                .thenReturn(Optional.of(existing));

        // Act & Assert
        assertThatThrownBy(() -> service.clockIn(tenantId, employeeId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void clockOut_isRejected_whenNotClockedIn() {
        // Arrange — no record for today
        employeeExists();
        when(attendanceRepository.findByTenantIdAndEmployeeIdAndWorkDate(any(), any(), any()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.clockOut(tenantId, employeeId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Not clocked in");
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void timesheet_sumsHoursAndCountsDaysPresent() {
        // Arrange — two worked days, four hours each
        employeeExists();
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        when(attendanceRepository.findTimesheet(tenantId, employeeId, from, to))
                .thenReturn(List.of(workedDay(from, 4), workedDay(from.plusDays(1), 4)));

        // Act
        TimesheetResponse sheet = service.timesheet(tenantId, employeeId, from, to);

        // Assert
        assertThat(sheet.daysPresent()).isEqualTo(2);
        assertThat(sheet.totalHours()).isEqualByComparingTo("8.00");
        assertThat(sheet.entries()).hasSize(2);
    }

    @Test
    void record_isBlocked_whenHrmIsNotOnThePlan() {
        // Arrange
        doThrow(new BusinessException("upgrade", HttpStatus.FORBIDDEN)).when(hrmAccess).require(tenantId);

        // Act & Assert
        assertThatThrownBy(() -> service.record(tenantId, request(null, null)))
                .isInstanceOf(BusinessException.class);
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void clockIn_anUnknownEmployee_is404() {
        // Arrange — plan is fine, but the employee isn't in this tenant
        when(employeeRepository.findByIdAndTenantId(employeeId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.clockIn(tenantId, employeeId)).isInstanceOf(NotFoundException.class);
        verify(attendanceRepository, never()).save(any());
    }

    private Attendance workedDay(LocalDate date, int hours) {
        Attendance a = new Attendance(tenantId, employeeId, date, AttendanceStatus.PRESENT);
        Instant in = date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC).plus(8, ChronoUnit.HOURS);
        a.setCheckIn(in);
        a.setCheckOut(in.plus(hours, ChronoUnit.HOURS));
        return a;
    }
}
