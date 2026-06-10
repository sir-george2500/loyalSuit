'use client'

import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { Share2, AlertCircle, Loader2, UserPlus, Pencil, ChevronLeft, ChevronRight, Ban, RotateCcw } from 'lucide-react'
import { adminAffiliateApi } from '@/lib/api/affiliates'
import type { Affiliate } from '@/types'

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function AffiliatesView() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [registering, setRegistering] = useState(false)
  const [editing, setEditing] = useState<Affiliate | null>(null)
  const [error, setError] = useState<string | null>(null)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['affiliates', page],
    queryFn: async () => (await adminAffiliateApi.list(page)).data.data,
    placeholderData: keepPreviousData,
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['affiliates'] })

  const toggle = useMutation({
    mutationFn: ({ id, op }: { id: string; op: 'activate' | 'deactivate' }) => adminAffiliateApi[op](id),
    onSuccess: () => { invalidate(); setError(null) },
    onError: (err) => setError(errorMessage(err, 'Could not update the affiliate.')),
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load affiliates. Try again.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <button className="btn btn-primary btn-sm gap-1" onClick={() => { setError(null); setRegistering(true) }}>
          <UserPlus className="h-4 w-4" /> Add affiliate
        </button>
      </div>

      {error && <div role="alert" className="alert alert-error text-sm"><span>{error}</span></div>}

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr><th>Affiliate</th><th>Code</th><th className="text-center">Reward</th><th>Status</th><th className="text-right">Actions</th></tr>
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
                  <td className="font-mono font-medium">{a.code}</td>
                  <td className="text-center">{a.rewardRate}%</td>
                  <td><span className={`badge badge-sm ${a.active ? 'badge-success' : 'badge-neutral'}`}>{a.active ? 'Active' : 'Inactive'}</span></td>
                  <td>
                    <div className="flex flex-wrap justify-end gap-1">
                      <button className="btn btn-ghost btn-xs gap-1" onClick={() => { setError(null); setEditing(a) }}>
                        <Pencil className="h-3.5 w-3.5" /> Edit
                      </button>
                      {a.active ? (
                        <button className="btn btn-ghost btn-xs gap-1 text-error" disabled={toggle.isPending}
                          onClick={() => toggle.mutate({ id: a.id, op: 'deactivate' })}>
                          <Ban className="h-3.5 w-3.5" /> Deactivate
                        </button>
                      ) : (
                        <button className="btn btn-ghost btn-xs gap-1" disabled={toggle.isPending}
                          onClick={() => toggle.mutate({ id: a.id, op: 'activate' })}>
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
                  <Share2 className="h-8 w-8" /><p className="text-sm">No affiliates yet.</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-base-content/60">
          <span>Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} affiliate{data.totalElements === 1 ? '' : 's'}</span>
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

      {registering && <RegisterModal onClose={() => setRegistering(false)} onSaved={() => { invalidate(); setRegistering(false) }} />}
      {editing && <EditModal affiliate={editing} onClose={() => setEditing(null)} onSaved={() => { invalidate(); setEditing(null) }} />}
    </div>
  )
}

function RegisterModal({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [rewardRate, setRewardRate] = useState('')
  const [error, setError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: () => adminAffiliateApi.register({ email: email.trim(), code: code.trim(), rewardRate: Number(rewardRate) }),
    onSuccess: onSaved,
    onError: (err) => setError(errorMessage(err, 'Could not enrol the affiliate.')),
  })

  const invalid = email.trim() === '' || code.trim() === '' || Number.isNaN(Number(rewardRate)) || Number(rewardRate) <= 0 || save.isPending

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">Add an affiliate</h3>
        <p className="mt-1 text-sm text-base-content/60">Enter the email of an existing user in this store.</p>
        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}
        <label className="form-control mt-2">
          <span className="label-text">User email</span>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="affiliate@store.com" className="input input-bordered" />
        </label>
        <div className="mt-2 grid grid-cols-2 gap-3">
          <label className="form-control">
            <span className="label-text">Referral code</span>
            <input value={code} onChange={(e) => setCode(e.target.value)} placeholder="RILEY" className="input input-bordered uppercase" />
          </label>
          <label className="form-control">
            <span className="label-text">Reward %</span>
            <input type="number" min={0} max={100} step="0.5" value={rewardRate} onChange={(e) => setRewardRate(e.target.value)} className="input input-bordered" />
          </label>
        </div>
        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={invalid} onClick={() => save.mutate()}>
            {save.isPending && <Loader2 className="h-4 w-4 animate-spin" />} Add
          </button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}

function EditModal({ affiliate, onClose, onSaved }: { affiliate: Affiliate; onClose: () => void; onSaved: () => void }) {
  const [code, setCode] = useState(affiliate.code)
  const [rewardRate, setRewardRate] = useState(String(affiliate.rewardRate))
  const [error, setError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: () => adminAffiliateApi.update(affiliate.id, { code: code.trim(), rewardRate: Number(rewardRate) }),
    onSuccess: onSaved,
    onError: (err) => setError(errorMessage(err, 'Could not update the affiliate.')),
  })

  const invalid = code.trim() === '' || Number.isNaN(Number(rewardRate)) || Number(rewardRate) <= 0 || save.isPending

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">Edit {affiliate.name ?? 'affiliate'}</h3>
        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}
        <div className="mt-2 grid grid-cols-2 gap-3">
          <label className="form-control">
            <span className="label-text">Referral code</span>
            <input value={code} onChange={(e) => setCode(e.target.value)} className="input input-bordered uppercase" />
          </label>
          <label className="form-control">
            <span className="label-text">Reward %</span>
            <input type="number" min={0} max={100} step="0.5" value={rewardRate} onChange={(e) => setRewardRate(e.target.value)} className="input input-bordered" />
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
