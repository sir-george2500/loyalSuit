import apiClient from './client'
import type {
  ApiResponse, AuthResponse, TwoFactorEnableResult, TwoFactorSetup, UserProfile,
} from '@/types'

export interface RegisterPayload {
  fullName: string
  email: string
  password: string
  businessName?: string
}

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
}

export const authApi = {
  login: (email: string, password: string) =>
    apiClient.post<ApiResponse<AuthResponse>>('/api/v1/auth/login', { email, password }),

  completeMfa: (mfaToken: string, code: string) =>
    apiClient.post<ApiResponse<AuthResponse>>('/api/v1/auth/login/2fa', { mfaToken, code }),

  twoFactorSetup: () =>
    apiClient.post<ApiResponse<TwoFactorSetup>>('/api/v1/auth/2fa/setup', {}),

  twoFactorEnable: (code: string) =>
    apiClient.post<ApiResponse<TwoFactorEnableResult>>('/api/v1/auth/2fa/enable', { code }),

  twoFactorDisable: (password: string) =>
    apiClient.post<ApiResponse<null>>('/api/v1/auth/2fa/disable', { password }),

  register: (payload: RegisterPayload) =>
    apiClient.post<ApiResponse<AuthResponse>>('/api/v1/auth/register', payload),

  me: () => apiClient.get<ApiResponse<UserProfile>>('/api/v1/auth/me'),

  changePassword: (payload: ChangePasswordPayload) =>
    apiClient.post<ApiResponse<null>>('/api/v1/auth/change-password', payload),

  forgotPassword: (email: string) =>
    apiClient.post<ApiResponse<null>>('/api/v1/auth/forgot-password', { email }),

  resetPassword: (token: string, newPassword: string) =>
    apiClient.post<ApiResponse<null>>('/api/v1/auth/reset-password', { token, newPassword }),
}
