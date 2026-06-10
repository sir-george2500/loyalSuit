import apiClient from './client'
import type { ApiResponse, DeliveryZone, PageResponse, PickupPoint } from '@/types'

export interface PickupPointPayload {
  name: string
  address?: string
  phone?: string
}

export interface DeliveryZonePayload {
  name: string
  fee: number
}

const ADMIN = '/api/v1/admin/pickup-points'

/** Admin configuration of pickup points and their per-point delivery zones. */
export const adminPickupApi = {
  list: (page = 0) =>
    apiClient.get<ApiResponse<PageResponse<PickupPoint>>>(ADMIN, { params: { page } }),
  create: (payload: PickupPointPayload) =>
    apiClient.post<ApiResponse<PickupPoint>>(ADMIN, payload),
  update: (id: string, payload: PickupPointPayload) =>
    apiClient.put<ApiResponse<PickupPoint>>(`${ADMIN}/${id}`, payload),
  activate: (id: string) => apiClient.patch<ApiResponse<PickupPoint>>(`${ADMIN}/${id}/activate`),
  deactivate: (id: string) => apiClient.patch<ApiResponse<PickupPoint>>(`${ADMIN}/${id}/deactivate`),
  addZone: (pointId: string, payload: DeliveryZonePayload) =>
    apiClient.post<ApiResponse<DeliveryZone>>(`${ADMIN}/${pointId}/zones`, payload),
  updateZone: (zoneId: string, payload: DeliveryZonePayload) =>
    apiClient.put<ApiResponse<DeliveryZone>>(`${ADMIN}/zones/${zoneId}`, payload),
  deleteZone: (zoneId: string) => apiClient.delete<ApiResponse<void>>(`${ADMIN}/zones/${zoneId}`),
}

/** Public storefront read: active pickup points and their zones, for checkout. */
export const storePickupApi = {
  list: (slug: string) =>
    apiClient.get<ApiResponse<PickupPoint[]>>(`/api/v1/store/${encodeURIComponent(slug)}/pickup-points`),
}
