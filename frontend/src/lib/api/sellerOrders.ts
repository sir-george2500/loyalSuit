import apiClient from './client'
import type { ApiResponse, PageResponse, VendorOrder } from '@/types'

/** A seller's own orders — the orders containing their products, with only their lines. */
export const sellerOrderApi = {
  list: (page = 0) =>
    apiClient.get<ApiResponse<PageResponse<VendorOrder>>>('/api/v1/vendor/orders', { params: { page } }),
  get: (id: string) => apiClient.get<ApiResponse<VendorOrder>>(`/api/v1/vendor/orders/${id}`),
}
