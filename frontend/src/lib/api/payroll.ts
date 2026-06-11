import apiClient from './client'
import type { ApiResponse, PageResponse, PayRun, PayRunDetail, Payslip } from '@/types'

export interface PayRunCreatePayload {
  periodStart: string
  periodEnd: string
  label?: string
}

export interface PayslipAdjustPayload {
  otherDeductions: number
  note?: string
}

const BASE = '/api/v1/admin/payroll'

/** Admin HRM — payroll. Plan-gated (Professional / Enterprise) on the backend. */
export const adminPayrollApi = {
  listRuns: (page = 0) => apiClient.get<ApiResponse<PageResponse<PayRun>>>(`${BASE}/runs`, { params: { page } }),
  generate: (payload: PayRunCreatePayload) => apiClient.post<ApiResponse<PayRunDetail>>(`${BASE}/runs`, payload),
  getRun: (id: string) => apiClient.get<ApiResponse<PayRunDetail>>(`${BASE}/runs/${id}`),
  finalise: (id: string) => apiClient.post<ApiResponse<PayRun>>(`${BASE}/runs/${id}/finalise`),
  pay: (id: string) => apiClient.post<ApiResponse<PayRun>>(`${BASE}/runs/${id}/pay`),
  adjustPayslip: (id: string, payload: PayslipAdjustPayload) =>
    apiClient.put<ApiResponse<Payslip>>(`${BASE}/payslips/${id}`, payload),
}
