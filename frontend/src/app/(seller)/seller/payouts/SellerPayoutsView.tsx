'use client'

import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { Wallet, Loader2, ChevronLeft, ChevronRight, Banknote, AlertCircle } from 'lucide-react'
import { vendorPayoutApi } from '@/lib/api/payouts'
import type { PayoutStatus } from '@/types'

const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'RWF' })

const STATUS_BADGE: Record<PayoutStatus, string> = {
  PENDING: 'badge-warning',
  PAID: 'badge-success',
  REJECTED: 'badge-neutral',
}

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function SellerPayoutsView() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [requesting, setRequesting] = useState(false)

  const { data: balance } = useQuery({
    queryKey: ['seller-payout-balance'],
    queryFn: async () => (await vendorPayoutApi.balance()).data.data,
  })

  const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['seller-payouts', page],
    queryFn: async () => (await vendorPayoutApi.list(page)).data.data,
    placeholderData: keepPreviousData,
  })

  const available = balance?.available ?? 0
  const owed = balance?.owed ?? 0

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load your payouts. Try again.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {owed > 0 && (
        <div role="alert" className="alert alert-warning text-sm">
          <AlertCircle className="h-4 w-4" />
          <span>
            A refund reversed commission on an already-paid order, so you currently owe{' '}
            <span className="font-semibold">{money.format(owed)}</span>. Your future earnings will clear this
            before your next withdrawal.
          </span>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-3">
        <BalanceCard label="Available" value={money.format(Math.max(available, 0))} tone="text-success" highlight />
        <BalanceCard
          label={owed > 0 ? 'Owed' : 'Pending'}
          value={money.format(owed > 0 ? owed : (balance?.pending ?? 0))}
          tone={owed > 0 ? 'text-error' : 'text-warning'}
        />
        <BalanceCard label="Paid out" value={money.format(balance?.paid ?? 0)} tone="text-base-content/70" />
      </div>

      <div className="flex justify-end">
        <button
          className="btn btn-primary btn-sm gap-2"
          onClick={() => setRequesting(true)}
          disabled={available <= 0}
        >
          <Banknote className="h-4 w-4" /> Request payout
        </button>
      </div>

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr><th>Requested</th><th className="text-right">Amount</th><th>Status</th><th>Reference / note</th></tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 4 }).map((_, i) => (
                <tr key={i}><td colSpan={4}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((p) => (
                <tr key={p.id}>
                  <td className="text-base-content/70">{new Date(p.createdAt).toLocaleDateString()}</td>
                  <td className="text-right font-medium">{money.format(p.amount)}</td>
                  <td><span className={`badge badge-sm ${STATUS_BADGE[p.status]}`}>{p.status}</span></td>
                  <td className="text-base-content/60">{p.reference || p.resolutionNote || '—'}</td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={4}>
                <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                  <Wallet className="h-8 w-8" /><p className="text-sm">No payout requests yet.</p>
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

      {requesting && (
        <RequestPayoutModal
          available={available}
          onClose={() => setRequesting(false)}
          onDone={() => {
            queryClient.invalidateQueries({ queryKey: ['seller-payouts'] })
            queryClient.invalidateQueries({ queryKey: ['seller-payout-balance'] })
            setRequesting(false)
          }}
        />
      )}
    </div>
  )
}

function BalanceCard({ label, value, tone, highlight = false }: {
  label: string; value: string; tone: string; highlight?: boolean
}) {
  return (
    <div className={`card shadow-sm ${highlight ? 'bg-success/5 border border-success/20' : 'bg-base-100'}`}>
      <div className="card-body p-5">
        <p className="text-sm text-base-content/60">{label}</p>
        <p className={`text-2xl font-bold ${tone}`}>{value}</p>
      </div>
    </div>
  )
}

function RequestPayoutModal({ available, onClose, onDone }: {
  available: number; onClose: () => void; onDone: () => void
}) {
  const [amount, setAmount] = useState('')
  const [error, setError] = useState<string | null>(null)

  const submit = useMutation({
    mutationFn: () => vendorPayoutApi.request(Number(amount)),
    onSuccess: onDone,
    onError: (err) => setError(errorMessage(err, 'Could not request the payout.')),
  })

  const numeric = Number(amount)
  const invalid = !amount || Number.isNaN(numeric) || numeric <= 0 || numeric > available

  return (
    <dialog className="modal modal-open">
      <div className="modal-box max-w-sm">
        <h3 className="mb-3 text-lg font-bold">Request a payout</h3>
        <p className="mb-3 text-sm text-base-content/60">
          Available to withdraw: <span className="font-medium text-success">{money.format(available)}</span>
        </p>

        {error && <div role="alert" className="alert alert-error mb-3 text-sm"><span>{error}</span></div>}

        <label className="form-control">
          <span className="label-text font-medium">Amount</span>
          <input
            type="number"
            step="0.01"
            min="0.01"
            max={available}
            value={amount}
            onChange={(e) => { setAmount(e.target.value); setError(null) }}
            className="input input-bordered w-full"
            placeholder="0.00"
            autoFocus
          />
        </label>

        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={invalid || submit.isPending} onClick={() => submit.mutate()}>
            {submit.isPending && <Loader2 className="h-4 w-4 animate-spin" />} Request
          </button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}
