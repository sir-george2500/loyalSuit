import apiClient from './client'
import type { ApiKey, ApiResponse, CreatedApiKey } from '@/types'

const BASE = '/api/v1/admin/api-keys'

export const apiKeyApi = {
  list: () => apiClient.get<ApiResponse<ApiKey[]>>(BASE),
  create: (name: string) => apiClient.post<ApiResponse<CreatedApiKey>>(BASE, { name }),
  revoke: (id: string) => apiClient.delete<ApiResponse<ApiKey>>(`${BASE}/${id}`),
}
