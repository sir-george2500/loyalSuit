import apiClient from './client'
import type { ApiResponse, DeliveryTracking, OrderResponse } from '@/types'

export const orderTrackingApi = {
  /** Public guest lookup: requires the order number and the email used at checkout. */
  track: (slug: string, orderNumber: string, email: string) =>
    apiClient.get<ApiResponse<OrderResponse>>(
      `/api/v1/store/${encodeURIComponent(slug)}/orders/${encodeURIComponent(orderNumber)}`,
      { params: { email } }
    ),
}

export const deliveryTrackingApi = {
  /** Public delivery progress for a verified order (same order-number + email gate). */
  track: (slug: string, orderNumber: string, email: string) =>
    apiClient.get<ApiResponse<DeliveryTracking>>(
      `/api/v1/store/${encodeURIComponent(slug)}/orders/${encodeURIComponent(orderNumber)}/delivery`,
      { params: { email } }
    ),
}
