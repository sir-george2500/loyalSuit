import apiClient from './client'
import type { ApiResponse, SetStockRequest, Stock, Warehouse, WarehouseRequest } from '@/types'

const WAREHOUSES = '/api/v1/inventory/warehouses'
const STOCK = '/api/v1/inventory/stock'

export const warehouseApi = {
  list: () => apiClient.get<ApiResponse<Warehouse[]>>(WAREHOUSES),
  create: (payload: WarehouseRequest) =>
    apiClient.post<ApiResponse<Warehouse>>(WAREHOUSES, payload),
  update: (id: string, payload: WarehouseRequest) =>
    apiClient.put<ApiResponse<Warehouse>>(`${WAREHOUSES}/${id}`, payload),
  remove: (id: string) => apiClient.delete<ApiResponse<null>>(`${WAREHOUSES}/${id}`),
}

export const stockApi = {
  listForProduct: (productId: string) =>
    apiClient.get<ApiResponse<Stock[]>>(STOCK, { params: { productId } }),
  low: () => apiClient.get<ApiResponse<Stock[]>>(`${STOCK}/low`),
  setLevel: (payload: SetStockRequest) =>
    apiClient.put<ApiResponse<Stock>>(STOCK, payload),
  adjust: (stockId: string, delta: number) =>
    apiClient.post<ApiResponse<Stock>>(`${STOCK}/${stockId}/adjust`, { delta }),
}
