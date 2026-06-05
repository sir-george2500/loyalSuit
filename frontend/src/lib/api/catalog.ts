import apiClient from './client'
import type { ApiResponse, Category, CreateCategoryRequest } from '@/types'

const BASE = '/api/v1/catalog/categories'

export const categoryApi = {
  list: () => apiClient.get<ApiResponse<Category[]>>(BASE),
  create: (payload: CreateCategoryRequest) =>
    apiClient.post<ApiResponse<Category>>(BASE, payload),
  update: (id: string, payload: CreateCategoryRequest) =>
    apiClient.put<ApiResponse<Category>>(`${BASE}/${id}`, payload),
  remove: (id: string) => apiClient.delete<ApiResponse<null>>(`${BASE}/${id}`),
}
