'use client'

import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import {
  ShoppingCart, AlertCircle, Loader2, ChevronLeft, ChevronRight, X, Banknote,
} from 'lucide-react'
import { ordersApi, nextStatuses } from '@/lib/api/orders'
import type { OrderStatus, PaymentStatus } from '@/types'

const ALL_STATUSES: OrderStatus[] = [
  'PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED',
]

const STATUS_BADGE: Record<OrderStatus, string> = {
  PENDING: 'badge-ghost',
  CONFIRMED: 'badge-info',
  PROCESSING: 'badge-warning',
  SHIPPED: 'badge-accent',
  DELIVERED: 'badge-success',
  CANCELLED: 'badge-error',
  REFUNDED: 'badge-neutral',
}

const PAYMENT_BADGE: Record<PaymentStatus, string> = {
  UNPAID: 'badge-warning',
  PAID: 'badge-success',
  REFUNDED: 'badge-neutral',
}

const dateFmt = new Intl.DateTimeFormat('en-US', {
  month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
})

function titleCase(s: string): string {
  return s.charAt(0) + s.slice(1).toLowerCase()
}

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function OrdersView() {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<OrderStatus | ''>('')
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['orders', page, status],
    queryFn: async () => (await ordersApi.list({ status, page })).data.data,
    placeholderData: keepPreviousData,
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load orders. Try again.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <label className="text-sm text-base-content/60" htmlFor="status-filter">Status</label>
        <select
          id="status-filter"
          value={status}
          onChange={(e) => { setStatus(e.target.value as OrderStatus | ''); setPage(0) }}
          className="select select-bordered select-sm"
        >
          <option value="">All</option>
          {ALL_STATUSES.map((s) => <option key={s} value={s}>{titleCase(s)}</option>)}
        </select>
        {isFetching && <span className="loading loading-spinner loading-xs text-primary" />}
      </div>

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr>
              <th>Order</th>
              <th>Customer</th>
              <th className="text-right">Total</th>
              <th>Status</th>
              <th>Payment</th>
              <th>Placed</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 6 }).map((_, i) => (
                <tr key={i}><td colSpan={7}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((o) => (
                <tr key={o.id}>
                  <td className="font-mono text-xs">{o.orderNumber}</td>
                  <td>{o.customerName}</td>
                  <td className="text-right">
                    {new Intl.NumberFormat('en-US', { style: 'currency', currency: o.currency }).format(o.total)}
                  </td>
                  <td><span className={`badge badge-sm ${STATUS_BADGE[o.status]}`}>{titleCase(o.status)}</span></td>
                  <td><span className={`badge badge-sm ${PAYMENT_BADGE[o.paymentStatus]}`}>{titleCase(o.paymentStatus)}</span></td>
                  <td className="whitespace-nowrap text-base-content/60">{dateFmt.format(new Date(o.createdAt))}</td>
                  <td className="text-right">
                    <button className="btn btn-ghost btn-xs" onClick={() => setSelectedId(o.id)}>View</button>
                  </td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={7}>
                <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                  <ShoppingCart className="h-8 w-8" /><p className="text-sm">No orders yet.</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-base-content/60">
          <span>Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} order{data.totalElements === 1 ? '' : 's'}</span>
          <div className="join">
            <button className="btn btn-sm join-item" onClick={() => setPage((p) => Math.max(p - 1, 0))} disabled={data.first}>
              <ChevronLeft className="h-4 w-4" /> Prev
            </button>
            <button className="btn btn-sm join-item" onClick={() => setPage((p) => p + 1)} disabled={data.last}>
              Next <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}

      {selectedId && <OrderDetailModal orderId={selectedId} onClose={() => setSelectedId(null)} />}
    </div>
  )
}

function OrderDetailModal({ orderId, onClose }: { orderId: string; onClose: () => void }) {
  const queryClient = useQueryClient()
  const [error, setError] = useState<string | null>(null)

  const { data: order, isLoading } = useQuery({
    queryKey: ['order', orderId],
    queryFn: async () => (await ordersApi.get(orderId)).data.data,
  })

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['order', orderId] })
    queryClient.invalidateQueries({ queryKey: ['orders'] })
  }

  const transition = useMutation({
    mutationFn: (status: OrderStatus) => ordersApi.transition(orderId, status),
    onSuccess: () => { invalidate(); setError(null) },
    onError: (err) => setError(errorMessage(err, 'Could not update the order.')),
  })

  const markPaid = useMutation({
    mutationFn: () => ordersApi.markPaid(orderId),
    onSuccess: () => { invalidate(); setError(null) },
    onError: (err) => setError(errorMessage(err, 'Could not record payment.')),
  })

  const busy = transition.isPending || markPaid.isPending
  const money = order
    ? new Intl.NumberFormat('en-US', { style: 'currency', currency: order.currency })
    : null

  return (
    <dialog className="modal modal-open">
      <div className="modal-box max-w-2xl">
        <div className="mb-3 flex items-start justify-between">
          <h3 className="text-lg font-bold">{order ? order.orderNumber : 'Order'}</h3>
          <button className="btn btn-ghost btn-sm btn-circle" onClick={onClose} aria-label="Close"><X className="h-4 w-4" /></button>
        </div>

        {error && <div role="alert" className="alert alert-error mb-3 text-sm"><span>{error}</span></div>}

        {isLoading || !order || !money ? (
          <div className="flex justify-center py-10"><Loader2 className="h-6 w-6 animate-spin text-primary" /></div>
        ) : (
          <div className="space-y-4">
            <div className="flex flex-wrap gap-2">
              <span className={`badge ${STATUS_BADGE[order.status]}`}>{titleCase(order.status)}</span>
              <span className={`badge ${PAYMENT_BADGE[order.paymentStatus]}`}>{titleCase(order.paymentStatus)}</span>
              <span className="badge badge-ghost">{order.paymentMethod}</span>
            </div>

            <div className="rounded-box border border-base-300 p-3 text-sm">
              <p className="font-medium">{order.customerName}</p>
              {order.customerEmail && <p className="text-base-content/60">{order.customerEmail}</p>}
              {order.customerPhone && <p className="text-base-content/60">{order.customerPhone}</p>}
              {order.shippingAddress && (
                <p className="mt-1 text-base-content/60">
                  {Object.values(order.shippingAddress).join(', ')}
                </p>
              )}
            </div>

            <div className="overflow-x-auto rounded-box border border-base-300">
              <table className="table table-sm">
                <thead><tr><th>Item</th><th className="text-center">Qty</th><th className="text-right">Unit</th><th className="text-right">Total</th></tr></thead>
                <tbody>
                  {order.items.map((it, i) => (
                    <tr key={i}>
                      <td className="font-mono text-xs">{it.productId.slice(0, 8)}…</td>
                      <td className="text-center">{it.quantity}</td>
                      <td className="text-right">{money.format(it.unitPrice)}</td>
                      <td className="text-right">{money.format(it.total)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex justify-between border-t border-base-300 pt-2 font-semibold">
              <span>Total</span><span>{money.format(order.total)}</span>
            </div>

            {/* Actions */}
            <div className="flex flex-wrap items-center gap-2 border-t border-base-300 pt-3">
              {order.paymentStatus === 'UNPAID' && (
                <button className="btn btn-success btn-sm gap-1" disabled={busy} onClick={() => markPaid.mutate()}>
                  <Banknote className="h-4 w-4" /> Mark cash received
                </button>
              )}
              {nextStatuses(order.status).map((s) => (
                <button
                  key={s}
                  className={`btn btn-sm ${s === 'CANCELLED' || s === 'REFUNDED' ? 'btn-outline btn-error' : 'btn-primary'}`}
                  disabled={busy}
                  onClick={() => transition.mutate(s)}
                >
                  {busy && <Loader2 className="h-4 w-4 animate-spin" />}
                  {titleCase(s)}
                </button>
              ))}
              {nextStatuses(order.status).length === 0 && order.paymentStatus !== 'UNPAID' && (
                <span className="text-sm text-base-content/50">No further actions.</span>
              )}
            </div>
          </div>
        )}

        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>Done</button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}
