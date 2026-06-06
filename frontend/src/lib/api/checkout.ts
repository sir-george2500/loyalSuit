import apiClient from './client'
import { getCartToken } from '@/lib/cart/token'
import type { ApiResponse, OrderResponse } from '@/types'

export interface CheckoutPayload {
  customerName: string
  customerEmail?: string
  customerPhone?: string
  addressLine1: string
  addressLine2?: string
  city: string
  state?: string
  postalCode?: string
  country?: string
  notes?: string
}

export const checkoutApi = {
  /**
   * Places a cash order. The same idempotencyKey must be reused across retries of
   * one checkout attempt so a double-submit can't create two orders.
   */
  placeOrder: (slug: string, payload: CheckoutPayload, idempotencyKey: string) =>
    apiClient.post<ApiResponse<OrderResponse>>(
      `/api/v1/store/${encodeURIComponent(slug)}/checkout`,
      payload,
      { headers: { 'X-Cart-Token': getCartToken(), 'Idempotency-Key': idempotencyKey } }
    ),
}
