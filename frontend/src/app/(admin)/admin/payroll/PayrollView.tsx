'use client'

import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import {
  Banknote, AlertCircle, Loader2, Plus, ChevronLeft, ChevronRight, ArrowLeft, Lock, Check, Pencil,
} from 'lucide-react'
import { adminPayrollApi, type PayRunCreatePayload, type PayslipAdjustPayload } from '@/lib/api/payroll'
import type { PayRunStatus, Payslip } from '@/types'

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

function statusOf(err: unknown): number | undefined {
  return (err as AxiosError)?.response?.status
}

function money(n: number): string {
  return n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const STATUS_BADGE: Record<PayRunStatus, string> = {
  DRAFT: 'badge-warning', FINALISED: 'badge-info', PAID: 'badge-success',
}

export default function PayrollView() {
  const [selected, setSelected] = useState<string | null>(null)

  if (selected) {
    return <RunDetail runId={selected} onBack={() => setSelected(null)} />
  }
  return <RunsList onOpen={setSelected} />
}

/* ------------------------------- Runs ------------------------------- */

function RunsList({ onOpen }: { onOpen: (id: string) => void }) {
  const [page, setPage] = useState(0)
  const [creating, setCreating] = useState(false)

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['pay-runs', page],
    queryFn: async () => (await adminPayrollApi.listRuns(page)).data.data,
    placeholderData: keepPreviousData,
    retry: (count, err) => statusOf(err) !== 403 && count < 2,
  })

  if (isError && statusOf(error) === 403) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center gap-3 text-center">
          <Lock className="h-7 w-7 text-base-content/50" />
          <h2 className="card-title">Payroll is a Professional feature</h2>
          <p className="max-w-md text-sm text-base-content/60">{errorMessage(error, 'Upgrade your plan to run payroll and produce payslips.')}</p>
          <a href="/admin/settings" className="btn btn-primary btn-sm">Upgrade plan</a>
        </div>
      </div>
    )
  }

  if (isError) {
    return <ErrorCard message="Couldn’t load pay runs. Try again." />
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <button className="btn btn-primary btn-sm gap-1" onClick={() => setCreating(true)}>
          <Plus className="h-4 w-4" /> New pay run
        </button>
      </div>

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr><th>Run</th><th>Period</th><th>Status</th><th className="text-center">Payslips</th><th className="text-right">Total net</th><th /></tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i}><td colSpan={6}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((r) => (
                <tr key={r.id} className="cursor-pointer hover" onClick={() => onOpen(r.id)}>
                  <td className="font-medium">{r.label}</td>
                  <td className="whitespace-nowrap">{r.periodStart} → {r.periodEnd}</td>
                  <td><span className={`badge badge-sm ${STATUS_BADGE[r.status]}`}>{r.status}</span></td>
                  <td className="text-center">{r.payslipCount}</td>
                  <td className="text-right font-mono">{money(r.totalNet)}</td>
                  <td className="text-right"><ChevronRight className="h-4 w-4 text-base-content/40" /></td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={6}>
                <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                  <Banknote className="h-8 w-8" /><p className="text-sm">No pay runs yet.</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-base-content/60">
          <span>Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} run{data.totalElements === 1 ? '' : 's'}</span>
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

      {creating && <NewRunModal onClose={() => setCreating(false)} onSaved={(id) => { setCreating(false); onOpen(id) }} />}
    </div>
  )
}

function NewRunModal({ onClose, onSaved }: { onClose: () => void; onSaved: (id: string) => void }) {
  const queryClient = useQueryClient()
  const [periodStart, setPeriodStart] = useState('')
  const [periodEnd, setPeriodEnd] = useState('')
  const [label, setLabel] = useState('')
  const [error, setError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: () => {
      const payload: PayRunCreatePayload = { periodStart, periodEnd, label: label.trim() || undefined }
      return adminPayrollApi.generate(payload)
    },
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['pay-runs'] })
      onSaved(res.data.data.run.id)
    },
    onError: (err) => setError(errorMessage(err, 'Could not generate the pay run.')),
  })

  const invalid = !periodStart || !periodEnd || periodEnd < periodStart || save.isPending

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">New pay run</h3>
        <p className="mt-1 text-sm text-base-content/60">Generates a draft payslip for every active employee over the period.</p>
        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}

        <div className="mt-2 grid grid-cols-2 gap-3">
          <label className="form-control">
            <span className="label-text">Period start</span>
            <input type="date" value={periodStart} onChange={(e) => setPeriodStart(e.target.value)} className="input input-bordered" />
          </label>
          <label className="form-control">
            <span className="label-text">Period end</span>
            <input type="date" value={periodEnd} onChange={(e) => setPeriodEnd(e.target.value)} className="input input-bordered" />
          </label>
        </div>
        <label className="form-control mt-2">
          <span className="label-text">Label</span>
          <input value={label} onChange={(e) => setLabel(e.target.value)} placeholder="Defaults to the month (e.g. June 2026)" className="input input-bordered" />
        </label>

        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={invalid} onClick={() => save.mutate()}>
            {save.isPending && <Loader2 className="h-4 w-4 animate-spin" />} Generate
          </button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}

/* ------------------------------ Detail ------------------------------ */

function RunDetail({ runId, onBack }: { runId: string; onBack: () => void }) {
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState<Payslip | null>(null)
  const [error, setError] = useState<string | null>(null)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['pay-run', runId],
    queryFn: async () => (await adminPayrollApi.getRun(runId)).data.data,
  })

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['pay-run', runId] })
    queryClient.invalidateQueries({ queryKey: ['pay-runs'] })
  }

  const transition = useMutation({
    mutationFn: ({ op }: { op: 'finalise' | 'pay' }) => adminPayrollApi[op](runId),
    onSuccess: () => { invalidate(); setError(null) },
    onError: (err) => setError(errorMessage(err, 'Could not update the run.')),
  })

  const run = data?.run
  const isDraft = run?.status === 'DRAFT'

  return (
    <div className="space-y-4">
      <button className="btn btn-ghost btn-sm gap-1" onClick={onBack}><ArrowLeft className="h-4 w-4" /> All runs</button>

      {isError ? (
        <ErrorCard message="Couldn’t load this pay run. Try again." />
      ) : isLoading || !run ? (
        <div className="h-24 animate-pulse rounded-box bg-base-200" />
      ) : (
        <>
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-box border border-base-300 bg-base-100 p-4">
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-bold">{run.label}</h2>
                <span className={`badge badge-sm ${STATUS_BADGE[run.status]}`}>{run.status}</span>
              </div>
              <p className="text-sm text-base-content/60">{run.periodStart} → {run.periodEnd} · {run.payslipCount} payslip{run.payslipCount === 1 ? '' : 's'} · total net {money(run.totalNet)}</p>
            </div>
            <div className="flex gap-2">
              {run.status === 'DRAFT' && (
                <button className="btn btn-primary btn-sm gap-1" disabled={transition.isPending}
                  onClick={() => transition.mutate({ op: 'finalise' })}>
                  <Lock className="h-4 w-4" /> Finalise
                </button>
              )}
              {run.status === 'FINALISED' && (
                <button className="btn btn-success btn-sm gap-1" disabled={transition.isPending}
                  onClick={() => transition.mutate({ op: 'pay' })}>
                  <Check className="h-4 w-4" /> Mark paid
                </button>
              )}
            </div>
          </div>

          {error && <div role="alert" className="alert alert-error text-sm"><span>{error}</span></div>}

          <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
            <table className="table table-sm">
              <thead>
                <tr>
                  <th>Employee</th><th className="text-right">Base</th><th className="text-center">Unpaid days</th>
                  <th className="text-right">Leave ded.</th><th className="text-right">Other ded.</th>
                  <th className="text-right">Net pay</th><th />
                </tr>
              </thead>
              <tbody>
                {data.payslips.length > 0 ? (
                  data.payslips.map((p) => (
                    <tr key={p.id}>
                      <td className="font-medium">{p.employeeName}</td>
                      <td className="text-right font-mono">{money(p.baseSalary)}</td>
                      <td className="text-center">{p.unpaidLeaveDays}</td>
                      <td className="text-right font-mono">{money(p.unpaidLeaveDeduction)}</td>
                      <td className="text-right font-mono">{money(p.otherDeductions)}</td>
                      <td className="text-right font-mono font-bold">{money(p.netPay)}</td>
                      <td className="text-right">
                        {isDraft && (
                          <button className="btn btn-ghost btn-xs gap-1" onClick={() => { setError(null); setEditing(p) }}>
                            <Pencil className="h-3.5 w-3.5" /> Adjust
                          </button>
                        )}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr><td colSpan={7}>
                    <div className="py-10 text-center text-sm text-base-content/50">No payslips — there were no active employees when this run was generated.</div>
                  </td></tr>
                )}
              </tbody>
            </table>
          </div>
        </>
      )}

      {editing && <AdjustModal payslip={editing} onClose={() => setEditing(null)} onSaved={() => { invalidate(); setEditing(null) }} />}
    </div>
  )
}

function AdjustModal({ payslip, onClose, onSaved }: { payslip: Payslip; onClose: () => void; onSaved: () => void }) {
  const [otherDeductions, setOtherDeductions] = useState(String(payslip.otherDeductions))
  const [note, setNote] = useState(payslip.note ?? '')
  const [error, setError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: () => {
      const payload: PayslipAdjustPayload = { otherDeductions: Number(otherDeductions), note: note.trim() || undefined }
      return adminPayrollApi.adjustPayslip(payslip.id, payload)
    },
    onSuccess: onSaved,
    onError: (err) => setError(errorMessage(err, 'Could not adjust the payslip.')),
  })

  const amount = Number(otherDeductions)
  const invalid = otherDeductions === '' || Number.isNaN(amount) || amount < 0 || save.isPending
  const projectedNet = Math.max(payslip.baseSalary - payslip.unpaidLeaveDeduction - (Number.isNaN(amount) ? 0 : amount), 0)

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">Adjust {payslip.employeeName}</h3>
        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}

        <label className="form-control mt-2">
          <span className="label-text">Other deductions</span>
          <input type="number" min={0} step="0.01" value={otherDeductions} onChange={(e) => setOtherDeductions(e.target.value)} className="input input-bordered" />
        </label>
        <label className="form-control mt-2">
          <span className="label-text">Note</span>
          <input value={note} onChange={(e) => setNote(e.target.value)} placeholder="Optional" className="input input-bordered" />
        </label>

        <p className="mt-3 text-sm text-base-content/60">Projected net pay: <span className="font-mono font-bold text-base-content">{money(projectedNet)}</span></p>

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
