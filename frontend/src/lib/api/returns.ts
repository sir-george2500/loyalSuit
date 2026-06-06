import apiClient from './client'
import type { ApiResponse, PageResponse, ReturnResponse, ReturnStatus } from '@/types'

export const returnsApi = {
  /** Public: a guest requests a return for a delivered order (verified by email). */
  request: (slug: string, orderNumber: string, payload: { email: string; reason: string }) =>
    apiClient.post<ApiResponse<ReturnResponse>>(
      `/api/v1/store/${encodeURIComponent(slug)}/orders/${encodeURIComponent(orderNumber)}/returns`,
      payload
    ),

  // ---- admin ----
  list: (params: { status?: ReturnStatus | ''; page?: number } = {}) =>
    apiClient.get<ApiResponse<PageResponse<ReturnResponse>>>('/api/v1/returns', {
      params: { page: params.page ?? 0, ...(params.status ? { status: params.status } : {}) },
    }),
  approve: (id: string) => apiClient.patch<ApiResponse<ReturnResponse>>(`/api/v1/returns/${id}/approve`),
  reject: (id: string, note: string) =>
    apiClient.patch<ApiResponse<ReturnResponse>>(`/api/v1/returns/${id}/reject`, { note }),
}
