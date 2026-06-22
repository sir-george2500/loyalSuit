'use client'

import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { CreditCard, AlertCircle, ChevronLeft, ChevronRight } from 'lucide-react'
import { paymentApi } from '@/lib/api/billing'
import type { PaymentStatus } from '@/types'

const STATUS_BADGE: Record<PaymentStatus, string> = {
  UNPAID: 'badge-warning',
  PAID: 'badge-success',
  REFUNDED: 'badge-neutral',
}

function titleCase(s: string): string {
  return s.charAt(0) + s.slice(1).toLowerCase()
}

function money(amount: number, currency: string): string {
  return new Intl.NumberFormat('en-RW', { style: 'currency', currency, maximumFractionDigits: 0 }).format(amount)
}

function dateTime(iso: string): string {
  return new Date(iso).toLocaleString(undefined, { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

export default function PaymentsView() {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<PaymentStatus | ''>('')

  const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['payments', page, status],
    queryFn: async () => (await paymentApi.list({ status, page })).data.data,
    placeholderData: keepPreviousData,
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load payments. Try again.</p>
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
          onChange={(e) => { setStatus(e.target.value as PaymentStatus | ''); setPage(0) }}
          className="select select-bordered select-sm"
        >
          <option value="">All</option>
          <option value="PAID">Paid</option>
          <option value="UNPAID">Unpaid</option>
          <option value="REFUNDED">Refunded</option>
        </select>
        {isFetching && <span className="loading loading-spinner loading-xs text-primary" />}
      </div>

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr><th>Order</th><th>Customer</th><th>Method</th><th>Status</th><th className="text-right">Amount</th><th>Date</th></tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i}><td colSpan={6}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((p) => (
                <tr key={p.orderId}>
                  <td className="font-mono text-xs">{p.orderNumber}</td>
                  <td className="text-sm">{p.customerName ?? '—'}</td>
                  <td><span className="badge badge-ghost badge-sm">{titleCase(p.method)}</span></td>
                  <td><span className={`badge badge-sm ${STATUS_BADGE[p.status]}`}>{titleCase(p.status)}</span></td>
                  <td className="text-right font-medium">{money(p.amount, p.currency)}</td>
                  <td className="text-sm text-base-content/60">{dateTime(p.date)}</td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={6}>
                <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                  <CreditCard className="h-8 w-8" /><p className="text-sm">No payments yet.</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-base-content/60">
          <span>Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} payment{data.totalElements === 1 ? '' : 's'}</span>
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
    </div>
  )
}
