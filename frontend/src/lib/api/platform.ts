import apiClient from './client'
import type { ApiResponse, PageResponse, SubscriptionPlan, SystemHealth, TenantAdmin } from '@/types'

const TENANTS = '/api/v1/platform/tenants'

export const platformTenantApi = {
  list: (params: { page?: number } = {}) =>
    apiClient.get<ApiResponse<PageResponse<TenantAdmin>>>(TENANTS, { params: { page: params.page ?? 0 } }),
  activate: (id: string) => apiClient.patch<ApiResponse<TenantAdmin>>(`${TENANTS}/${id}/activate`),
  deactivate: (id: string) => apiClient.patch<ApiResponse<TenantAdmin>>(`${TENANTS}/${id}/deactivate`),
  changePlan: (id: string, plan: SubscriptionPlan) =>
    apiClient.patch<ApiResponse<TenantAdmin>>(`${TENANTS}/${id}/plan`, { plan }),
}

export const systemHealthApi = {
  get: () => apiClient.get<ApiResponse<SystemHealth>>('/api/v1/platform/system'),
}
