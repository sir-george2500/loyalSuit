import apiClient from './client'
import type { ApiResponse, ProductReview, ProductReviews, ReviewEligibility, VendorReviews } from '@/types'

export interface CreateReviewPayload {
  productId: string
  rating: number
  comment?: string
}

export const reviewApi = {
  /** Public: a product's reviews + aggregate rating. */
  forProduct: (productId: string, page = 0) =>
    apiClient.get<ApiResponse<ProductReviews>>(`/api/v1/store/products/${productId}/reviews`, { params: { page } }),

  /** Customer: write a review (server enforces a delivered purchase). */
  create: (payload: CreateReviewPayload) =>
    apiClient.post<ApiResponse<ProductReview>>('/api/v1/reviews', payload),

  /** Customer: may I review this product? */
  eligibility: (productId: string) =>
    apiClient.get<ApiResponse<ReviewEligibility>>('/api/v1/reviews/eligibility', { params: { productId } }),

  /** Seller: reviews on my products + my aggregate rating. */
  forVendor: (page = 0) =>
    apiClient.get<ApiResponse<VendorReviews>>('/api/v1/vendor/reviews', { params: { page } }),
}
