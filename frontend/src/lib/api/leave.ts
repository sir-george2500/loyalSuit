import apiClient from './client'
import type {
  ApiResponse, LeaveBalance, LeaveRequest, LeaveStatus, LeaveType, PageResponse,
} from '@/types'

export interface LeaveTypePayload {
  name: string
  annualAllowanceDays: number
  paid?: boolean
  active?: boolean
}

export interface LeaveRequestPayload {
  employeeId: string
  leaveTypeId: string
  startDate: string
  endDate: string
  reason?: string
}

const TYPES = '/api/v1/admin/leave-types'
const LEAVE = '/api/v1/admin/leave'

/** Admin HRM — leave types. Plan-gated (Professional / Enterprise) on the backend. */
export const adminLeaveTypeApi = {
  list: () => apiClient.get<ApiResponse<LeaveType[]>>(TYPES),
  create: (payload: LeaveTypePayload) => apiClient.post<ApiResponse<LeaveType>>(TYPES, payload),
  update: (id: string, payload: LeaveTypePayload) => apiClient.put<ApiResponse<LeaveType>>(`${TYPES}/${id}`, payload),
}

/** Admin HRM — leave requests and balances. */
export const adminLeaveApi = {
  list: (page = 0, status?: LeaveStatus) =>
    apiClient.get<ApiResponse<PageResponse<LeaveRequest>>>(LEAVE, { params: { page, status } }),
  create: (payload: LeaveRequestPayload) => apiClient.post<ApiResponse<LeaveRequest>>(LEAVE, payload),
  approve: (id: string, note?: string) => apiClient.post<ApiResponse<LeaveRequest>>(`${LEAVE}/${id}/approve`, { note }),
  reject: (id: string, note?: string) => apiClient.post<ApiResponse<LeaveRequest>>(`${LEAVE}/${id}/reject`, { note }),
  balances: (employeeId: string) =>
    apiClient.get<ApiResponse<LeaveBalance[]>>(`${LEAVE}/balances`, { params: { employeeId } }),
}
