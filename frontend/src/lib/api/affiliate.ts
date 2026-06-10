import apiClient from './client'
import type { ApiResponse } from '@/types'

export interface AffiliateMe {
  id: string
  code: string
  rewardRate: number
  active: boolean
  earned: number
  reversed: number
}

/** A signed-in affiliate's own code + earnings. */
export const affiliateApi = {
  me: () => apiClient.get<ApiResponse<AffiliateMe>>('/api/v1/affiliate/me'),
}
