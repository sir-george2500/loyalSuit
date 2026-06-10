import apiClient from './client'
import type { ApiResponse, LoyaltyBalance } from '@/types'

/** A signed-in customer's loyalty points. */
export const loyaltyApi = {
  me: () => apiClient.get<ApiResponse<LoyaltyBalance>>('/api/v1/loyalty/me'),
}
