import apiClient from './client'
import type { ApiResponse, NotificationPreferences } from '@/types'

const BASE = '/api/v1/admin/settings/notifications'

export const notificationSettingsApi = {
  get: () => apiClient.get<ApiResponse<NotificationPreferences>>(BASE),
  update: (payload: NotificationPreferences) =>
    apiClient.put<ApiResponse<NotificationPreferences>>(BASE, payload),
}
