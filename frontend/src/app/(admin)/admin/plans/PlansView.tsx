'use client'

import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { Layers, AlertCircle, Loader2, Plus, Pencil, Ban, RotateCcw } from 'lucide-react'
import { planApi, type UpsertPlanPayload } from '@/lib/api/plans'
import type { BillingInterval, Plan } from '@/types'

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

function money(amount: number, currency: string): string {
  return new Intl.NumberFormat('en-RW', { style: 'currency', currency, maximumFractionDigits: 0 }).format(amount)
}

export default function PlansView() {
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState<Plan | null>(null)
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['plans'],
    queryFn: async () => (await planApi.list()).data.data,
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['plans'] })

  const toggleActive = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      active ? planApi.deactivate(id) : planApi.activate(id),
    onSuccess: () => { invalidate(); setError(null) },
    onError: (err) => setError(errorMessage(err, 'Could not update the plan.')),
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load plans. Try again.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <button className="btn btn-primary btn-sm gap-1" onClick={() => { setError(null); setCreating(true) }}>
          <Plus className="h-4 w-4" /> New plan
        </button>
      </div>

      {error && <div role="alert" className="alert alert-error text-sm"><span>{error}</span></div>}

      {isLoading ? (
        <div className="grid gap-4 md:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="card border border-base-300 bg-base-100"><div className="card-body"><div className="h-28 animate-pulse rounded bg-base-200" /></div></div>
          ))}
        </div>
      ) : data && data.length > 0 ? (
        <div className="grid gap-4 md:grid-cols-3">
          {data.map((p) => (
            <div key={p.id} className="card border border-base-300 bg-base-100">
              <div className="card-body">
                <div className="flex items-start justify-between">
                  <div>
                    <h2 className="card-title text-base">{p.name}</h2>
                    <div className="font-mono text-xs text-base-content/50">{p.code}</div>
                  </div>
                  <span className={`badge badge-sm ${p.active ? 'badge-success' : 'badge-ghost'}`}>{p.active ? 'Active' : 'Inactive'}</span>
                </div>
                <div className="text-2xl font-bold">
                  {money(p.price, p.currency)}
                  <span className="text-sm font-normal text-base-content/50">/{p.billingInterval === 'YEARLY' ? 'yr' : 'mo'}</span>
                </div>
                {p.description && <p className="text-sm text-base-content/60">{p.description}</p>}
                <ul className="mt-1 space-y-1 text-sm text-base-content/70">
                  <li>{p.maxProducts == null ? 'Unlimited products' : `Up to ${p.maxProducts} products`}</li>
                  <li>{p.maxStaff == null ? 'Unlimited staff' : `Up to ${p.maxStaff} staff`}</li>
                </ul>
                <div className="card-actions mt-2 justify-end">
                  <button className="btn btn-ghost btn-xs gap-1" onClick={() => { setError(null); setEditing(p) }}>
                    <Pencil className="h-3.5 w-3.5" /> Edit
                  </button>
                  {p.active ? (
                    <button className="btn btn-ghost btn-xs gap-1 text-error" disabled={toggleActive.isPending}
                      onClick={() => { setError(null); toggleActive.mutate({ id: p.id, active: true }) }}>
                      <Ban className="h-3.5 w-3.5" /> Deactivate
                    </button>
                  ) : (
                    <button className="btn btn-ghost btn-xs gap-1" disabled={toggleActive.isPending}
                      onClick={() => { setError(null); toggleActive.mutate({ id: p.id, active: false }) }}>
                      <RotateCcw className="h-3.5 w-3.5" /> Activate
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="card border border-base-300 bg-base-100">
          <div className="card-body items-center gap-2 py-10 text-center text-base-content/50">
            <Layers className="h-8 w-8" /><p className="text-sm">No plans yet. Create your first one.</p>
          </div>
        </div>
      )}

      {(creating || editing) && (
        <PlanModal
          plan={editing}
          onClose={() => { setCreating(false); setEditing(null) }}
          onSaved={() => { invalidate(); setCreating(false); setEditing(null) }}
        />
      )}
    </div>
  )
}

function PlanModal({ plan, onClose, onSaved }: { plan: Plan | null; onClose: () => void; onSaved: () => void }) {
  const [form, setForm] = useState<UpsertPlanPayload>({
    code: plan?.code ?? '',
    name: plan?.name ?? '',
    description: plan?.description ?? '',
    price: plan?.price ?? 0,
    currency: plan?.currency ?? 'RWF',
    billingInterval: plan?.billingInterval ?? 'MONTHLY',
    maxProducts: plan?.maxProducts ?? null,
    maxStaff: plan?.maxStaff ?? null,
    active: plan?.active ?? true,
  })
  const [error, setError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: () => (plan ? planApi.update(plan.id, form) : planApi.create(form)),
    onSuccess: onSaved,
    onError: (err) => setError(errorMessage(err, 'Could not save the plan.')),
  })

  const numOrNull = (v: string) => (v.trim() === '' ? null : Number(v))

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">{plan ? 'Edit plan' : 'New plan'}</h3>
        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}

        <div className="grid grid-cols-2 gap-3 pt-2">
          <label className="form-control">
            <span className="label-text">Code</span>
            <input className="input input-bordered input-sm" value={form.code} disabled={!!plan}
              onChange={(e) => setForm({ ...form, code: e.target.value })} />
          </label>
          <label className="form-control">
            <span className="label-text">Name</span>
            <input className="input input-bordered input-sm" value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </label>
          <label className="form-control col-span-2">
            <span className="label-text">Description</span>
            <input className="input input-bordered input-sm" value={form.description ?? ''}
              onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </label>
          <label className="form-control">
            <span className="label-text">Price</span>
            <input type="number" min={0} className="input input-bordered input-sm" value={form.price}
              onChange={(e) => setForm({ ...form, price: Number(e.target.value) })} />
          </label>
          <label className="form-control">
            <span className="label-text">Currency</span>
            <input className="input input-bordered input-sm" value={form.currency}
              onChange={(e) => setForm({ ...form, currency: e.target.value.toUpperCase() })} />
          </label>
          <label className="form-control">
            <span className="label-text">Billing interval</span>
            <select className="select select-bordered select-sm" value={form.billingInterval}
              onChange={(e) => setForm({ ...form, billingInterval: e.target.value as BillingInterval })}>
              <option value="MONTHLY">Monthly</option>
              <option value="YEARLY">Yearly</option>
            </select>
          </label>
          <label className="form-control">
            <span className="label-text">Status</span>
            <select className="select select-bordered select-sm" value={form.active ? 'true' : 'false'}
              onChange={(e) => setForm({ ...form, active: e.target.value === 'true' })}>
              <option value="true">Active</option>
              <option value="false">Inactive</option>
            </select>
          </label>
          <label className="form-control">
            <span className="label-text">Max products <span className="text-base-content/40">(blank = ∞)</span></span>
            <input type="number" min={0} className="input input-bordered input-sm" value={form.maxProducts ?? ''}
              onChange={(e) => setForm({ ...form, maxProducts: numOrNull(e.target.value) })} />
          </label>
          <label className="form-control">
            <span className="label-text">Max staff <span className="text-base-content/40">(blank = ∞)</span></span>
            <input type="number" min={0} className="input input-bordered input-sm" value={form.maxStaff ?? ''}
              onChange={(e) => setForm({ ...form, maxStaff: numOrNull(e.target.value) })} />
          </label>
        </div>

        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={save.isPending} onClick={() => { setError(null); save.mutate() }}>
            {save.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
            Save
          </button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}
