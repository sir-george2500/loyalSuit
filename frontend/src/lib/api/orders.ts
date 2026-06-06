import apiClient from './client'
import type { ApiResponse, OrderResponse, OrderStatus, OrderSummary, PageResponse } from '@/types'

const BASE = '/api/v1/orders'

export const ordersApi = {
  list: (params: { status?: OrderStatus | ''; page?: number } = {}) =>
    apiClient.get<ApiResponse<PageResponse<OrderSummary>>>(BASE, {
      params: {
        page: params.page ?? 0,
        ...(params.status ? { status: params.status } : {}),
      },
    }),
  get: (id: string) => apiClient.get<ApiResponse<OrderResponse>>(`${BASE}/${id}`),
  transition: (id: string, status: OrderStatus) =>
    apiClient.patch<ApiResponse<OrderResponse>>(`${BASE}/${id}/status`, { status }),
  markPaid: (id: string) => apiClient.patch<ApiResponse<OrderResponse>>(`${BASE}/${id}/mark-paid`),
}

/** Mirror of the backend fulfilment state machine — drives which actions to show. */
export function nextStatuses(status: OrderStatus): OrderStatus[] {
  switch (status) {
    case 'PENDING':
      return ['CONFIRMED', 'CANCELLED']
    case 'CONFIRMED':
      return ['PROCESSING', 'CANCELLED']
    case 'PROCESSING':
      return ['SHIPPED', 'CANCELLED']
    case 'SHIPPED':
      return ['DELIVERED']
    case 'DELIVERED':
      return ['REFUNDED']
    default:
      return []
  }
}
