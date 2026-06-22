import apiClient from './client'
import type { ApiResponse, FeatureFlag } from '@/types'

const BASE = '/api/v1/platform/flags'

export interface UpsertFeatureFlagPayload {
  flagKey: string
  description?: string
  enabled: boolean
}

export const featureFlagApi = {
  list: () => apiClient.get<ApiResponse<FeatureFlag[]>>(BASE),
  create: (payload: UpsertFeatureFlagPayload) => apiClient.post<ApiResponse<FeatureFlag>>(BASE, payload),
  update: (id: string, payload: UpsertFeatureFlagPayload) =>
    apiClient.put<ApiResponse<FeatureFlag>>(`${BASE}/${id}`, payload),
  enable: (id: string) => apiClient.patch<ApiResponse<FeatureFlag>>(`${BASE}/${id}/enable`),
  disable: (id: string) => apiClient.patch<ApiResponse<FeatureFlag>>(`${BASE}/${id}/disable`),
  remove: (id: string) => apiClient.delete<ApiResponse<void>>(`${BASE}/${id}`),
}
