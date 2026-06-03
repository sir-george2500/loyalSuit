import apiClient from './client'
import type { ApiResponse, DashboardStats } from '@/types'

export const dashboardApi = {
  getStats: () => apiClient.get<ApiResponse<DashboardStats>>('/api/v1/dashboard/stats'),
}
