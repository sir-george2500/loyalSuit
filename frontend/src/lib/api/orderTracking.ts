import apiClient from './client'
import type { ApiResponse, OrderResponse } from '@/types'

export const orderTrackingApi = {
  /** Public guest lookup: requires the order number and the email used at checkout. */
  track: (slug: string, orderNumber: string, email: string) =>
    apiClient.get<ApiResponse<OrderResponse>>(
      `/api/v1/store/${encodeURIComponent(slug)}/orders/${encodeURIComponent(orderNumber)}`,
      { params: { email } }
    ),
}
