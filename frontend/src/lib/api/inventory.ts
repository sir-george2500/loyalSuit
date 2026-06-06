import apiClient from './client'
import type { ApiResponse, Warehouse, WarehouseRequest } from '@/types'

const WAREHOUSES = '/api/v1/inventory/warehouses'

export const warehouseApi = {
  list: () => apiClient.get<ApiResponse<Warehouse[]>>(WAREHOUSES),
  create: (payload: WarehouseRequest) =>
    apiClient.post<ApiResponse<Warehouse>>(WAREHOUSES, payload),
  update: (id: string, payload: WarehouseRequest) =>
    apiClient.put<ApiResponse<Warehouse>>(`${WAREHOUSES}/${id}`, payload),
  remove: (id: string) => apiClient.delete<ApiResponse<null>>(`${WAREHOUSES}/${id}`),
}
