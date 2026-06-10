import apiClient from './client'
import { getCartToken } from '@/lib/cart/token'
import type { ApiResponse, Coupon, CouponPreview, DiscountType, PageResponse } from '@/types'

export interface CouponPayload {
  code: string
  description?: string
  discountType: DiscountType
  value: number
  minOrderSubtotal?: number | null
  maxDiscount?: number | null
  usageLimit?: number | null
  perCustomerLimit?: number | null
  startsAt?: string | null
  endsAt?: string | null
  active?: boolean
}

const ADMIN = '/api/v1/admin/coupons'

/** Admin coupon management. */
export const adminCouponApi = {
  list: (page = 0) => apiClient.get<ApiResponse<PageResponse<Coupon>>>(ADMIN, { params: { page } }),
  create: (payload: CouponPayload) => apiClient.post<ApiResponse<Coupon>>(ADMIN, payload),
  update: (id: string, payload: CouponPayload) => apiClient.put<ApiResponse<Coupon>>(`${ADMIN}/${id}`, payload),
  activate: (id: string) => apiClient.patch<ApiResponse<Coupon>>(`${ADMIN}/${id}/activate`),
  deactivate: (id: string) => apiClient.patch<ApiResponse<Coupon>>(`${ADMIN}/${id}/deactivate`),
}

/** Public storefront preview of a coupon against the current cart. */
export const storeCouponApi = {
  preview: (slug: string, code: string) =>
    apiClient.get<ApiResponse<CouponPreview>>(
      `/api/v1/store/${encodeURIComponent(slug)}/coupons/${encodeURIComponent(code)}`,
      { headers: { 'X-Cart-Token': getCartToken() } },
    ),
}
