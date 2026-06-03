import apiClient from './client'
import type { PageResponse, Product, CreateProductRequest } from '@/types'

export const productApi = {
  list: (params?: { page?: number; size?: number }) =>
    apiClient.get<{ data: PageResponse<Product> }>('/api/v1/catalog/products', { params }),

  get: (id: string) =>
    apiClient.get<{ data: Product }>(`/api/v1/catalog/products/${id}`),

  create: (data: CreateProductRequest) =>
    apiClient.post<{ data: Product }>('/api/v1/catalog/products', data),

  publish: (id: string) =>
    apiClient.patch<{ data: Product }>(`/api/v1/catalog/products/${id}/publish`),

  delete: (id: string) =>
    apiClient.delete(`/api/v1/catalog/products/${id}`),
}
