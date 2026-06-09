'use client'

import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import {
  Truck, AlertCircle, Loader2, ChevronLeft, ChevronRight, UserPlus, Pencil, Ban, RotateCcw,
} from 'lucide-react'
import { adminDeliveryAgentApi } from '@/lib/api/delivery'
import type { DeliveryAgent, VehicleType } from '@/types'

const VEHICLES: VehicleType[] = ['ON_FOOT', 'BICYCLE', 'MOTORBIKE', 'CAR', 'VAN', 'TRUCK']

function vehicleLabel(v: VehicleType): string {
  return v.split('_').map((w) => w.charAt(0) + w.slice(1).toLowerCase()).join(' ')
}

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function DeliveryAgentsView() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [active, setActive] = useState<'' | 'true' | 'false'>('')
  const [registering, setRegistering] = useState(false)
  const [editing, setEditing] = useState<DeliveryAgent | null>(null)
  const [error, setError] = useState<string | null>(null)

  const activeParam = active === '' ? undefined : active === 'true'

  const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['delivery-agents', page, active],
    queryFn: async () => (await adminDeliveryAgentApi.list({ active: activeParam, page })).data.data,
    placeholderData: keepPreviousData,
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['delivery-agents'] })

  const toggle = useMutation({
    mutationFn: ({ id, op }: { id: string; op: 'activate' | 'deactivate' }) => adminDeliveryAgentApi[op](id),
    onSuccess: () => { invalidate(); setError(null) },
    onError: (err) => setError(errorMessage(err, 'Could not update the agent.')),
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load delivery agents. Try again.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <label className="text-sm text-base-content/60" htmlFor="active-filter">Status</label>
        <select
          id="active-filter"
          value={active}
          onChange={(e) => { setActive(e.target.value as '' | 'true' | 'false'); setPage(0) }}
          className="select select-bordered select-sm"
        >
          <option value="">All</option>
          <option value="true">Active</option>
          <option value="false">Inactive</option>
        </select>
        {isFetching && <span className="loading loading-spinner loading-xs text-primary" />}
        <button className="btn btn-primary btn-sm ml-auto gap-1" onClick={() => { setError(null); setRegistering(true) }}>
          <UserPlus className="h-4 w-4" /> Onboard agent
        </button>
      </div>

      {error && <div role="alert" className="alert alert-error text-sm"><span>{error}</span></div>}

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr><th>Agent</th><th>Phone</th><th>Vehicle</th><th>Status</th><th className="text-right">Actions</th></tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i}><td colSpan={5}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((a) => (
                <tr key={a.id}>
                  <td>
                    <div className="font-medium">{a.name ?? '—'}</div>
                    <div className="text-xs text-base-content/50">{a.email ?? ''}</div>
                  </td>
                  <td>{a.phone}</td>
                  <td>{vehicleLabel(a.vehicleType)}</td>
                  <td>
                    <span className={`badge badge-sm ${a.active ? 'badge-success' : 'badge-neutral'}`}>
                      {a.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td>
                    <div className="flex flex-wrap justify-end gap-1">
                      <button className="btn btn-ghost btn-xs gap-1" onClick={() => { setError(null); setEditing(a) }}>
                        <Pencil className="h-3.5 w-3.5" /> Edit
                      </button>
                      {a.active ? (
                        <button className="btn btn-ghost btn-xs gap-1 text-error" disabled={toggle.isPending}
                          onClick={() => { setError(null); toggle.mutate({ id: a.id, op: 'deactivate' }) }}>
                          <Ban className="h-3.5 w-3.5" /> Deactivate
                        </button>
                      ) : (
                        <button className="btn btn-ghost btn-xs gap-1" disabled={toggle.isPending}
                          onClick={() => { setError(null); toggle.mutate({ id: a.id, op: 'activate' }) }}>
                          <RotateCcw className="h-3.5 w-3.5" /> Activate
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={5}>
                <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                  <Truck className="h-8 w-8" /><p className="text-sm">No delivery agents yet.</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-base-content/60">
          <span>Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} agent{data.totalElements === 1 ? '' : 's'}</span>
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

      {registering && (
        <RegisterModal onClose={() => setRegistering(false)} onSaved={() => { invalidate(); setRegistering(false) }} />
      )}
      {editing && (
        <EditModal agent={editing} onClose={() => setEditing(null)} onSaved={() => { invalidate(); setEditing(null) }} />
      )}
    </div>
  )
}

function VehicleSelect({ value, onChange }: { value: VehicleType; onChange: (v: VehicleType) => void }) {
  return (
    <label className="form-control mt-2">
      <span className="label-text">Vehicle</span>
      <select className="select select-bordered" value={value} onChange={(e) => onChange(e.target.value as VehicleType)}>
        {VEHICLES.map((v) => <option key={v} value={v}>{vehicleLabel(v)}</option>)}
      </select>
    </label>
  )
}

function RegisterModal({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [vehicleType, setVehicleType] = useState<VehicleType>('MOTORBIKE')
  const [error, setError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: () => adminDeliveryAgentApi.register({ email: email.trim(), phone: phone.trim(), vehicleType }),
    onSuccess: onSaved,
    onError: (err) => setError(errorMessage(err, 'Could not onboard the agent.')),
  })

  const canSave = email.trim() !== '' && phone.trim() !== '' && !save.isPending

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">Onboard a delivery agent</h3>
        <p className="mt-1 text-sm text-base-content/60">
          Enter the email of an existing user in this store. They’ll be granted the delivery-agent role.
        </p>
        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}
        <label className="form-control mt-2">
          <span className="label-text">User email</span>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
            placeholder="rider@store.com" className="input input-bordered" />
        </label>
        <label className="form-control mt-2">
          <span className="label-text">Contact phone</span>
          <input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)}
            placeholder="+1 555 0100" className="input input-bordered" />
        </label>
        <VehicleSelect value={vehicleType} onChange={setVehicleType} />
        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={!canSave} onClick={() => save.mutate()}>
            {save.isPending && <Loader2 className="h-4 w-4 animate-spin" />} Onboard
          </button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}

function EditModal({ agent, onClose, onSaved }: { agent: DeliveryAgent; onClose: () => void; onSaved: () => void }) {
  const [phone, setPhone] = useState(agent.phone)
  const [vehicleType, setVehicleType] = useState<VehicleType>(agent.vehicleType)
  const [error, setError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: () => adminDeliveryAgentApi.update(agent.id, { phone: phone.trim(), vehicleType }),
    onSuccess: onSaved,
    onError: (err) => setError(errorMessage(err, 'Could not update the agent.')),
  })

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">Edit {agent.name ?? 'agent'}</h3>
        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}
        <label className="form-control mt-2">
          <span className="label-text">Contact phone</span>
          <input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} className="input input-bordered" />
        </label>
        <VehicleSelect value={vehicleType} onChange={setVehicleType} />
        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={phone.trim() === '' || save.isPending} onClick={() => save.mutate()}>
            {save.isPending && <Loader2 className="h-4 w-4 animate-spin" />} Save
          </button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}
