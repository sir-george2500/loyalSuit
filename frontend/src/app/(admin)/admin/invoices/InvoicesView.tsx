'use client'

import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { FileText, AlertCircle, ChevronLeft, ChevronRight, Eye, Loader2 } from 'lucide-react'
import { invoiceApi } from '@/lib/api/billing'
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

function date(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

export default function InvoicesView() {
  const [page, setPage] = useState(0)
  const [openId, setOpenId] = useState<string | null>(null)

  const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['invoices', page],
    queryFn: async () => (await invoiceApi.list({ page })).data.data,
    placeholderData: keepPreviousData,
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load invoices. Try again.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {isFetching && <span className="loading loading-spinner loading-xs text-primary" />}

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr><th>Invoice</th><th>Customer</th><th>Status</th><th className="text-right">Total</th><th>Issued</th><th /></tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i}><td colSpan={6}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((inv) => (
                <tr key={inv.orderId}>
                  <td>
                    <div className="font-mono text-xs font-medium">{inv.invoiceNumber}</div>
                    <div className="font-mono text-xs text-base-content/50">{inv.orderNumber}</div>
                  </td>
                  <td className="text-sm">{inv.customerName ?? '—'}</td>
                  <td><span className={`badge badge-sm ${STATUS_BADGE[inv.status]}`}>{titleCase(inv.status)}</span></td>
                  <td className="text-right font-medium">{money(inv.total, inv.currency)}</td>
                  <td className="text-sm text-base-content/60">{date(inv.issuedAt)}</td>
                  <td className="text-right">
                    <button className="btn btn-ghost btn-xs gap-1" onClick={() => setOpenId(inv.orderId)}>
                      <Eye className="h-3.5 w-3.5" /> View
                    </button>
                  </td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={6}>
                <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                  <FileText className="h-8 w-8" /><p className="text-sm">No invoices yet.</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-base-content/60">
          <span>Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} invoice{data.totalElements === 1 ? '' : 's'}</span>
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

      {openId && <InvoiceModal orderId={openId} onClose={() => setOpenId(null)} />}
    </div>
  )
}

function InvoiceModal({ orderId, onClose }: { orderId: string; onClose: () => void }) {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['invoice', orderId],
    queryFn: async () => (await invoiceApi.get(orderId)).data.data,
  })

  return (
    <dialog className="modal modal-open">
      <div className="modal-box max-w-2xl">
        {isLoading ? (
          <div className="flex items-center justify-center py-12"><Loader2 className="h-6 w-6 animate-spin text-primary" /></div>
        ) : isError || !data ? (
          <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/60">
            <AlertCircle className="h-6 w-6 text-error" /><p className="text-sm">Couldn’t load this invoice.</p>
          </div>
        ) : (
          <>
            <div className="flex items-start justify-between">
              <div>
                <h3 className="text-lg font-bold">{data.invoiceNumber}</h3>
                <p className="text-xs text-base-content/50">Order {data.orderNumber} · {date(data.issuedAt)}</p>
              </div>
              <span className={`badge badge-sm ${STATUS_BADGE[data.status]}`}>{titleCase(data.status)}</span>
            </div>

            <div className="mt-3 text-sm">
              <div className="font-medium">{data.customerName ?? 'Walk-in customer'}</div>
              {data.customerEmail && <div className="text-base-content/60">{data.customerEmail}</div>}
              {data.customerPhone && <div className="text-base-content/60">{data.customerPhone}</div>}
            </div>

            <div className="mt-4 overflow-x-auto rounded-box border border-base-300">
              <table className="table table-sm">
                <thead>
                  <tr><th>Item</th><th className="text-center">Qty</th><th className="text-right">Unit</th><th className="text-right">Total</th></tr>
                </thead>
                <tbody>
                  {data.items.map((line, i) => (
                    <tr key={i}>
                      <td>{line.description}</td>
                      <td className="text-center">{line.quantity}</td>
                      <td className="text-right">{money(line.unitPrice, data.currency)}</td>
                      <td className="text-right">{money(line.total, data.currency)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="mt-3 ml-auto w-full max-w-xs space-y-1 text-sm">
              <Row label="Subtotal" value={money(data.subtotal, data.currency)} />
              {data.discountAmount > 0 && <Row label="Discount" value={`− ${money(data.discountAmount, data.currency)}`} />}
              {data.shippingAmount > 0 && <Row label="Shipping" value={money(data.shippingAmount, data.currency)} />}
              {data.taxAmount > 0 && <Row label="Tax" value={money(data.taxAmount, data.currency)} />}
              <div className="divider my-1" />
              <div className="flex justify-between font-bold">
                <span>Total</span><span>{money(data.total, data.currency)}</span>
              </div>
            </div>
          </>
        )}

        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>Close</button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between text-base-content/70">
      <span>{label}</span><span>{value}</span>
    </div>
  )
}
