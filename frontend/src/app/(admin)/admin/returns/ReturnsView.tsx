'use client'

import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { Undo2, AlertCircle, Loader2, ChevronLeft, ChevronRight, Check, X } from 'lucide-react'
import { returnsApi } from '@/lib/api/returns'
import type { ReturnResponse, ReturnStatus } from '@/types'

const STATUS_BADGE: Record<ReturnStatus, string> = {
  REQUESTED: 'badge-warning',
  APPROVED: 'badge-success',
  REJECTED: 'badge-neutral',
}

const dateFmt = new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })

function titleCase(s: string): string {
  return s.charAt(0) + s.slice(1).toLowerCase()
}

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function ReturnsView() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<ReturnStatus | ''>('')
  const [rejecting, setRejecting] = useState<ReturnResponse | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['returns', page, status],
    queryFn: async () => (await returnsApi.list({ status, page })).data.data,
    placeholderData: keepPreviousData,
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['returns'] })

  const approve = useMutation({
    mutationFn: (id: string) => returnsApi.approve(id),
    onSuccess: () => { invalidate(); setActionError(null) },
    onError: (err) => setActionError(errorMessage(err, 'Could not approve the return.')),
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load returns. Try again.</p>
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
          onChange={(e) => { setStatus(e.target.value as ReturnStatus | ''); setPage(0) }}
          className="select select-bordered select-sm"
        >
          <option value="">All</option>
          <option value="REQUESTED">Requested</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
        </select>
        {isFetching && <span className="loading loading-spinner loading-xs text-primary" />}
      </div>

      {actionError && <div role="alert" className="alert alert-error text-sm"><span>{actionError}</span></div>}

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr><th>Order</th><th>Reason</th><th>Status</th><th>Requested</th><th className="text-right">Actions</th></tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i}><td colSpan={5}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((r) => (
                <tr key={r.id}>
                  <td className="font-mono text-xs">{r.orderNumber}</td>
                  <td className="max-w-xs truncate text-base-content/70" title={r.reason}>{r.reason}</td>
                  <td><span className={`badge badge-sm ${STATUS_BADGE[r.status]}`}>{titleCase(r.status)}</span></td>
                  <td className="whitespace-nowrap text-base-content/60">{dateFmt.format(new Date(r.createdAt))}</td>
                  <td>
                    <div className="flex justify-end gap-1">
                      {r.status === 'REQUESTED' ? (
                        <>
                          <button className="btn btn-success btn-xs gap-1" disabled={approve.isPending} onClick={() => { setActionError(null); approve.mutate(r.id) }}>
                            <Check className="h-3.5 w-3.5" /> Approve
                          </button>
                          <button className="btn btn-ghost btn-xs gap-1 text-error" onClick={() => { setActionError(null); setRejecting(r) }}>
                            <X className="h-3.5 w-3.5" /> Reject
                          </button>
                        </>
                      ) : (
                        <span className="text-xs text-base-content/40">{r.resolutionNote ?? '—'}</span>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={5}>
                <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                  <Undo2 className="h-8 w-8" /><p className="text-sm">No return requests.</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-base-content/60">
          <span>Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} return{data.totalElements === 1 ? '' : 's'}</span>
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

      {rejecting && (
        <RejectModal
          target={rejecting}
          onClose={() => setRejecting(null)}
          onRejected={() => { invalidate(); setRejecting(null) }}
        />
      )}
    </div>
  )
}

function RejectModal({
  target,
  onClose,
  onRejected,
}: {
  target: ReturnResponse
  onClose: () => void
  onRejected: () => void
}) {
  const [note, setNote] = useState('')
  const [error, setError] = useState<string | null>(null)

  const reject = useMutation({
    mutationFn: () => returnsApi.reject(target.id, note.trim()),
    onSuccess: onRejected,
    onError: (err) => setError(errorMessage(err, 'Could not reject the return.')),
  })

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">Reject return for {target.orderNumber}</h3>
        <p className="py-2 text-sm text-base-content/60">Optionally tell the customer why.</p>
        {error && <div role="alert" className="alert alert-error mb-2 text-sm"><span>{error}</span></div>}
        <textarea
          value={note}
          onChange={(e) => setNote(e.target.value)}
          rows={3}
          placeholder="Reason (optional)"
          className="textarea textarea-bordered w-full"
        />
        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-error" disabled={reject.isPending} onClick={() => reject.mutate()}>
            {reject.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
            Reject
          </button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}
