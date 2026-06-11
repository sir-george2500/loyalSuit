import apiClient from './client'
import type { ApiResponse, Attendance, AttendanceStatus, Timesheet } from '@/types'

export interface AttendancePayload {
  employeeId: string
  workDate: string
  status: AttendanceStatus
  checkIn?: string
  checkOut?: string
  note?: string
}

const ADMIN = '/api/v1/admin/attendance'

/** Admin HRM — attendance and timesheets. Plan-gated (Professional / Enterprise) on the backend. */
export const adminAttendanceApi = {
  record: (payload: AttendancePayload) => apiClient.post<ApiResponse<Attendance>>(ADMIN, payload),
  clockIn: (employeeId: string) => apiClient.post<ApiResponse<Attendance>>(`${ADMIN}/${employeeId}/clock-in`),
  clockOut: (employeeId: string) => apiClient.post<ApiResponse<Attendance>>(`${ADMIN}/${employeeId}/clock-out`),
  timesheet: (employeeId: string, from: string, to: string) =>
    apiClient.get<ApiResponse<Timesheet>>(`${ADMIN}/timesheet`, { params: { employeeId, from, to } }),
}
