import apiClient from './client'
import type { ApiResponse, DeliveryAgent, PageResponse, VehicleType } from '@/types'

export interface RegisterDeliveryAgentPayload {
  email: string
  phone: string
  vehicleType: VehicleType
}

export interface UpdateDeliveryAgentPayload {
  phone: string
  vehicleType: VehicleType
}

const ADMIN = '/api/v1/admin/delivery-agents'

/** Admin management of the courier roster. */
export const adminDeliveryAgentApi = {
  list: (params: { active?: boolean; page?: number } = {}) =>
    apiClient.get<ApiResponse<PageResponse<DeliveryAgent>>>(ADMIN, {
      params: { page: params.page ?? 0, ...(params.active === undefined ? {} : { active: params.active }) },
    }),
  register: (payload: RegisterDeliveryAgentPayload) =>
    apiClient.post<ApiResponse<DeliveryAgent>>(ADMIN, payload),
  update: (id: string, payload: UpdateDeliveryAgentPayload) =>
    apiClient.put<ApiResponse<DeliveryAgent>>(`${ADMIN}/${id}`, payload),
  activate: (id: string) => apiClient.patch<ApiResponse<DeliveryAgent>>(`${ADMIN}/${id}/activate`),
  deactivate: (id: string) => apiClient.patch<ApiResponse<DeliveryAgent>>(`${ADMIN}/${id}/deactivate`),
}

/** A delivery agent's own profile. */
export const deliveryAgentApi = {
  me: () => apiClient.get<ApiResponse<DeliveryAgent>>('/api/v1/delivery/me'),
}
