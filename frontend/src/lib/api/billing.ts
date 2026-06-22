import apiClient from './client'
import type { ApiResponse, Invoice, InvoiceSummary, PageResponse, Payment, PaymentStatus } from '@/types'

export const paymentApi = {
  list: (params: { status?: PaymentStatus | ''; page?: number } = {}) =>
    apiClient.get<ApiResponse<PageResponse<Payment>>>('/api/v1/admin/payments', {
      params: { page: params.page ?? 0, ...(params.status ? { status: params.status } : {}) },
    }),
}

export const invoiceApi = {
  list: (params: { page?: number } = {}) =>
    apiClient.get<ApiResponse<PageResponse<InvoiceSummary>>>('/api/v1/admin/invoices', {
      params: { page: params.page ?? 0 },
    }),
  get: (orderId: string) =>
    apiClient.get<ApiResponse<Invoice>>(`/api/v1/admin/invoices/${orderId}`),
}
