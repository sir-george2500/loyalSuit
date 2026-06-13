'use client'

import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { AlertCircle, ChevronLeft, ChevronRight, Inbox } from 'lucide-react'
import { sellerOrderApi } from '@/lib/api/sellerOrders'
import type { OrderStatus, VendorOrder } from '@/types'

const STATUS_BADGE: Record<OrderStatus, string> = {
  PENDING: 'badge-warning',
  CONFIRMED: 'badge-info',
  PROCESSING: 'badge-info',
  SHIPPED: 'badge-primary',
  DELIVERED: 'badge-success',
  CANCELLED: 'badge-ghost',
  REFUNDED: 'badge-error',
}

function money(amount: number, currency: string) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: currency || 'RWF' }).format(amount)
}

function OrderCard({ order }: { order: VendorOrder }) {
  return (
    <div className="card border border-base-300 bg-base-100">
      <div className="card-body gap-3 p-5">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div>
            <p className="font-mono text-sm font-semibold">{order.orderNumber}</p>
            <p className="text-xs text-base-content/50">
              {new Date(order.createdAt).toLocaleDateString()} · {order.customerName ?? 'Guest'}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <span className={`badge badge-sm ${STATUS_BADGE[order.status] ?? 'badge-ghost'}`}>
              {order.status.toLowerCase()}
            </span>
            <span className="badge badge-ghost badge-sm">{order.paymentStatus.toLowerCase()}</span>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="table table-sm">
            <thead>
              <tr>
                <th>Your items</th>
                <th className="text-center">Qty</th>
                <th className="text-right">Total</th>
              </tr>
            </thead>
            <tbody>
              {order.items.map((it) => (
                <tr key={it.productId + (it.variantId ?? '')}>
                  <td>{it.productName}</td>
                  <td className="text-center">{it.quantity}</td>
                  <td className="text-right">{money(it.total, order.currency)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="flex justify-end border-t border-base-200 pt-2 text-sm">
          <span className="text-base-content/60">Your earnings on this order:&nbsp;</span>
          <span className="font-semibold">{money(order.vendorSubtotal, order.currency)}</span>
        </div>
      </div>
    </div>
  )
}

export default function SellerOrdersView() {
  const [page, setPage] = useState(0)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['seller-orders', page],
    queryFn: async () => (await sellerOrderApi.list(page)).data.data,
    placeholderData: keepPreviousData,
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load your orders. Try again.</p>
        </div>
      </div>
    )
  }

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="h-28 w-full animate-pulse rounded-box bg-base-200" />
        ))}
      </div>
    )
  }

  if (!data || data.content.length === 0) {
    return (
      <div className="flex flex-col items-center gap-2 rounded-box border border-base-300 bg-base-100 py-16 text-center text-base-content/50">
        <Inbox className="h-8 w-8" />
        <p className="text-sm">No orders yet. When a customer buys one of your products, it shows up here.</p>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="space-y-3">
        {data.content.map((o) => (
          <OrderCard key={o.orderId} order={o} />
        ))}
      </div>

      <div className="flex items-center justify-between text-sm text-base-content/60">
        <span>
          Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} order
          {data.totalElements === 1 ? '' : 's'}
        </span>
        <div className="join">
          <button className="btn btn-sm join-item" onClick={() => setPage((p) => Math.max(p - 1, 0))} disabled={data.first}>
            <ChevronLeft className="h-4 w-4" /> Prev
          </button>
          <button className="btn btn-sm join-item" onClick={() => setPage((p) => p + 1)} disabled={data.last}>
            Next <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  )
}
