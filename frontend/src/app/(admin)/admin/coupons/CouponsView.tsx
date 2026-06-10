'use client'

import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { Ticket, AlertCircle, Loader2, Plus, Pencil, ChevronLeft, ChevronRight, Ban, RotateCcw } from 'lucide-react'
import { adminCouponApi, type CouponPayload } from '@/lib/api/coupons'
import type { Coupon, DiscountType } from '@/types'

const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

function discountLabel(c: Coupon): string {
  return c.discountType === 'PERCENT' ? `${c.value}%` : money.format(c.value)
}

/** ISO instant → value for a <input type="datetime-local"> (local time, no seconds). */
function toLocalInput(iso?: string | null): string {
  if (!iso) return ''
  const d = new Date(iso)
  const offset = d.getTimezoneOffset() * 60000
  return new Date(d.getTime() - offset).toISOString().slice(0, 16)
}

function toIso(local: string): string | null {
  return local ? new Date(local).toISOString() : null
}

export default function CouponsView() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<Coupon | null>(null)
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['coupons', page],
    queryFn: async () => (await adminCouponApi.list(page)).data.data,
    placeholderData: keepPreviousData,
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['coupons'] })

  const toggle = useMutation({
    mutationFn: ({ id, op }: { id: string; op: 'activate' | 'deactivate' }) => adminCouponApi[op](id),
    onSuccess: () => { invalidate(); setError(null) },
    onError: (err) => setError(errorMessage(err, 'Could not update the coupon.')),
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load coupons. Try again.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <button className="btn btn-primary btn-sm gap-1" onClick={() => { setError(null); setCreating(true) }}>
          <Plus className="h-4 w-4" /> New coupon
        </button>
      </div>

      {error && <div role="alert" className="alert alert-error text-sm"><span>{error}</span></div>}

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr><th>Code</th><th>Discount</th><th className="text-center">Used</th><th>Status</th><th className="text-right">Actions</th></tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i}><td colSpan={5}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((c) => (
                <tr key={c.id}>
                  <td>
                    <div className="font-mono font-medium">{c.code}</div>
                    {c.description && <div className="text-xs text-base-content/50">{c.description}</div>}
                  </td>
                  <td>{discountLabel(c)}{c.minOrderSubtotal ? <span className="text-xs text-base-content/50"> · min {money.format(c.minOrderSubtotal)}</span> : null}</td>
                  <td className="text-center">{c.timesRedeemed}{c.usageLimit ? ` / ${c.usageLimit}` : ''}</td>
                  <td><span className={`badge badge-sm ${c.active ? 'badge-success' : 'badge-neutral'}`}>{c.active ? 'Active' : 'Inactive'}</span></td>
                  <td>
                    <div className="flex flex-wrap justify-end gap-1">
                      <button className="btn btn-ghost btn-xs gap-1" onClick={() => { setError(null); setEditing(c) }}>
                        <Pencil className="h-3.5 w-3.5" /> Edit
                      </button>
                      {c.active ? (
                        <button className="btn btn-ghost btn-xs gap-1 text-error" disabled={toggle.isPending}
                          onClick={() => toggle.mutate({ id: c.id, op: 'deactivate' })}>
                          <Ban className="h-3.5 w-3.5" /> Deactivate
                        </button>
                      ) : (
                        <button className="btn btn-ghost btn-xs gap-1" disabled={toggle.isPending}
                          onClick={() => toggle.mutate({ id: c.id, op: 'activate' })}>
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
                  <Ticket className="h-8 w-8" /><p className="text-sm">No coupons yet.</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-base-content/60">
          <span>Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} coupon{data.totalElements === 1 ? '' : 's'}</span>
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

      {(creating || editing) && (
        <CouponModal
          coupon={editing}
          onClose={() => { setCreating(false); setEditing(null) }}
          onSaved={() => { invalidate(); setCreating(false); setEditing(null) }}
        />
      )}
    </div>
  )
}

function CouponModal({ coupon, onClose, onSaved }: { coupon: Coupon | null; onClose: () => void; onSaved: () => void }) {
  const [code, setCode] = useState(coupon?.code ?? '')
  const [description, setDescription] = useState(coupon?.description ?? '')
  const [discountType, setDiscountType] = useState<DiscountType>(coupon?.discountType ?? 'PERCENT')
  const [value, setValue] = useState(coupon ? String(coupon.value) : '')
  const [minOrderSubtotal, setMinOrderSubtotal] = useState(coupon?.minOrderSubtotal != null ? String(coupon.minOrderSubtotal) : '')
  const [maxDiscount, setMaxDiscount] = useState(coupon?.maxDiscount != null ? String(coupon.maxDiscount) : '')
  const [usageLimit, setUsageLimit] = useState(coupon?.usageLimit != null ? String(coupon.usageLimit) : '')
  const [perCustomerLimit, setPerCustomerLimit] = useState(coupon?.perCustomerLimit != null ? String(coupon.perCustomerLimit) : '')
  const [startsAt, setStartsAt] = useState(toLocalInput(coupon?.startsAt))
  const [endsAt, setEndsAt] = useState(toLocalInput(coupon?.endsAt))
  const [error, setError] = useState<string | null>(null)

  const num = (s: string): number | null => (s.trim() === '' ? null : Number(s))

  const save = useMutation({
    mutationFn: () => {
      const payload: CouponPayload = {
        code: code.trim(),
        description: description.trim() || undefined,
        discountType,
        value: Number(value),
        minOrderSubtotal: num(minOrderSubtotal),
        maxDiscount: discountType === 'PERCENT' ? num(maxDiscount) : null,
        usageLimit: num(usageLimit),
        perCustomerLimit: num(perCustomerLimit),
        startsAt: toIso(startsAt),
        endsAt: toIso(endsAt),
      }
      return coupon ? adminCouponApi.update(coupon.id, payload) : adminCouponApi.create(payload)
    },
    onSuccess: onSaved,
    onError: (err) => setError(errorMessage(err, 'Could not save the coupon.')),
  })

  const invalid = code.trim() === '' || value.trim() === '' || Number.isNaN(Number(value)) || save.isPending

  return (
    <dialog className="modal modal-open">
      <div className="modal-box max-w-lg">
        <h3 className="text-lg font-bold">{coupon ? 'Edit coupon' : 'New coupon'}</h3>
        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}

        <div className="grid grid-cols-2 gap-3">
          <label className="form-control">
            <span className="label-text">Code</span>
            <input value={code} onChange={(e) => setCode(e.target.value)} placeholder="SAVE10" className="input input-bordered uppercase" />
          </label>
          <label className="form-control">
            <span className="label-text">Type</span>
            <select className="select select-bordered" value={discountType} onChange={(e) => setDiscountType(e.target.value as DiscountType)}>
              <option value="PERCENT">Percentage</option>
              <option value="FIXED_AMOUNT">Fixed amount</option>
            </select>
          </label>
        </div>

        <label className="form-control mt-2">
          <span className="label-text">Description</span>
          <input value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Optional" className="input input-bordered" />
        </label>

        <div className="mt-2 grid grid-cols-2 gap-3">
          <label className="form-control">
            <span className="label-text">{discountType === 'PERCENT' ? 'Percent off' : 'Amount off'}</span>
            <input type="number" min={0} step="0.01" value={value} onChange={(e) => setValue(e.target.value)} className="input input-bordered" />
          </label>
          {discountType === 'PERCENT' && (
            <label className="form-control">
              <span className="label-text">Max discount</span>
              <input type="number" min={0} step="0.01" value={maxDiscount} onChange={(e) => setMaxDiscount(e.target.value)} placeholder="No cap" className="input input-bordered" />
            </label>
          )}
        </div>

        <div className="mt-2 grid grid-cols-3 gap-3">
          <label className="form-control">
            <span className="label-text">Min subtotal</span>
            <input type="number" min={0} step="0.01" value={minOrderSubtotal} onChange={(e) => setMinOrderSubtotal(e.target.value)} placeholder="None" className="input input-bordered" />
          </label>
          <label className="form-control">
            <span className="label-text">Total uses</span>
            <input type="number" min={1} step="1" value={usageLimit} onChange={(e) => setUsageLimit(e.target.value)} placeholder="∞" className="input input-bordered" />
          </label>
          <label className="form-control">
            <span className="label-text">Per customer</span>
            <input type="number" min={1} step="1" value={perCustomerLimit} onChange={(e) => setPerCustomerLimit(e.target.value)} placeholder="∞" className="input input-bordered" />
          </label>
        </div>

        <div className="mt-2 grid grid-cols-2 gap-3">
          <label className="form-control">
            <span className="label-text">Starts</span>
            <input type="datetime-local" value={startsAt} onChange={(e) => setStartsAt(e.target.value)} className="input input-bordered" />
          </label>
          <label className="form-control">
            <span className="label-text">Ends</span>
            <input type="datetime-local" value={endsAt} onChange={(e) => setEndsAt(e.target.value)} className="input input-bordered" />
          </label>
        </div>

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
