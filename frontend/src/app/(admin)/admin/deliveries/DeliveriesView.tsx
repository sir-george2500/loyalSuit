'use client'

import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import {
  Truck, AlertCircle, Loader2, ChevronLeft, ChevronRight, PackagePlus, ArrowRight, History,
} from 'lucide-react'
import { adminDeliveryApi, adminDeliveryAgentApi } from '@/lib/api/delivery'
import type { Delivery, DeliveryStatus } from '@/types'

const STATUS_BADGE: Record<DeliveryStatus, string> = {
  PENDING: 'badge-ghost',
  ASSIGNED: 'badge-info',
  PICKED_UP: 'badge-primary',
  IN_TRANSIT: 'badge-warning',
  DELIVERED: 'badge-success',
  FAILED: 'badge-error',
}

/** Valid next statuses, mirroring the backend state machine (assignment is its own action). */
const NEXT_STATUSES: Record<DeliveryStatus, DeliveryStatus[]> = {
  PENDING: [],
  ASSIGNED: ['PICKED_UP', 'FAILED'],
  PICKED_UP: ['IN_TRANSIT', 'FAILED'],
  IN_TRANSIT: ['DELIVERED', 'FAILED'],
  DELIVERED: [],
  FAILED: [],
}

function statusLabel(s: DeliveryStatus): string {
  return s.split('_').map((w) => w.charAt(0) + w.slice(1).toLowerCase()).join(' ')
}

function fmt(ts?: string | null): string {
  return ts ? new Date(ts).toLocaleString() : '—'
}

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function DeliveriesView() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<DeliveryStatus | ''>('')
  const [assigning, setAssigning] = useState(false)
  const [advancing, setAdvancing] = useState<Delivery | null>(null)
  const [timelineFor, setTimelineFor] = useState<Delivery | null>(null)

  const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['deliveries', page, status],
    queryFn: async () => (await adminDeliveryApi.list({ status, page })).data.data,
    placeholderData: keepPreviousData,
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['deliveries'] })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load deliveries. Try again.</p>
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
          onChange={(e) => { setStatus(e.target.value as DeliveryStatus | ''); setPage(0) }}
          className="select select-bordered select-sm"
        >
          <option value="">All</option>
          {(Object.keys(STATUS_BADGE) as DeliveryStatus[]).map((s) => (
            <option key={s} value={s}>{statusLabel(s)}</option>
          ))}
        </select>
        {isFetching && <span className="loading loading-spinner loading-xs text-primary" />}
        <button className="btn btn-primary btn-sm ml-auto gap-1" onClick={() => setAssigning(true)}>
          <PackagePlus className="h-4 w-4" /> Assign delivery
        </button>
      </div>

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr><th>Order</th><th>Customer</th><th>Agent</th><th>Status</th><th className="text-right">Actions</th></tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i}><td colSpan={5}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((d) => (
                <tr key={d.id}>
                  <td className="font-mono text-xs">{d.orderNumber}</td>
                  <td>{d.customerName ?? '—'}</td>
                  <td>{d.agentName ?? <span className="text-base-content/40">Unassigned</span>}</td>
                  <td><span className={`badge badge-sm ${STATUS_BADGE[d.status]}`}>{statusLabel(d.status)}</span></td>
                  <td>
                    <div className="flex flex-wrap justify-end gap-1">
                      {NEXT_STATUSES[d.status].length > 0 && (
                        <button className="btn btn-ghost btn-xs gap-1" onClick={() => setAdvancing(d)}>
                          <ArrowRight className="h-3.5 w-3.5" /> Update
                        </button>
                      )}
                      <button className="btn btn-ghost btn-xs gap-1" onClick={() => setTimelineFor(d)}>
                        <History className="h-3.5 w-3.5" /> Timeline
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={5}>
                <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                  <Truck className="h-8 w-8" /><p className="text-sm">No deliveries yet.</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-base-content/60">
          <span>Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} deliver{data.totalElements === 1 ? 'y' : 'ies'}</span>
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

      {assigning && <AssignModal onClose={() => setAssigning(false)} onSaved={() => { invalidate(); setAssigning(false) }} />}
      {advancing && (
        <AdvanceModal delivery={advancing} onClose={() => setAdvancing(null)} onSaved={() => { invalidate(); setAdvancing(null) }} />
      )}
      {timelineFor && <TimelineModal delivery={timelineFor} onClose={() => setTimelineFor(null)} />}
    </div>
  )
}

function AssignModal({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const [orderNumber, setOrderNumber] = useState('')
  const [agentId, setAgentId] = useState('')
  const [error, setError] = useState<string | null>(null)

  const { data: agents } = useQuery({
    queryKey: ['delivery-agents', 'active-options'],
    queryFn: async () => (await adminDeliveryAgentApi.list({ active: true })).data.data.content,
  })

  const save = useMutation({
    mutationFn: () => adminDeliveryApi.assign({ orderNumber: orderNumber.trim(), agentId }),
    onSuccess: onSaved,
    onError: (err) => setError(errorMessage(err, 'Could not assign the delivery.')),
  })

  const canSave = orderNumber.trim() !== '' && agentId !== '' && !save.isPending

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">Assign a delivery</h3>
        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}
        <label className="form-control mt-2">
          <span className="label-text">Order number</span>
          <input value={orderNumber} onChange={(e) => setOrderNumber(e.target.value)}
            placeholder="ORD-1001" className="input input-bordered" />
        </label>
        <label className="form-control mt-2">
          <span className="label-text">Courier</span>
          <select className="select select-bordered" value={agentId} onChange={(e) => setAgentId(e.target.value)}>
            <option value="" disabled>Select an agent…</option>
            {agents?.map((a) => <option key={a.id} value={a.id}>{a.name ?? a.email}</option>)}
          </select>
        </label>
        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={!canSave} onClick={() => save.mutate()}>
            {save.isPending && <Loader2 className="h-4 w-4 animate-spin" />} Assign
          </button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}

function AdvanceModal({ delivery, onClose, onSaved }: { delivery: Delivery; onClose: () => void; onSaved: () => void }) {
  const [target, setTarget] = useState<DeliveryStatus>(NEXT_STATUSES[delivery.status][0])
  const [note, setNote] = useState('')
  const [recipient, setRecipient] = useState('')
  const [error, setError] = useState<string | null>(null)

  const isComplete = target === 'DELIVERED'
  const needsReason = target === 'FAILED'

  const save = useMutation({
    // DELIVERED captures proof and settles a cash-on-delivery order, so it goes through complete().
    mutationFn: () =>
      isComplete
        ? adminDeliveryApi.complete(delivery.id, { recipientName: recipient.trim(), note: note.trim() || undefined })
        : adminDeliveryApi.advance(delivery.id, target, note.trim() || undefined),
    onSuccess: onSaved,
    onError: (err) => setError(errorMessage(err, 'Could not update the delivery.')),
  })

  const canSave = !save.isPending && (!needsReason || note.trim() !== '') && (!isComplete || recipient.trim() !== '')

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">Update delivery {delivery.orderNumber}</h3>
        {isComplete && (
          <p className="mt-1 text-sm text-base-content/60">
            Completing settles a cash-on-delivery order (marks it paid and earns commission).
          </p>
        )}
        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}
        <label className="form-control mt-2">
          <span className="label-text">New status</span>
          <select className="select select-bordered" value={target} onChange={(e) => setTarget(e.target.value as DeliveryStatus)}>
            {NEXT_STATUSES[delivery.status].map((s) => <option key={s} value={s}>{statusLabel(s)}</option>)}
          </select>
        </label>
        {isComplete && (
          <label className="form-control mt-2">
            <span className="label-text">Received by <span className="text-error">(required)</span></span>
            <input value={recipient} onChange={(e) => setRecipient(e.target.value)}
              placeholder="Recipient name" className="input input-bordered" />
          </label>
        )}
        <label className="form-control mt-2">
          <span className="label-text">Note {needsReason && <span className="text-error">(required)</span>}</span>
          <textarea value={note} onChange={(e) => setNote(e.target.value)} rows={2}
            placeholder={needsReason ? 'Why did it fail?' : 'Optional'} className="textarea textarea-bordered" />
        </label>
        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={!canSave} onClick={() => save.mutate()}>
            {save.isPending && <Loader2 className="h-4 w-4 animate-spin" />} Save
          </button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}

function TimelineModal({ delivery, onClose }: { delivery: Delivery; onClose: () => void }) {
  const { data, isLoading } = useQuery({
    queryKey: ['delivery', delivery.id],
    queryFn: async () => (await adminDeliveryApi.get(delivery.id)).data.data,
  })

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">Timeline · {delivery.orderNumber}</h3>
        {isLoading ? (
          <div className="py-6 text-center"><span className="loading loading-spinner text-primary" /></div>
        ) : (
          <ul className="mt-4 space-y-3">
            {data?.events?.length ? data.events.map((e, i) => (
              <li key={i} className="flex gap-3 text-sm">
                <span className={`badge badge-sm ${STATUS_BADGE[e.status]} mt-0.5`}>{statusLabel(e.status)}</span>
                <div className="min-w-0">
                  <div className="text-base-content/60">{fmt(e.occurredAt)}</div>
                  {e.note && <div className="text-base-content/80">{e.note}</div>}
                </div>
              </li>
            )) : <p className="text-sm text-base-content/50">No events yet.</p>}
          </ul>
        )}
        <div className="modal-action">
          <button className="btn btn-primary" onClick={onClose}>Close</button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}
