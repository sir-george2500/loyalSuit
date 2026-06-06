import apiClient from './client'
import type { ApiResponse, PageResponse, Payout, PayoutBalance, PayoutStatus } from '@/types'

const VENDOR = '/api/v1/vendor/payouts'
const ADMIN = '/api/v1/admin/payouts'

/** A vendor's own payouts: balance, list, and requesting a new one. Scoped server-side. */
export const vendorPayoutApi = {
  balance: () => apiClient.get<ApiResponse<PayoutBalance>>(`${VENDOR}/balance`),
  list: (page = 0) =>
    apiClient.get<ApiResponse<PageResponse<Payout>>>(VENDOR, { params: { page } }),
  request: (amount: number) => apiClient.post<ApiResponse<Payout>>(VENDOR, { amount }),
}

/** Admin review of payout requests (owner-only). */
export const adminPayoutApi = {
  list: (params: { status?: PayoutStatus | ''; page?: number } = {}) =>
    apiClient.get<ApiResponse<PageResponse<Payout>>>(ADMIN, {
      params: { page: params.page ?? 0, ...(params.status ? { status: params.status } : {}) },
    }),
  pay: (id: string, payload: { reference?: string; note?: string }) =>
    apiClient.patch<ApiResponse<Payout>>(`${ADMIN}/${id}/pay`, payload),
  reject: (id: string, note: string) =>
    apiClient.patch<ApiResponse<Payout>>(`${ADMIN}/${id}/reject`, { note }),
}
