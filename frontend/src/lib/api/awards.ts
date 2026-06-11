import apiClient from './client'
import type { ApiResponse, Award, AwardCategory } from '@/types'

export interface AwardPayload {
  employeeId: string
  title: string
  category: AwardCategory
  awardedOn: string
  description?: string
  awardedBy?: string
}

const ADMIN = '/api/v1/admin/awards'

/** Admin HRM — employee awards / recognition. Plan-gated (Professional / Enterprise) on the backend. */
export const adminAwardApi = {
  list: (employeeId: string) => apiClient.get<ApiResponse<Award[]>>(ADMIN, { params: { employeeId } }),
  create: (payload: AwardPayload) => apiClient.post<ApiResponse<Award>>(ADMIN, payload),
  remove: (id: string) => apiClient.delete<ApiResponse<void>>(`${ADMIN}/${id}`),
}
