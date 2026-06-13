'use client'

import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { MapPin, AlertCircle, Loader2, Plus, Pencil, Trash2, Ban, RotateCcw } from 'lucide-react'
import { adminPickupApi } from '@/lib/api/pickup'
import type { DeliveryZone, PickupPoint } from '@/types'

const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'RWF' })

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function PickupPointsView() {
  const queryClient = useQueryClient()
  const [editingPoint, setEditingPoint] = useState<PickupPoint | null>(null)
  const [creatingPoint, setCreatingPoint] = useState(false)
  const [zoneFor, setZoneFor] = useState<{ point: PickupPoint; zone: DeliveryZone | null } | null>(null)
  const [error, setError] = useState<string | null>(null)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['pickup-points'],
    queryFn: async () => (await adminPickupApi.list()).data.data,
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['pickup-points'] })

  const toggle = useMutation({
    mutationFn: ({ id, op }: { id: string; op: 'activate' | 'deactivate' }) => adminPickupApi[op](id),
    onSuccess: () => { invalidate(); setError(null) },
    onError: (err) => setError(errorMessage(err, 'Could not update the pickup point.')),
  })

  const removeZone = useMutation({
    mutationFn: (zoneId: string) => adminPickupApi.deleteZone(zoneId),
    onSuccess: () => { invalidate(); setError(null) },
    onError: (err) => setError(errorMessage(err, 'Could not delete the zone.')),
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load pickup points. Try again.</p>
        </div>
      </div>
    )
  }

  const points = data?.content ?? []

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <button className="btn btn-primary btn-sm gap-1" onClick={() => { setError(null); setCreatingPoint(true) }}>
          <Plus className="h-4 w-4" /> Add pickup point
        </button>
      </div>

      {error && <div role="alert" className="alert alert-error text-sm"><span>{error}</span></div>}

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 2 }).map((_, i) => <div key={i} className="h-32 animate-pulse rounded-box bg-base-200" />)}
        </div>
      ) : points.length === 0 ? (
        <div className="flex flex-col items-center gap-2 rounded-box border border-base-300 bg-base-100 py-12 text-center text-base-content/50">
          <MapPin className="h-8 w-8" /><p className="text-sm">No pickup points yet.</p>
        </div>
      ) : (
        points.map((point) => (
          <div key={point.id} className="rounded-box border border-base-300 bg-base-100 p-4">
            <div className="flex items-start justify-between gap-3">
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-semibold">{point.name}</span>
                  <span className={`badge badge-sm ${point.active ? 'badge-success' : 'badge-neutral'}`}>
                    {point.active ? 'Active' : 'Inactive'}
                  </span>
                </div>
                <p className="text-xs text-base-content/50">{[point.address, point.phone].filter(Boolean).join(' · ') || '—'}</p>
              </div>
              <div className="flex gap-1">
                <button className="btn btn-ghost btn-xs gap-1" onClick={() => { setError(null); setEditingPoint(point) }}>
                  <Pencil className="h-3.5 w-3.5" /> Edit
                </button>
                {point.active ? (
                  <button className="btn btn-ghost btn-xs gap-1 text-error" disabled={toggle.isPending}
                    onClick={() => toggle.mutate({ id: point.id, op: 'deactivate' })}>
                    <Ban className="h-3.5 w-3.5" /> Deactivate
                  </button>
                ) : (
                  <button className="btn btn-ghost btn-xs gap-1" disabled={toggle.isPending}
                    onClick={() => toggle.mutate({ id: point.id, op: 'activate' })}>
                    <RotateCcw className="h-3.5 w-3.5" /> Activate
                  </button>
                )}
              </div>
            </div>

            <div className="mt-3 border-t border-base-200 pt-3">
              <div className="mb-2 flex items-center justify-between">
                <span className="text-sm font-medium text-base-content/70">Delivery zones</span>
                <button className="btn btn-ghost btn-xs gap-1" onClick={() => { setError(null); setZoneFor({ point, zone: null }) }}>
                  <Plus className="h-3.5 w-3.5" /> Add zone
                </button>
              </div>
              {point.zones.length === 0 ? (
                <p className="text-xs text-base-content/40">No zones — this point won’t be offered at checkout.</p>
              ) : (
                <ul className="divide-y divide-base-200">
                  {point.zones.map((zone) => (
                    <li key={zone.id} className="flex items-center justify-between py-1.5 text-sm">
                      <span className={zone.active ? '' : 'text-base-content/40 line-through'}>{zone.name}</span>
                      <div className="flex items-center gap-2">
                        <span className="font-medium">{money.format(zone.fee)}</span>
                        <button className="btn btn-ghost btn-xs" onClick={() => { setError(null); setZoneFor({ point, zone }) }}>
                          <Pencil className="h-3.5 w-3.5" />
                        </button>
                        <button className="btn btn-ghost btn-xs text-error" disabled={removeZone.isPending}
                          onClick={() => removeZone.mutate(zone.id)}>
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        ))
      )}

      {(creatingPoint || editingPoint) && (
        <PointModal
          point={editingPoint}
          onClose={() => { setCreatingPoint(false); setEditingPoint(null) }}
          onSaved={() => { invalidate(); setCreatingPoint(false); setEditingPoint(null) }}
        />
      )}
      {zoneFor && (
        <ZoneModal
          pointId={zoneFor.point.id}
          zone={zoneFor.zone}
          onClose={() => setZoneFor(null)}
          onSaved={() => { invalidate(); setZoneFor(null) }}
        />
      )}
    </div>
  )
}

function PointModal({ point, onClose, onSaved }: { point: PickupPoint | null; onClose: () => void; onSaved: () => void }) {
  const [name, setName] = useState(point?.name ?? '')
  const [address, setAddress] = useState(point?.address ?? '')
  const [phone, setPhone] = useState(point?.phone ?? '')
  const [error, setError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: () => {
      const payload = { name: name.trim(), address: address.trim() || undefined, phone: phone.trim() || undefined }
      return point ? adminPickupApi.update(point.id, payload) : adminPickupApi.create(payload)
    },
    onSuccess: onSaved,
    onError: (err) => setError(errorMessage(err, 'Could not save the pickup point.')),
  })

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">{point ? 'Edit pickup point' : 'New pickup point'}</h3>
        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}
        <label className="form-control mt-2">
          <span className="label-text">Name</span>
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Downtown branch" className="input input-bordered" />
        </label>
        <label className="form-control mt-2">
          <span className="label-text">Address</span>
          <input value={address} onChange={(e) => setAddress(e.target.value)} placeholder="Optional" className="input input-bordered" />
        </label>
        <label className="form-control mt-2">
          <span className="label-text">Phone</span>
          <input value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="Optional" className="input input-bordered" />
        </label>
        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={name.trim() === '' || save.isPending} onClick={() => save.mutate()}>
            {save.isPending && <Loader2 className="h-4 w-4 animate-spin" />} Save
          </button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}

function ZoneModal({ pointId, zone, onClose, onSaved }: {
  pointId: string; zone: DeliveryZone | null; onClose: () => void; onSaved: () => void
}) {
  const [name, setName] = useState(zone?.name ?? '')
  const [fee, setFee] = useState(zone ? String(zone.fee) : '')
  const [error, setError] = useState<string | null>(null)

  const feeNum = Number.parseFloat(fee)

  const save = useMutation({
    mutationFn: () => {
      const payload = { name: name.trim(), fee: feeNum }
      return zone ? adminPickupApi.updateZone(zone.id, payload) : adminPickupApi.addZone(pointId, payload)
    },
    onSuccess: onSaved,
    onError: (err) => setError(errorMessage(err, 'Could not save the zone.')),
  })

  const invalid = name.trim() === '' || Number.isNaN(feeNum) || feeNum < 0 || save.isPending

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">{zone ? 'Edit zone' : 'New delivery zone'}</h3>
        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}
        <label className="form-control mt-2">
          <span className="label-text">Zone name</span>
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="City centre" className="input input-bordered" />
        </label>
        <label className="form-control mt-2">
          <span className="label-text">Delivery fee</span>
          <input type="number" min={0} step="0.01" value={fee} onChange={(e) => setFee(e.target.value)}
            placeholder="0.00" className="input input-bordered" />
        </label>
        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={invalid} onClick={() => save.mutate()}>
            {save.isPending && <Loader2 className="h-4 w-4 animate-spin" />} Save
          </button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}
