import apiClient from './client'
import type { ApiResponse, BillingInterval, Plan } from '@/types'

const BASE = '/api/v1/platform/plans'

export interface UpsertPlanPayload {
  code: string
  name: string
  description?: string
  price: number
  currency: string
  billingInterval: BillingInterval
  maxProducts?: number | null
  maxStaff?: number | null
  active: boolean
}

export const planApi = {
  list: () => apiClient.get<ApiResponse<Plan[]>>(BASE),
  create: (payload: UpsertPlanPayload) => apiClient.post<ApiResponse<Plan>>(BASE, payload),
  update: (id: string, payload: UpsertPlanPayload) =>
    apiClient.put<ApiResponse<Plan>>(`${BASE}/${id}`, payload),
  activate: (id: string) => apiClient.patch<ApiResponse<Plan>>(`${BASE}/${id}/activate`),
  deactivate: (id: string) => apiClient.patch<ApiResponse<Plan>>(`${BASE}/${id}/deactivate`),
}
