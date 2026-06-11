'use client'

import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import {
  CalendarOff, AlertCircle, Loader2, Plus, Check, X, Pencil, ChevronLeft, ChevronRight, Lock,
} from 'lucide-react'
import { adminEmployeeApi } from '@/lib/api/employees'
import { adminLeaveApi, adminLeaveTypeApi, type LeaveRequestPayload, type LeaveTypePayload } from '@/lib/api/leave'
import type { Employee, LeaveBalance, LeaveStatus, LeaveType } from '@/types'

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

function statusOf(err: unknown): number | undefined {
  return (err as AxiosError)?.response?.status
}

type Tab = 'requests' | 'types' | 'balances'

const STATUS_FILTERS: { label: string; value: LeaveStatus | 'ALL' }[] = [
  { label: 'All', value: 'ALL' },
  { label: 'Pending', value: 'PENDING' },
  { label: 'Approved', value: 'APPROVED' },
  { label: 'Rejected', value: 'REJECTED' },
]

const STATUS_BADGE: Record<LeaveStatus, string> = {
  PENDING: 'badge-warning', APPROVED: 'badge-success', REJECTED: 'badge-neutral',
}

export default function LeaveView() {
  const [tab, setTab] = useState<Tab>('requests')

  // Leave types drive the plan gate (loaded on every tab) and feed the request modal.
  const types = useQuery({
    queryKey: ['leave-types'],
    queryFn: async () => (await adminLeaveTypeApi.list()).data.data,
    retry: (count, err) => statusOf(err) !== 403 && count < 2,
  })

  if (types.isError && statusOf(types.error) === 403) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center gap-3 text-center">
          <Lock className="h-7 w-7 text-base-content/50" />
          <h2 className="card-title">Leave is a Professional feature</h2>
          <p className="max-w-md text-sm text-base-content/60">{errorMessage(types.error, 'Upgrade your plan to manage leave types, requests, and balances.')}</p>
          <a href="/admin/settings" className="btn btn-primary btn-sm">Upgrade plan</a>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div role="tablist" className="tabs tabs-boxed w-fit">
        <button role="tab" className={`tab ${tab === 'requests' ? 'tab-active' : ''}`} onClick={() => setTab('requests')}>Requests</button>
        <button role="tab" className={`tab ${tab === 'types' ? 'tab-active' : ''}`} onClick={() => setTab('types')}>Leave types</button>
        <button role="tab" className={`tab ${tab === 'balances' ? 'tab-active' : ''}`} onClick={() => setTab('balances')}>Balances</button>
      </div>

      {tab === 'requests' && <RequestsTab types={types.data ?? []} />}
      {tab === 'types' && <TypesTab />}
      {tab === 'balances' && <BalancesTab />}
    </div>
  )
}

/* ----------------------------- Requests ----------------------------- */

function RequestsTab({ types }: { types: LeaveType[] }) {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [filter, setFilter] = useState<LeaveStatus | 'ALL'>('PENDING')
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['leave-requests', page, filter],
    queryFn: async () => (await adminLeaveApi.list(page, filter === 'ALL' ? undefined : filter)).data.data,
    placeholderData: keepPreviousData,
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['leave-requests'] })

  const decide = useMutation({
    mutationFn: ({ id, op }: { id: string; op: 'approve' | 'reject' }) => adminLeaveApi[op](id),
    onSuccess: () => { invalidate(); setError(null) },
    onError: (err) => setError(errorMessage(err, 'Could not update the request.')),
  })

  if (isError) {
    return <ErrorCard message="Couldn’t load leave requests. Try again." />
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div role="tablist" className="tabs tabs-boxed tabs-sm">
          {STATUS_FILTERS.map((f) => (
            <button key={f.value} role="tab" className={`tab ${filter === f.value ? 'tab-active' : ''}`}
              onClick={() => { setFilter(f.value); setPage(0) }}>
              {f.label}
            </button>
          ))}
        </div>
        <button className="btn btn-primary btn-sm gap-1" onClick={() => { setError(null); setCreating(true) }}>
          <Plus className="h-4 w-4" /> New request
        </button>
      </div>

      {error && <div role="alert" className="alert alert-error text-sm"><span>{error}</span></div>}

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr><th>Type</th><th>Dates</th><th className="text-center">Days</th><th>Reason</th><th>Status</th><th className="text-right">Actions</th></tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i}><td colSpan={6}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((r) => (
                <tr key={r.id}>
                  <td className="font-medium">{r.leaveTypeName}</td>
                  <td className="whitespace-nowrap">{r.startDate} → {r.endDate}</td>
                  <td className="text-center">{r.days}</td>
                  <td className="max-w-xs truncate text-base-content/60">{r.reason ?? ''}</td>
                  <td><span className={`badge badge-sm ${STATUS_BADGE[r.status]}`}>{r.status}</span></td>
                  <td className="text-right">
                    {r.status === 'PENDING' ? (
                      <div className="flex justify-end gap-1">
                        <button className="btn btn-ghost btn-xs gap-1 text-success" disabled={decide.isPending}
                          onClick={() => decide.mutate({ id: r.id, op: 'approve' })}>
                          <Check className="h-3.5 w-3.5" /> Approve
                        </button>
                        <button className="btn btn-ghost btn-xs gap-1 text-error" disabled={decide.isPending}
                          onClick={() => decide.mutate({ id: r.id, op: 'reject' })}>
                          <X className="h-3.5 w-3.5" /> Reject
                        </button>
                      </div>
                    ) : (
                      <span className="text-xs text-base-content/40">{r.decisionNote ?? '—'}</span>
                    )}
                  </td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={6}>
                <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                  <CalendarOff className="h-8 w-8" /><p className="text-sm">No leave requests here.</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      <Pager data={data} onPrev={() => setPage((p) => Math.max(p - 1, 0))} onNext={() => setPage((p) => p + 1)} noun="request" />

      {creating && <RequestModal types={types} onClose={() => setCreating(false)} onSaved={() => { invalidate(); setCreating(false) }} />}
    </div>
  )
}

function RequestModal({ types, onClose, onSaved }: { types: LeaveType[]; onClose: () => void; onSaved: () => void }) {
  const [employeeId, setEmployeeId] = useState('')
  const [leaveTypeId, setLeaveTypeId] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)

  const employees = useQuery({
    queryKey: ['employees', 'picker'],
    queryFn: async () => (await adminEmployeeApi.list(0, 'ACTIVE', 100)).data.data.content,
  })

  const activeTypes = types.filter((t) => t.active)

  const save = useMutation({
    mutationFn: () => {
      const payload: LeaveRequestPayload = { employeeId, leaveTypeId, startDate, endDate, reason: reason.trim() || undefined }
      return adminLeaveApi.create(payload)
    },
    onSuccess: onSaved,
    onError: (err) => setError(errorMessage(err, 'Could not raise the request.')),
  })

  const invalid = !employeeId || !leaveTypeId || !startDate || !endDate || endDate < startDate || save.isPending

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">New leave request</h3>
        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}

        <label className="form-control mt-2">
          <span className="label-text">Employee</span>
          <select className="select select-bordered" value={employeeId} onChange={(e) => setEmployeeId(e.target.value)}>
            <option value="">Select an employee…</option>
            {employees.data?.map((e: Employee) => <option key={e.id} value={e.id}>{e.fullName} — {e.jobTitle}</option>)}
          </select>
        </label>

        <label className="form-control mt-2">
          <span className="label-text">Leave type</span>
          <select className="select select-bordered" value={leaveTypeId} onChange={(e) => setLeaveTypeId(e.target.value)}>
            <option value="">Select a type…</option>
            {activeTypes.map((t) => <option key={t.id} value={t.id}>{t.name} ({t.annualAllowanceDays} days/yr)</option>)}
          </select>
        </label>

        <div className="mt-2 grid grid-cols-2 gap-3">
          <label className="form-control">
            <span className="label-text">From</span>
            <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} className="input input-bordered" />
          </label>
          <label className="form-control">
            <span className="label-text">To</span>
            <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} className="input input-bordered" />
          </label>
        </div>

        <label className="form-control mt-2">
          <span className="label-text">Reason</span>
          <input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Optional" className="input input-bordered" />
        </label>

        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={invalid} onClick={() => save.mutate()}>
            {save.isPending && <Loader2 className="h-4 w-4 animate-spin" />} Raise request
          </button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}

/* ------------------------------- Types ------------------------------ */

function TypesTab() {
  const queryClient = useQueryClient()
  const [adding, setAdding] = useState(false)
  const [editing, setEditing] = useState<LeaveType | null>(null)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['leave-types'],
    queryFn: async () => (await adminLeaveTypeApi.list()).data.data,
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['leave-types'] })

  if (isError) return <ErrorCard message="Couldn’t load leave types. Try again." />

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <button className="btn btn-primary btn-sm gap-1" onClick={() => setAdding(true)}>
          <Plus className="h-4 w-4" /> Add type
        </button>
      </div>

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr><th>Name</th><th className="text-center">Allowance</th><th>Paid</th><th>Status</th><th className="text-right">Actions</th></tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 3 }).map((_, i) => (
                <tr key={i}><td colSpan={5}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
              ))
            ) : data && data.length > 0 ? (
              data.map((t) => (
                <tr key={t.id}>
                  <td className="font-medium">{t.name}</td>
                  <td className="text-center">{t.annualAllowanceDays} days/yr</td>
                  <td>{t.paid ? 'Paid' : 'Unpaid'}</td>
                  <td><span className={`badge badge-sm ${t.active ? 'badge-success' : 'badge-neutral'}`}>{t.active ? 'Active' : 'Inactive'}</span></td>
                  <td className="text-right">
                    <button className="btn btn-ghost btn-xs gap-1" onClick={() => setEditing(t)}>
                      <Pencil className="h-3.5 w-3.5" /> Edit
                    </button>
                  </td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={5}>
                <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                  <CalendarOff className="h-8 w-8" /><p className="text-sm">No leave types yet.</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {adding && <TypeModal onClose={() => setAdding(false)} onSaved={() => { invalidate(); setAdding(false) }} />}
      {editing && <TypeModal type={editing} onClose={() => setEditing(null)} onSaved={() => { invalidate(); setEditing(null) }} />}
    </div>
  )
}

function TypeModal({ type, onClose, onSaved }: { type?: LeaveType; onClose: () => void; onSaved: () => void }) {
  const isEdit = !!type
  const [name, setName] = useState(type?.name ?? '')
  const [days, setDays] = useState(type ? String(type.annualAllowanceDays) : '')
  const [paid, setPaid] = useState(type?.paid ?? true)
  const [active, setActive] = useState(type?.active ?? true)
  const [error, setError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: () => {
      const payload: LeaveTypePayload = { name: name.trim(), annualAllowanceDays: Number(days), paid, active }
      return isEdit ? adminLeaveTypeApi.update(type!.id, payload) : adminLeaveTypeApi.create(payload)
    },
    onSuccess: onSaved,
    onError: (err) => setError(errorMessage(err, 'Could not save the leave type.')),
  })

  const invalid = name.trim() === '' || days === '' || Number.isNaN(Number(days)) || Number(days) < 0 || save.isPending

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">{isEdit ? `Edit ${type!.name}` : 'Add a leave type'}</h3>
        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}

        <div className="mt-2 grid grid-cols-2 gap-3">
          <label className="form-control">
            <span className="label-text">Name</span>
            <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Annual" className="input input-bordered" />
          </label>
          <label className="form-control">
            <span className="label-text">Allowance (days/yr)</span>
            <input type="number" min={0} max={366} value={days} onChange={(e) => setDays(e.target.value)} className="input input-bordered" />
          </label>
        </div>

        <div className="mt-3 flex gap-6">
          <label className="label cursor-pointer gap-2">
            <input type="checkbox" className="checkbox checkbox-sm" checked={paid} onChange={(e) => setPaid(e.target.checked)} />
            <span className="label-text">Paid leave</span>
          </label>
          <label className="label cursor-pointer gap-2">
            <input type="checkbox" className="checkbox checkbox-sm" checked={active} onChange={(e) => setActive(e.target.checked)} />
            <span className="label-text">Active</span>
          </label>
        </div>

        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={invalid} onClick={() => save.mutate()}>
            {save.isPending && <Loader2 className="h-4 w-4 animate-spin" />} {isEdit ? 'Save' : 'Add'}
          </button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}

/* ----------------------------- Balances ----------------------------- */

function BalancesTab() {
  const [employeeId, setEmployeeId] = useState('')

  const employees = useQuery({
    queryKey: ['employees', 'picker'],
    queryFn: async () => (await adminEmployeeApi.list(0, 'ACTIVE', 100)).data.data.content,
  })

  const balances = useQuery({
    queryKey: ['leave-balances', employeeId],
    queryFn: async () => (await adminLeaveApi.balances(employeeId)).data.data,
    enabled: employeeId !== '',
  })

  return (
    <div className="space-y-4">
      <label className="form-control w-fit">
        <span className="label-text">Employee</span>
        <select className="select select-bordered select-sm min-w-56" value={employeeId} onChange={(e) => setEmployeeId(e.target.value)}>
          <option value="">Select an employee…</option>
          {employees.data?.map((e: Employee) => <option key={e.id} value={e.id}>{e.fullName} — {e.jobTitle}</option>)}
        </select>
      </label>

      {!employeeId ? (
        <div className="card border border-base-300 bg-base-100">
          <div className="card-body items-center gap-2 py-10 text-center text-base-content/50">
            <CalendarOff className="h-8 w-8" /><p className="text-sm">Pick an employee to see their leave balances.</p>
          </div>
        </div>
      ) : balances.isError ? (
        <ErrorCard message="Couldn’t load balances. Try again." />
      ) : (
        <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
          <table className="table table-sm">
            <thead>
              <tr><th>Type</th><th className="text-center">Allowance</th><th className="text-center">Taken</th><th className="text-center">Remaining</th></tr>
            </thead>
            <tbody>
              {balances.isLoading ? (
                Array.from({ length: 3 }).map((_, i) => (
                  <tr key={i}><td colSpan={4}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
                ))
              ) : balances.data && balances.data.length > 0 ? (
                balances.data.map((b: LeaveBalance) => (
                  <tr key={b.leaveTypeId}>
                    <td className="font-medium">{b.leaveTypeName}</td>
                    <td className="text-center">{b.allowanceDays}</td>
                    <td className="text-center">{b.takenDays}</td>
                    <td className={`text-center font-bold ${b.remainingDays <= 0 ? 'text-error' : 'text-success'}`}>{b.remainingDays}</td>
                  </tr>
                ))
              ) : (
                <tr><td colSpan={4}>
                  <div className="py-10 text-center text-sm text-base-content/50">No active leave types to show.</div>
                </td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

/* ------------------------------ Shared ------------------------------ */

function ErrorCard({ message }: { message: string }) {
  return (
    <div className="card border border-base-300 bg-base-100">
      <div className="card-body items-center text-center">
        <AlertCircle className="h-6 w-6 text-error" />
        <p className="text-sm text-base-content/60">{message}</p>
      </div>
    </div>
  )
}

function Pager({ data, onPrev, onNext, noun }: {
  data?: { page: number; totalPages: number; totalElements: number; first: boolean; last: boolean }
  onPrev: () => void
  onNext: () => void
  noun: string
}) {
  if (!data || data.totalElements === 0) return null
  return (
    <div className="flex items-center justify-between text-sm text-base-content/60">
      <span>Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} {noun}{data.totalElements === 1 ? '' : 's'}</span>
      <div className="join">
        <button className="btn btn-sm join-item" onClick={onPrev} disabled={data.first}>
          <ChevronLeft className="h-4 w-4" /> Prev
        </button>
        <button className="btn btn-sm join-item" onClick={onNext} disabled={data.last}>
          Next <ChevronRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  )
}
