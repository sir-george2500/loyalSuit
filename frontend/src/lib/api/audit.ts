import apiClient from './client'
import type { ApiResponse, AuditAction, AuditLog, PageResponse } from '@/types'

export interface AuditQuery {
  page?: number
  size?: number
  action?: AuditAction | ''
}

export const auditApi = {
  list: ({ page = 0, size = 25, action = '' }: AuditQuery = {}) =>
    apiClient.get<ApiResponse<PageResponse<AuditLog>>>('/api/v1/audit', {
      params: {
        page,
        size,
        ...(action ? { action } : {}),
      },
    }),
}
