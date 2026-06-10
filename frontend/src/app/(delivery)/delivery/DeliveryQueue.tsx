'use client'

import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { Package, Loader2, MapPin, ArrowRight, X } from 'lucide-react'
import { deliveryAgentApi } from '@/lib/api/delivery'
import type { Delivery, DeliveryStatus } from '@/types'

const STATUS_BADGE: Record<DeliveryStatus, string> = {
  PENDING: 'badge-ghost',
  ASSIGNED: 'badge-info',
  PICKED_UP: 'badge-primary',
  IN_TRANSIT: 'badge-warning',
  DELIVERED: 'badge-success',
  FAILED: 'badge-error',
}

/** Valid next statuses, mirroring the backend state machine. */
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

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function DeliveryQueue() {
  const queryClient = useQueryClient()
  const [failing, setFailing] = useState<Delivery | null>(null)
  const [error, setError] = useState<string | null>(null)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['my-deliveries'],
    queryFn: async () => (await deliveryAgentApi.assignments()).data.data.content,
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['my-deliveries'] })

  const advance = useMutation({
    mutationFn: ({ id, status }: { id: string; status: DeliveryStatus }) =>
      deliveryAgentApi.advance(id, status),
    onSuccess: () => { invalidate(); setError(null) },
    onError: (err) => setError(errorMessage(err, 'Could not update the delivery.')),
  })

  if (isError) {
    return <p className="rounded-lg bg-gray-800 p-4 text-sm text-error">Couldn’t load your deliveries. Try again.</p>
  }

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="h-24 animate-pulse rounded-xl bg-gray-800" />
        ))}
      </div>
    )
  }

  if (!data || data.length === 0) {
    return (
      <div className="flex flex-col items-center gap-2 py-16 text-center text-gray-500">
        <Package className="h-10 w-10" />
        <p className="text-sm">No active deliveries right now.</p>
      </div>
    )
  }

  return (
    <div className="space-y-3">
      {error && <p className="rounded-lg bg-error/20 p-3 text-sm text-error">{error}</p>}

      {data.map((d) => (
        <div key={d.id} className="rounded-xl border border-gray-700 bg-gray-800 p-4">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <div className="font-mono text-sm">{d.orderNumber}</div>
              <div className="mt-0.5 flex items-center gap-1 text-sm text-gray-300">
                <MapPin className="h-3.5 w-3.5 text-gray-500" /> {d.customerName ?? 'Customer'}
              </div>
            </div>
            <span className={`badge badge-sm ${STATUS_BADGE[d.status]}`}>{statusLabel(d.status)}</span>
          </div>

          <div className="mt-3 flex flex-wrap gap-2">
            {NEXT_STATUSES[d.status].map((next) =>
              next === 'FAILED' ? (
                <button key={next} className="btn btn-outline btn-error btn-sm"
                  onClick={() => { setError(null); setFailing(d) }}>
                  Can’t deliver
                </button>
              ) : (
                <button key={next} className="btn btn-primary btn-sm gap-1" disabled={advance.isPending}
                  onClick={() => { setError(null); advance.mutate({ id: d.id, status: next }) }}>
                  {advance.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <ArrowRight className="h-4 w-4" />}
                  {statusLabel(next)}
                </button>
              ),
            )}
          </div>
        </div>
      ))}

      {failing && <FailModal delivery={failing} onClose={() => setFailing(null)} onDone={() => { invalidate(); setFailing(null) }} />}
    </div>
  )
}

function FailModal({ delivery, onClose, onDone }: { delivery: Delivery; onClose: () => void; onDone: () => void }) {
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)

  const fail = useMutation({
    mutationFn: () => deliveryAgentApi.advance(delivery.id, 'FAILED', reason.trim()),
    onSuccess: onDone,
    onError: (err) => setError(errorMessage(err, 'Could not update the delivery.')),
  })

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <div className="w-full max-w-sm rounded-xl bg-gray-800 p-5">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-semibold">Mark {delivery.orderNumber} failed</h3>
          <button onClick={onClose} className="btn btn-circle btn-ghost btn-sm"><X className="h-4 w-4" /></button>
        </div>
        {error && <p className="mt-2 text-sm text-error">{error}</p>}
        <textarea value={reason} onChange={(e) => setReason(e.target.value)} rows={3} autoFocus
          placeholder="What happened? (e.g. customer unreachable)"
          className="textarea textarea-bordered mt-3 w-full bg-gray-900 text-white" />
        <button className="btn btn-error btn-block mt-4"
          disabled={reason.trim() === '' || fail.isPending} onClick={() => fail.mutate()}>
          {fail.isPending && <Loader2 className="h-4 w-4 animate-spin" />} Mark failed
        </button>
      </div>
    </div>
  )
}
