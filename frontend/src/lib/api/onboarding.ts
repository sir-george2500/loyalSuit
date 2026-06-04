import apiClient from './client'
import type { ApiResponse, CompleteOnboardingRequest, OnboardingStatus } from '@/types'

export const onboardingApi = {
  getStatus: () =>
    apiClient.get<ApiResponse<OnboardingStatus>>('/api/v1/onboarding/status'),
  complete: (payload: CompleteOnboardingRequest) =>
    apiClient.post<ApiResponse<OnboardingStatus>>('/api/v1/onboarding/complete', payload),
}
