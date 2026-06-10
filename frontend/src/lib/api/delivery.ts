import apiClient from './client'
import type { ApiResponse, Delivery, DeliveryStatus, DeliveryAgent, PageResponse, VehicleType } from '@/types'

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

/** A delivery agent's own profile, work queue, and status updates. */
export const deliveryAgentApi = {
  me: () => apiClient.get<ApiResponse<DeliveryAgent>>('/api/v1/delivery/me'),
  assignments: (page = 0) =>
    apiClient.get<ApiResponse<PageResponse<Delivery>>>('/api/v1/delivery/assignments', { params: { page } }),
  advance: (id: string, status: DeliveryStatus, note?: string) =>
    apiClient.patch<ApiResponse<Delivery>>(`/api/v1/delivery/assignments/${id}/status`, { status, note }),
}

export interface AssignDeliveryPayload {
  orderNumber: string
  agentId: string
}

const ADMIN_DELIVERIES = '/api/v1/admin/deliveries'

/** Dispatch desk: assign orders to couriers and advance the delivery lifecycle. */
export const adminDeliveryApi = {
  list: (params: { status?: DeliveryStatus | ''; page?: number } = {}) =>
    apiClient.get<ApiResponse<PageResponse<Delivery>>>(ADMIN_DELIVERIES, {
      params: { page: params.page ?? 0, ...(params.status ? { status: params.status } : {}) },
    }),
  get: (id: string) => apiClient.get<ApiResponse<Delivery>>(`${ADMIN_DELIVERIES}/${id}`),
  assign: (payload: AssignDeliveryPayload) =>
    apiClient.post<ApiResponse<Delivery>>(`${ADMIN_DELIVERIES}/assign`, payload),
  advance: (id: string, status: DeliveryStatus, note?: string) =>
    apiClient.patch<ApiResponse<Delivery>>(`${ADMIN_DELIVERIES}/${id}/status`, { status, note }),
}
