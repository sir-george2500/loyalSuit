import apiClient from './client'
import type { ApiResponse, AppNotification, PageResponse } from '@/types'

/** A user's in-app notification inbox. */
export const notificationApi = {
  list: (page = 0) =>
    apiClient.get<ApiResponse<PageResponse<AppNotification>>>('/api/v1/notifications', { params: { page } }),
  unreadCount: () => apiClient.get<ApiResponse<{ count: number }>>('/api/v1/notifications/unread-count'),
  markRead: (id: string) => apiClient.patch<ApiResponse<void>>(`/api/v1/notifications/${id}/read`),
  markAllRead: () => apiClient.post<ApiResponse<void>>('/api/v1/notifications/read-all'),
}
