import apiClient from './client'
import type {
  ApiResponse,
  Category,
  CreateCategoryRequest,
  PageResponse,
  Product,
  ProductWriteRequest,
} from '@/types'

const CATEGORIES = '/api/v1/catalog/categories'
const PRODUCTS = '/api/v1/catalog/products'

export const categoryApi = {
  list: () => apiClient.get<ApiResponse<Category[]>>(CATEGORIES),
  create: (payload: CreateCategoryRequest) =>
    apiClient.post<ApiResponse<Category>>(CATEGORIES, payload),
  update: (id: string, payload: CreateCategoryRequest) =>
    apiClient.put<ApiResponse<Category>>(`${CATEGORIES}/${id}`, payload),
  remove: (id: string) => apiClient.delete<ApiResponse<null>>(`${CATEGORIES}/${id}`),
}

export const productApi = {
  list: (page = 0, size = 20) =>
    apiClient.get<ApiResponse<PageResponse<Product>>>(PRODUCTS, { params: { page, size } }),
  create: (payload: ProductWriteRequest) =>
    apiClient.post<ApiResponse<Product>>(PRODUCTS, payload),
  update: (id: string, payload: ProductWriteRequest) =>
    apiClient.put<ApiResponse<Product>>(`${PRODUCTS}/${id}`, payload),
  remove: (id: string) => apiClient.delete<ApiResponse<null>>(`${PRODUCTS}/${id}`),
  publish: (id: string) => apiClient.patch<ApiResponse<Product>>(`${PRODUCTS}/${id}/publish`),
  unpublish: (id: string) => apiClient.patch<ApiResponse<Product>>(`${PRODUCTS}/${id}/unpublish`),
  archive: (id: string) => apiClient.patch<ApiResponse<Product>>(`${PRODUCTS}/${id}/archive`),
}
