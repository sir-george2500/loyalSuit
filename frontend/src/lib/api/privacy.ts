import apiClient from './client'
import type { ApiResponse } from '@/types'

/** GDPR data-subject rights for the signed-in user. */
export const privacyApi = {
  exportData: () =>
    apiClient.get<ApiResponse<Record<string, unknown>>>('/api/v1/privacy/export'),

  deleteAccount: (password: string) =>
    apiClient.post<ApiResponse<null>>('/api/v1/privacy/delete-account', { password }),
}
