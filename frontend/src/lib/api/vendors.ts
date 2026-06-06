import apiClient from './client'
import type { ApiResponse, PageResponse, Vendor, VendorStatus } from '@/types'

export interface ApplyVendorPayload {
  storeName: string
  description?: string
}

export const vendorApi = {
  apply: (payload: ApplyVendorPayload) =>
    apiClient.post<ApiResponse<Vendor>>('/api/v1/vendor/apply', payload),
  me: () => apiClient.get<ApiResponse<Vendor>>('/api/v1/vendor/me'),
}

const ADMIN = '/api/v1/admin/vendors'

export const adminVendorApi = {
  list: (params: { status?: VendorStatus | ''; page?: number } = {}) =>
    apiClient.get<ApiResponse<PageResponse<Vendor>>>(ADMIN, {
      params: { page: params.page ?? 0, ...(params.status ? { status: params.status } : {}) },
    }),
  approve: (id: string) => apiClient.patch<ApiResponse<Vendor>>(`${ADMIN}/${id}/approve`),
  reject: (id: string) => apiClient.patch<ApiResponse<Vendor>>(`${ADMIN}/${id}/reject`),
  suspend: (id: string) => apiClient.patch<ApiResponse<Vendor>>(`${ADMIN}/${id}/suspend`),
  reinstate: (id: string) => apiClient.patch<ApiResponse<Vendor>>(`${ADMIN}/${id}/reinstate`),
  setCommission: (id: string, rate: number) =>
    apiClient.patch<ApiResponse<Vendor>>(`${ADMIN}/${id}/commission`, { rate }),
}
