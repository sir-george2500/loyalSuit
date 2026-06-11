'use client'

import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { Award as AwardIcon, AlertCircle, Loader2, Plus, Trash2, Lock } from 'lucide-react'
import { adminEmployeeApi } from '@/lib/api/employees'
import { adminAwardApi, type AwardPayload } from '@/lib/api/awards'
import type { Award, AwardCategory, Employee } from '@/types'

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

function statusOf(err: unknown): number | undefined {
  return (err as AxiosError)?.response?.status
}

const CATEGORIES: { label: string; value: AwardCategory }[] = [
  { label: 'Performance', value: 'PERFORMANCE' },
  { label: 'Milestone', value: 'MILESTONE' },
  { label: 'Teamwork', value: 'TEAMWORK' },
  { label: 'Innovation', value: 'INNOVATION' },
  { label: 'Other', value: 'OTHER' },
]

const CATEGORY_BADGE: Record<AwardCategory, string> = {
  PERFORMANCE: 'badge-primary', MILESTONE: 'badge-secondary', TEAMWORK: 'badge-accent',
  INNOVATION: 'badge-info', OTHER: 'badge-neutral',
}

function categoryLabel(c: AwardCategory): string {
  return CATEGORIES.find((x) => x.value === c)?.label ?? c
}

export default function AwardsView() {
  const queryClient = useQueryClient()
  const [employeeId, setEmployeeId] = useState('')
  const [adding, setAdding] = useState(false)
  const [confirmingId, setConfirmingId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const employees = useQuery({
    queryKey: ['employees', 'picker'],
    queryFn: async () => (await adminEmployeeApi.list(0, 'ACTIVE', 100)).data.data.content,
    retry: (count, err) => statusOf(err) !== 403 && count < 2,
  })

  const awards = useQuery({
    queryKey: ['awards', employeeId],
    queryFn: async () => (await adminAwardApi.list(employeeId)).data.data,
    enabled: employeeId !== '',
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['awards', employeeId] })

  const remove = useMutation({
    mutationFn: (id: string) => adminAwardApi.remove(id),
    onSuccess: () => { invalidate(); setConfirmingId(null); setError(null) },
    onError: (err) => setError(errorMessage(err, 'Could not remove the award.')),
  })

  if (employees.isError && statusOf(employees.error) === 403) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center gap-3 text-center">
          <Lock className="h-7 w-7 text-base-content/50" />
          <h2 className="card-title">Awards are a Professional feature</h2>
          <p className="max-w-md text-sm text-base-content/60">{errorMessage(employees.error, 'Upgrade your plan to recognise employees and keep an awards record.')}</p>
          <a href="/admin/settings" className="btn btn-primary btn-sm">Upgrade plan</a>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <label className="form-control">
          <span className="label-text">Employee</span>
          <select className="select select-bordered select-sm min-w-56" value={employeeId}
            onChange={(e) => { setEmployeeId(e.target.value); setError(null) }}>
            <option value="">Select an employee…</option>
            {employees.data?.map((e: Employee) => <option key={e.id} value={e.id}>{e.fullName} — {e.jobTitle}</option>)}
          </select>
        </label>
        {employeeId && (
          <button className="btn btn-primary btn-sm gap-1" onClick={() => { setError(null); setAdding(true) }}>
            <Plus className="h-4 w-4" /> Give an award
          </button>
        )}
      </div>

      {error && <div role="alert" className="alert alert-error text-sm"><span>{error}</span></div>}

      {!employeeId ? (
        <div className="card border border-base-300 bg-base-100">
          <div className="card-body items-center gap-2 py-10 text-center text-base-content/50">
            <AwardIcon className="h-8 w-8" /><p className="text-sm">Pick an employee to see and record their awards.</p>
          </div>
        </div>
      ) : awards.isError ? (
        <div className="card border border-base-300 bg-base-100">
          <div className="card-body items-center text-center">
            <AlertCircle className="h-6 w-6 text-error" />
            <p className="text-sm text-base-content/60">Couldn’t load awards. Try again.</p>
          </div>
        </div>
      ) : awards.isLoading ? (
        <div className="grid gap-3 sm:grid-cols-2">
          {Array.from({ length: 2 }).map((_, i) => <div key={i} className="h-24 animate-pulse rounded-box bg-base-200" />)}
        </div>
      ) : awards.data && awards.data.length > 0 ? (
        <div className="grid gap-3 sm:grid-cols-2">
          {awards.data.map((a: Award) => (
            <div key={a.id} className="card border border-base-300 bg-base-100">
              <div className="card-body gap-2 p-4">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <h3 className="font-semibold">{a.title}</h3>
                    <span className={`badge badge-sm ${CATEGORY_BADGE[a.category]}`}>{categoryLabel(a.category)}</span>
                  </div>
                  {confirmingId === a.id ? (
                    <div className="flex gap-1">
                      <button className="btn btn-error btn-xs" disabled={remove.isPending} onClick={() => remove.mutate(a.id)}>
                        {remove.isPending && <Loader2 className="h-3 w-3 animate-spin" />} Confirm
                      </button>
                      <button className="btn btn-ghost btn-xs" onClick={() => setConfirmingId(null)}>Cancel</button>
                    </div>
                  ) : (
                    <button className="btn btn-ghost btn-xs text-error" onClick={() => { setError(null); setConfirmingId(a.id) }}>
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  )}
                </div>
                {a.description && <p className="text-sm text-base-content/70">{a.description}</p>}
                <p className="text-xs text-base-content/50">{a.awardedOn}{a.awardedBy ? ` · by ${a.awardedBy}` : ''}</p>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="card border border-base-300 bg-base-100">
          <div className="card-body items-center gap-2 py-10 text-center text-base-content/50">
            <AwardIcon className="h-8 w-8" /><p className="text-sm">No awards yet — recognise a job well done.</p>
          </div>
        </div>
      )}

      {adding && <AwardModal employeeId={employeeId} onClose={() => setAdding(false)} onSaved={() => { invalidate(); setAdding(false) }} />}
    </div>
  )
}

function AwardModal({ employeeId, onClose, onSaved }: { employeeId: string; onClose: () => void; onSaved: () => void }) {
  const [title, setTitle] = useState('')
  const [category, setCategory] = useState<AwardCategory>('PERFORMANCE')
  const [awardedOn, setAwardedOn] = useState(() => new Date().toISOString().slice(0, 10))
  const [description, setDescription] = useState('')
  const [awardedBy, setAwardedBy] = useState('')
  const [error, setError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: () => {
      const payload: AwardPayload = {
        employeeId, title: title.trim(), category, awardedOn,
        description: description.trim() || undefined,
        awardedBy: awardedBy.trim() || undefined,
      }
      return adminAwardApi.create(payload)
    },
    onSuccess: onSaved,
    onError: (err) => setError(errorMessage(err, 'Could not record the award.')),
  })

  const invalid = title.trim() === '' || awardedOn === '' || save.isPending

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">Give an award</h3>
        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}

        <label className="form-control mt-2">
          <span className="label-text">Title</span>
          <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Employee of the Month" className="input input-bordered" />
        </label>

        <div className="mt-2 grid grid-cols-2 gap-3">
          <label className="form-control">
            <span className="label-text">Category</span>
            <select value={category} onChange={(e) => setCategory(e.target.value as AwardCategory)} className="select select-bordered">
              {CATEGORIES.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
            </select>
          </label>
          <label className="form-control">
            <span className="label-text">Date</span>
            <input type="date" value={awardedOn} onChange={(e) => setAwardedOn(e.target.value)} className="input input-bordered" />
          </label>
        </div>

        <label className="form-control mt-2">
          <span className="label-text">Description</span>
          <textarea value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Optional" className="textarea textarea-bordered" rows={2} />
        </label>

        <label className="form-control mt-2">
          <span className="label-text">Awarded by</span>
          <input value={awardedBy} onChange={(e) => setAwardedBy(e.target.value)} placeholder="e.g. Store Manager (optional)" className="input input-bordered" />
        </label>

        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={invalid} onClick={() => save.mutate()}>
            {save.isPending && <Loader2 className="h-4 w-4 animate-spin" />} Record award
          </button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}
