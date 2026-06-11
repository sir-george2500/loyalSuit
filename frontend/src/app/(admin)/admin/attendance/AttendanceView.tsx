'use client'

import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import {
  CalendarClock, AlertCircle, Loader2, LogIn, LogOut, Lock, ClipboardPen,
} from 'lucide-react'
import { adminEmployeeApi } from '@/lib/api/employees'
import { adminAttendanceApi, type AttendancePayload } from '@/lib/api/attendance'
import type { AttendanceStatus, Employee } from '@/types'

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

function statusOf(err: unknown): number | undefined {
  return (err as AxiosError)?.response?.status
}

/** Local YYYY-MM-DD without the UTC shift that toISOString() introduces. */
function ymd(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function time(iso?: string | null): string {
  return iso ? new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '—'
}

const STATUS_BADGE: Record<AttendanceStatus, string> = {
  PRESENT: 'badge-success', LATE: 'badge-warning', HALF_DAY: 'badge-info', ABSENT: 'badge-neutral',
}

const STATUS_OPTIONS: { label: string; value: AttendanceStatus }[] = [
  { label: 'Present', value: 'PRESENT' },
  { label: 'Late', value: 'LATE' },
  { label: 'Half day', value: 'HALF_DAY' },
  { label: 'Absent', value: 'ABSENT' },
]

export default function AttendanceView() {
  const queryClient = useQueryClient()
  const today = useMemo(() => new Date(), [])
  const [employeeId, setEmployeeId] = useState('')
  const [from, setFrom] = useState(() => ymd(new Date(today.getFullYear(), today.getMonth(), 1)))
  const [to, setTo] = useState(() => ymd(new Date(today.getFullYear(), today.getMonth() + 1, 0)))
  const [recording, setRecording] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)

  const employees = useQuery({
    queryKey: ['employees', 'picker'],
    queryFn: async () => (await adminEmployeeApi.list(0, 'ACTIVE', 100)).data.data.content,
    retry: (count, err) => statusOf(err) !== 403 && count < 2,
  })

  const sheet = useQuery({
    queryKey: ['timesheet', employeeId, from, to],
    queryFn: async () => (await adminAttendanceApi.timesheet(employeeId, from, to)).data.data,
    enabled: employeeId !== '' && from !== '' && to !== '',
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['timesheet'] })

  const clock = useMutation({
    mutationFn: ({ op }: { op: 'clockIn' | 'clockOut' }) => adminAttendanceApi[op](employeeId),
    onSuccess: () => { invalidate(); setActionError(null) },
    onError: (err) => setActionError(errorMessage(err, 'Could not update attendance.')),
  })

  // A BASIC-plan tenant is blocked at the service with a 403 — surface the upgrade path.
  if (employees.isError && statusOf(employees.error) === 403) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center gap-3 text-center">
          <Lock className="h-7 w-7 text-base-content/50" />
          <h2 className="card-title">Attendance is a Professional feature</h2>
          <p className="max-w-md text-sm text-base-content/60">{errorMessage(employees.error, 'Upgrade your plan to track attendance, leave, and payroll.')}</p>
          <a href="/admin/settings" className="btn btn-primary btn-sm">Upgrade plan</a>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end gap-3 rounded-box border border-base-300 bg-base-100 p-4">
        <label className="form-control">
          <span className="label-text">Employee</span>
          <select className="select select-bordered select-sm min-w-56" value={employeeId} onChange={(e) => setEmployeeId(e.target.value)}>
            <option value="">Select an employee…</option>
            {employees.data?.map((e: Employee) => (
              <option key={e.id} value={e.id}>{e.fullName} — {e.jobTitle}</option>
            ))}
          </select>
        </label>
        <label className="form-control">
          <span className="label-text">From</span>
          <input type="date" className="input input-bordered input-sm" value={from} onChange={(e) => setFrom(e.target.value)} />
        </label>
        <label className="form-control">
          <span className="label-text">To</span>
          <input type="date" className="input input-bordered input-sm" value={to} onChange={(e) => setTo(e.target.value)} />
        </label>

        <div className="ml-auto flex gap-2">
          <button className="btn btn-outline btn-sm gap-1" disabled={!employeeId || clock.isPending}
            onClick={() => clock.mutate({ op: 'clockIn' })}>
            <LogIn className="h-4 w-4" /> Clock in
          </button>
          <button className="btn btn-outline btn-sm gap-1" disabled={!employeeId || clock.isPending}
            onClick={() => clock.mutate({ op: 'clockOut' })}>
            <LogOut className="h-4 w-4" /> Clock out
          </button>
          <button className="btn btn-primary btn-sm gap-1" disabled={!employeeId}
            onClick={() => { setActionError(null); setRecording(true) }}>
            <ClipboardPen className="h-4 w-4" /> Record day
          </button>
        </div>
      </div>

      {actionError && <div role="alert" className="alert alert-error text-sm"><span>{actionError}</span></div>}

      {!employeeId ? (
        <div className="card border border-base-300 bg-base-100">
          <div className="card-body items-center gap-2 py-10 text-center text-base-content/50">
            <CalendarClock className="h-8 w-8" /><p className="text-sm">Pick an employee to see their timesheet.</p>
          </div>
        </div>
      ) : sheet.isError ? (
        <div className="card border border-base-300 bg-base-100">
          <div className="card-body items-center text-center">
            <AlertCircle className="h-6 w-6 text-error" />
            <p className="text-sm text-base-content/60">Couldn’t load the timesheet. Try again.</p>
          </div>
        </div>
      ) : (
        <>
          {sheet.data && (
            <div className="flex flex-wrap gap-4 text-sm">
              <div className="rounded-box border border-base-300 bg-base-100 px-4 py-2">
                <span className="text-base-content/60">Days present</span>
                <div className="text-xl font-bold">{sheet.data.daysPresent}</div>
              </div>
              <div className="rounded-box border border-base-300 bg-base-100 px-4 py-2">
                <span className="text-base-content/60">Total hours</span>
                <div className="text-xl font-bold">{sheet.data.totalHours.toFixed(2)}</div>
              </div>
            </div>
          )}

          <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
            <table className="table table-sm">
              <thead>
                <tr><th>Date</th><th>Check-in</th><th>Check-out</th><th className="text-right">Hours</th><th>Status</th><th>Note</th></tr>
              </thead>
              <tbody>
                {sheet.isLoading ? (
                  Array.from({ length: 5 }).map((_, i) => (
                    <tr key={i}><td colSpan={6}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
                  ))
                ) : sheet.data && sheet.data.entries.length > 0 ? (
                  sheet.data.entries.map((a) => (
                    <tr key={a.id}>
                      <td className="whitespace-nowrap font-medium">{a.workDate}</td>
                      <td>{time(a.checkIn)}</td>
                      <td>{time(a.checkOut)}</td>
                      <td className="text-right font-mono">{a.hoursWorked.toFixed(2)}</td>
                      <td><span className={`badge badge-sm ${STATUS_BADGE[a.status]}`}>{a.status.replace('_', ' ')}</span></td>
                      <td className="max-w-xs truncate text-base-content/60">{a.note ?? ''}</td>
                    </tr>
                  ))
                ) : (
                  <tr><td colSpan={6}>
                    <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                      <CalendarClock className="h-8 w-8" /><p className="text-sm">No attendance in this range.</p>
                    </div>
                  </td></tr>
                )}
              </tbody>
            </table>
          </div>
        </>
      )}

      {recording && (
        <RecordModal
          employeeId={employeeId}
          defaultDate={ymd(today)}
          onClose={() => setRecording(false)}
          onSaved={() => { invalidate(); setRecording(false) }}
        />
      )}
    </div>
  )
}

function RecordModal({ employeeId, defaultDate, onClose, onSaved }: {
  employeeId: string
  defaultDate: string
  onClose: () => void
  onSaved: () => void
}) {
  const [workDate, setWorkDate] = useState(defaultDate)
  const [status, setStatus] = useState<AttendanceStatus>('PRESENT')
  const [checkIn, setCheckIn] = useState('')
  const [checkOut, setCheckOut] = useState('')
  const [note, setNote] = useState('')
  const [error, setError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: () => {
      const payload: AttendancePayload = {
        employeeId,
        workDate,
        status,
        checkIn: checkIn ? new Date(checkIn).toISOString() : undefined,
        checkOut: checkOut ? new Date(checkOut).toISOString() : undefined,
        note: note.trim() || undefined,
      }
      return adminAttendanceApi.record(payload)
    },
    onSuccess: onSaved,
    onError: (err) => setError(errorMessage(err, 'Could not save attendance.')),
  })

  const invalid = workDate === '' || save.isPending

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">Record a day</h3>
        <p className="mt-1 text-sm text-base-content/60">Enter or correct attendance for this employee.</p>
        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}

        <div className="mt-2 grid grid-cols-2 gap-3">
          <label className="form-control">
            <span className="label-text">Date</span>
            <input type="date" value={workDate} onChange={(e) => setWorkDate(e.target.value)} className="input input-bordered" />
          </label>
          <label className="form-control">
            <span className="label-text">Status</span>
            <select value={status} onChange={(e) => setStatus(e.target.value as AttendanceStatus)} className="select select-bordered">
              {STATUS_OPTIONS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
            </select>
          </label>
        </div>

        <div className="mt-2 grid grid-cols-2 gap-3">
          <label className="form-control">
            <span className="label-text">Check-in</span>
            <input type="datetime-local" value={checkIn} onChange={(e) => setCheckIn(e.target.value)} className="input input-bordered" />
          </label>
          <label className="form-control">
            <span className="label-text">Check-out</span>
            <input type="datetime-local" value={checkOut} onChange={(e) => setCheckOut(e.target.value)} className="input input-bordered" />
          </label>
        </div>

        <label className="form-control mt-2">
          <span className="label-text">Note</span>
          <input value={note} onChange={(e) => setNote(e.target.value)} placeholder="Optional" className="input input-bordered" />
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
