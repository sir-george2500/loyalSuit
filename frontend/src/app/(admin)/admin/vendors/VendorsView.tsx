'use client'

import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { Store, AlertCircle, Loader2, ChevronLeft, ChevronRight, Check, X, Ban, RotateCcw, Percent } from 'lucide-react'
import { adminVendorApi } from '@/lib/api/vendors'
import type { Vendor, VendorStatus } from '@/types'

const STATUS_BADGE: Record<VendorStatus, string> = {
  PENDING: 'badge-warning',
  ACTIVE: 'badge-success',
  SUSPENDED: 'badge-error',
  REJECTED: 'badge-neutral',
}

function titleCase(s: string): string {
  return s.charAt(0) + s.slice(1).toLowerCase()
}

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function VendorsView() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<VendorStatus | ''>('')
  const [commissionFor, setCommissionFor] = useState<Vendor | null>(null)
  const [error, setError] = useState<string | null>(null)

  const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['vendors', page, status],
    queryFn: async () => (await adminVendorApi.list({ status, page })).data.data,
    placeholderData: keepPreviousData,
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['vendors'] })

  const action = useMutation({
    mutationFn: ({ id, op }: { id: string; op: 'approve' | 'reject' | 'suspend' | 'reinstate' }) =>
      adminVendorApi[op](id),
    onSuccess: () => { invalidate(); setError(null) },
    onError: (err) => setError(errorMessage(err, 'Could not update the vendor.')),
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load vendors. Try again.</p>
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
          onChange={(e) => { setStatus(e.target.value as VendorStatus | ''); setPage(0) }}
          className="select select-bordered select-sm"
        >
          <option value="">All</option>
          <option value="PENDING">Pending</option>
          <option value="ACTIVE">Active</option>
          <option value="SUSPENDED">Suspended</option>
          <option value="REJECTED">Rejected</option>
        </select>
        {isFetching && <span className="loading loading-spinner loading-xs text-primary" />}
      </div>

      {error && <div role="alert" className="alert alert-error text-sm"><span>{error}</span></div>}

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr><th>Store</th><th className="text-center">Commission</th><th>Status</th><th className="text-right">Actions</th></tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i}><td colSpan={4}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((v) => (
                <tr key={v.id}>
                  <td>
                    <div className="font-medium">{v.storeName}</div>
                    <div className="font-mono text-xs text-base-content/50">{v.slug}</div>
                  </td>
                  <td className="text-center">{v.commissionRate}%</td>
                  <td><span className={`badge badge-sm ${STATUS_BADGE[v.status]}`}>{titleCase(v.status)}</span></td>
                  <td>
                    <div className="flex flex-wrap justify-end gap-1">
                      {v.status === 'PENDING' && (
                        <>
                          <button className="btn btn-success btn-xs gap-1" disabled={action.isPending}
                            onClick={() => { setError(null); action.mutate({ id: v.id, op: 'approve' }) }}>
                            <Check className="h-3.5 w-3.5" /> Approve
                          </button>
                          <button className="btn btn-ghost btn-xs gap-1 text-error" disabled={action.isPending}
                            onClick={() => { setError(null); action.mutate({ id: v.id, op: 'reject' }) }}>
                            <X className="h-3.5 w-3.5" /> Reject
                          </button>
                        </>
                      )}
                      {v.status === 'ACTIVE' && (
                        <button className="btn btn-ghost btn-xs gap-1 text-error" disabled={action.isPending}
                          onClick={() => { setError(null); action.mutate({ id: v.id, op: 'suspend' }) }}>
                          <Ban className="h-3.5 w-3.5" /> Suspend
                        </button>
                      )}
                      {v.status === 'SUSPENDED' && (
                        <button className="btn btn-ghost btn-xs gap-1" disabled={action.isPending}
                          onClick={() => { setError(null); action.mutate({ id: v.id, op: 'reinstate' }) }}>
                          <RotateCcw className="h-3.5 w-3.5" /> Reinstate
                        </button>
                      )}
                      {v.status !== 'REJECTED' && (
                        <button className="btn btn-ghost btn-xs gap-1" onClick={() => { setError(null); setCommissionFor(v) }}>
                          <Percent className="h-3.5 w-3.5" /> Commission
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={4}>
                <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                  <Store className="h-8 w-8" /><p className="text-sm">No vendors yet.</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-base-content/60">
          <span>Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} vendor{data.totalElements === 1 ? '' : 's'}</span>
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

      {commissionFor && (
        <CommissionModal
          vendor={commissionFor}
          onClose={() => setCommissionFor(null)}
          onSaved={() => { invalidate(); setCommissionFor(null) }}
        />
      )}
    </div>
  )
}

function CommissionModal({ vendor, onClose, onSaved }: { vendor: Vendor; onClose: () => void; onSaved: () => void }) {
  const [rate, setRate] = useState(String(vendor.commissionRate))
  const [error, setError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: () => adminVendorApi.setCommission(vendor.id, Number(rate)),
    onSuccess: onSaved,
    onError: (err) => setError(errorMessage(err, 'Could not update the commission.')),
  })

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">Commission for {vendor.storeName}</h3>
        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}
        <label className="form-control mt-2">
          <span className="label-text">Rate (%)</span>
          <input
            type="number" min={0} max={100} step="0.5" value={rate}
            onChange={(e) => setRate(e.target.value)}
            className="input input-bordered"
          />
        </label>
        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={save.isPending} onClick={() => save.mutate()}>
            {save.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
            Save
          </button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}
