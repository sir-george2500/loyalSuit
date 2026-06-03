import apiClient from './client'
import type { ApiResponse, AuthResponse, UserProfile } from '@/types'

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

  register: (payload: RegisterPayload) =>
    apiClient.post<ApiResponse<AuthResponse>>('/api/v1/auth/register', payload),

  me: () => apiClient.get<ApiResponse<UserProfile>>('/api/v1/auth/me'),

  changePassword: (payload: ChangePasswordPayload) =>
    apiClient.post<ApiResponse<null>>('/api/v1/auth/change-password', payload),
}
