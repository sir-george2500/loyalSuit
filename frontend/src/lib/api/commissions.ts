import apiClient from './client'
import type { ApiResponse, CommissionEntry, PageResponse, VendorEarnings } from '@/types'

/** A vendor's own earnings: balance + commission ledger. Scoped server-side to the caller. */
export const vendorCommissionApi = {
  earnings: () => apiClient.get<ApiResponse<VendorEarnings>>('/api/v1/vendor/earnings'),
  ledger: (page = 0) =>
    apiClient.get<ApiResponse<PageResponse<CommissionEntry>>>('/api/v1/vendor/commissions', {
      params: { page },
    }),
}

/** The tenant-wide commission ledger (owner-only). */
export const adminCommissionApi = {
  list: (page = 0) =>
    apiClient.get<ApiResponse<PageResponse<CommissionEntry>>>('/api/v1/admin/commissions', {
      params: { page },
    }),
}
