import apiClient from './client'
import type { Affiliate, ApiResponse, PageResponse } from '@/types'

export interface RegisterAffiliatePayload {
  email: string
  code: string
  rewardRate: number
}

export interface UpdateAffiliatePayload {
  code: string
  rewardRate: number
}

const ADMIN = '/api/v1/admin/affiliates'

/** Admin management of the affiliate / referral program. */
export const adminAffiliateApi = {
  list: (page = 0) => apiClient.get<ApiResponse<PageResponse<Affiliate>>>(ADMIN, { params: { page } }),
  register: (payload: RegisterAffiliatePayload) => apiClient.post<ApiResponse<Affiliate>>(ADMIN, payload),
  update: (id: string, payload: UpdateAffiliatePayload) => apiClient.put<ApiResponse<Affiliate>>(`${ADMIN}/${id}`, payload),
  activate: (id: string) => apiClient.patch<ApiResponse<Affiliate>>(`${ADMIN}/${id}/activate`),
  deactivate: (id: string) => apiClient.patch<ApiResponse<Affiliate>>(`${ADMIN}/${id}/deactivate`),
}
