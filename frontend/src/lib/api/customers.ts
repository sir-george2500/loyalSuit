import apiClient from './client'
import type { ApiResponse, Customer, PageResponse } from '@/types'

const BASE = '/api/v1/admin/customers'

export const customerApi = {
  list: (params: { search?: string; page?: number } = {}) =>
    apiClient.get<ApiResponse<PageResponse<Customer>>>(BASE, {
      params: { page: params.page ?? 0, ...(params.search ? { search: params.search } : {}) },
    }),
}
