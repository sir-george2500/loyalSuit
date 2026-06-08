import apiClient from './client'
import type { ApiResponse, PageResponse, PosProduct, PosSale } from '@/types'

const BASE = '/api/v1/pos'

export interface SaleLineInput {
  productId: string
  variantId?: string
  quantity: number
}

export interface CompleteSaleInput {
  /** Terminal-generated UUID; makes completing the sale idempotent on retry. */
  clientSaleId: string
  items: SaleLineInput[]
  amountTendered: number
}

/** In-store POS terminal: catalog lookup, ringing up a cash sale, recent sales. */
export const posApi = {
  products: (q = '', page = 0) =>
    apiClient.get<ApiResponse<PageResponse<PosProduct>>>(`${BASE}/products`, { params: { q, page } }),
  completeSale: (payload: CompleteSaleInput) =>
    apiClient.post<ApiResponse<PosSale>>(`${BASE}/sales`, payload),
  sales: (page = 0) =>
    apiClient.get<ApiResponse<PageResponse<PosSale>>>(`${BASE}/sales`, { params: { page } }),
}
