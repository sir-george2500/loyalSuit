import apiClient from './client'
import type { ApiResponse, PageResponse, RolePermissions, StaffMember, UserRole } from '@/types'

const BASE = '/api/v1/admin/staff'

export const staffApi = {
  list: (params: { page?: number } = {}) =>
    apiClient.get<ApiResponse<PageResponse<StaffMember>>>(BASE, { params: { page: params.page ?? 0 } }),
  changeRole: (id: string, role: UserRole) =>
    apiClient.patch<ApiResponse<StaffMember>>(`${BASE}/${id}/role`, { role }),
  activate: (id: string) => apiClient.patch<ApiResponse<StaffMember>>(`${BASE}/${id}/activate`),
  deactivate: (id: string) => apiClient.patch<ApiResponse<StaffMember>>(`${BASE}/${id}/deactivate`),
}

export const roleApi = {
  list: () => apiClient.get<ApiResponse<RolePermissions[]>>('/api/v1/admin/roles'),
}
