import apiClient from './client'
import { getCartToken } from '@/lib/cart/token'
import type { ApiResponse, CartView } from '@/types'

function headers() {
  return { headers: { 'X-Cart-Token': getCartToken() } }
}

function base(slug: string) {
  return `/api/v1/store/${encodeURIComponent(slug)}/cart`
}

export interface AddItemPayload {
  productId: string
  variantId?: string
  quantity: number
}

export const cartApi = {
  view: (slug: string) => apiClient.get<ApiResponse<CartView>>(base(slug), headers()),
  addItem: (slug: string, payload: AddItemPayload) =>
    apiClient.post<ApiResponse<CartView>>(`${base(slug)}/items`, payload, headers()),
  updateItem: (slug: string, payload: { productId: string; variantId?: string; quantity: number }) =>
    apiClient.put<ApiResponse<CartView>>(`${base(slug)}/items`, payload, headers()),
  removeItem: (slug: string, productId: string, variantId?: string | null) =>
    apiClient.delete<ApiResponse<CartView>>(`${base(slug)}/items`, {
      ...headers(),
      params: { productId, ...(variantId ? { variantId } : {}) },
    }),
}
