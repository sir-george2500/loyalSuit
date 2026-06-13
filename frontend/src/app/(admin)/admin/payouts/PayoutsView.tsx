'use client'

import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { Wallet, AlertCircle, Loader2, ChevronLeft, ChevronRight, Check, X } from 'lucide-react'
import { adminPayoutApi } from '@/lib/api/payouts'
import type { Payout, PayoutStatus } from '@/types'

const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'RWF' })

const STATUS_BADGE: Record<PayoutStatus, string> = {
  PENDING: 'badge-warning',
  PAID: 'badge-success',
  REJECTED: 'badge-neutral',
}

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function PayoutsView() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<PayoutStatus | ''>('')
  const [deciding, setDeciding] = useState<{ payout: Payout; mode: 'pay' | 'reject' } | null>(null)
  const [error, setError] = useState<string | null>(null)

  const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['admin-payouts', page, status],
    queryFn: async () => (await adminPayoutApi.list({ status, page })).data.data,
    placeholderData: keepPreviousData,
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load payouts. Try again.</p>
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
          onChange={(e) => { setStatus(e.target.value as PayoutStatus | ''); setPage(0) }}
          className="select select-bordered select-sm"
        >
          <option value="">All</option>
          <option value="PENDING">Pending</option>
          <option value="PAID">Paid</option>
          <option value="REJECTED">Rejected</option>
        </select>
      </div>

      {error && <div role="alert" className="alert alert-error text-sm"><span>{error}</span></div>}

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr>
              <th>Requested</th><th>Vendor</th><th className="text-right">Amount</th>
              <th>Status</th><th>Reference / note</th><th className="text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i}><td colSpan={6}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((p) => (
                <tr key={p.id}>
                  <td className="text-base-content/70">{new Date(p.createdAt).toLocaleDateString()}</td>
                  <td className="font-mono text-xs text-base-content/50">{p.vendorId.slice(0, 8)}</td>
                  <td className="text-right font-medium">{money.format(p.amount)}</td>
                  <td><span className={`badge badge-sm ${STATUS_BADGE[p.status]}`}>{p.status}</span></td>
                  <td className="text-base-content/60">{p.reference || p.resolutionNote || '—'}</td>
                  <td>
                    <div className="flex items-center justify-end gap-1">
                      {p.status === 'PENDING' ? (
                        <>
                          <button className="btn btn-ghost btn-xs text-success" onClick={() => { setError(null); setDeciding({ payout: p, mode: 'pay' }) }}>
                            <Check className="h-3.5 w-3.5" /> Pay
                          </button>
                          <button className="btn btn-ghost btn-xs text-error" onClick={() => { setError(null); setDeciding({ payout: p, mode: 'reject' }) }}>
                            <X className="h-3.5 w-3.5" /> Reject
                          </button>
                        </>
                      ) : (
                        <span className="text-xs text-base-content/40">decided</span>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={6}>
                <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                  <Wallet className="h-8 w-8" /><p className="text-sm">No payout requests.</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-base-content/60">
          <span>
            Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} request{data.totalElements === 1 ? '' : 's'}
            {isFetching && <span className="loading loading-spinner loading-xs ml-2 align-middle text-primary" />}
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
      )}

      {deciding && (
        <DecidePayoutModal
          payout={deciding.payout}
          mode={deciding.mode}
          onClose={() => setDeciding(null)}
          onDone={() => { queryClient.invalidateQueries({ queryKey: ['admin-payouts'] }); setDeciding(null) }}
          onError={(msg) => { setError(msg); setDeciding(null) }}
        />
      )}
    </div>
  )
}

function DecidePayoutModal({ payout, mode, onClose, onDone, onError }: {
  payout: Payout
  mode: 'pay' | 'reject'
  onClose: () => void
  onDone: () => void
  onError: (msg: string) => void
}) {
  const [reference, setReference] = useState('')
  const [note, setNote] = useState('')
  const isPay = mode === 'pay'

  const submit = useMutation({
    mutationFn: () =>
      isPay
        ? adminPayoutApi.pay(payout.id, { reference: reference || undefined, note: note || undefined })
        : adminPayoutApi.reject(payout.id, note),
    onSuccess: onDone,
    onError: (err) => onError(errorMessage(err, 'Could not update the payout.')),
  })

  return (
    <dialog className="modal modal-open">
      <div className="modal-box max-w-sm">
        <h3 className="mb-1 text-lg font-bold">{isPay ? 'Mark payout as paid' : 'Reject payout'}</h3>
        <p className="mb-3 text-sm text-base-content/60">Amount: {money.format(payout.amount)}</p>

        {isPay && (
          <label className="form-control mb-2">
            <span className="label-text font-medium">Cash reference (optional)</span>
            <input value={reference} onChange={(e) => setReference(e.target.value)}
              className="input input-bordered w-full" placeholder="e.g. CASH-001" />
          </label>
        )}
        <label className="form-control">
          <span className="label-text font-medium">{isPay ? 'Note (optional)' : 'Reason'}</span>
          <textarea value={note} onChange={(e) => setNote(e.target.value)}
            className="textarea textarea-bordered w-full" rows={2}
            placeholder={isPay ? 'Anything to record' : 'Why is this rejected?'} />
        </label>

        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button
            className={`btn ${isPay ? 'btn-success' : 'btn-error'}`}
            disabled={submit.isPending || (!isPay && !note.trim())}
            onClick={() => submit.mutate()}
          >
            {submit.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
            {isPay ? 'Mark paid' : 'Reject'}
          </button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}
