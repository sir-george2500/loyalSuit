'use client'

import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import {
  IdCard, AlertCircle, Loader2, UserPlus, Pencil, ChevronLeft, ChevronRight, Lock,
} from 'lucide-react'
import { adminEmployeeApi, type EmployeePayload } from '@/lib/api/employees'
import type { Employee, EmployeeStatus, EmploymentType } from '@/types'

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

function statusOf(err: unknown): number | undefined {
  return (err as AxiosError)?.response?.status
}

const STATUS_FILTERS: { label: string; value: EmployeeStatus | 'ALL' }[] = [
  { label: 'All', value: 'ALL' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'On leave', value: 'ON_LEAVE' },
  { label: 'Terminated', value: 'TERMINATED' },
]

const EMPLOYMENT_TYPES: { label: string; value: EmploymentType }[] = [
  { label: 'Full-time', value: 'FULL_TIME' },
  { label: 'Part-time', value: 'PART_TIME' },
  { label: 'Contract', value: 'CONTRACT' },
  { label: 'Intern', value: 'INTERN' },
]

const TYPE_LABEL: Record<EmploymentType, string> = {
  FULL_TIME: 'Full-time', PART_TIME: 'Part-time', CONTRACT: 'Contract', INTERN: 'Intern',
}

const STATUS_BADGE: Record<EmployeeStatus, string> = {
  ACTIVE: 'badge-success', ON_LEAVE: 'badge-warning', TERMINATED: 'badge-neutral',
}

const STATUS_LABEL: Record<EmployeeStatus, string> = {
  ACTIVE: 'Active', ON_LEAVE: 'On leave', TERMINATED: 'Terminated',
}

export default function EmployeesView() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [filter, setFilter] = useState<EmployeeStatus | 'ALL'>('ALL')
  const [adding, setAdding] = useState(false)
  const [editing, setEditing] = useState<Employee | null>(null)

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['employees', page, filter],
    queryFn: async () =>
      (await adminEmployeeApi.list(page, filter === 'ALL' ? undefined : filter)).data.data,
    placeholderData: keepPreviousData,
    retry: (count, err) => statusOf(err) !== 403 && count < 2,
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['employees'] })

  // A BASIC-plan tenant is blocked at the service with a 403 — surface the upgrade path, not an error.
  if (isError && statusOf(error) === 403) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center gap-3 text-center">
          <Lock className="h-7 w-7 text-base-content/50" />
          <h2 className="card-title">Employees is a Professional feature</h2>
          <p className="max-w-md text-sm text-base-content/60">{errorMessage(error, 'Upgrade your plan to manage your team roster, attendance, leave, and payroll.')}</p>
          <a href="/admin/settings" className="btn btn-primary btn-sm">Upgrade plan</a>
        </div>
      </div>
    )
  }

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load employees. Try again.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div role="tablist" className="tabs tabs-boxed tabs-sm">
          {STATUS_FILTERS.map((f) => (
            <button
              key={f.value}
              role="tab"
              className={`tab ${filter === f.value ? 'tab-active' : ''}`}
              onClick={() => { setFilter(f.value); setPage(0) }}
            >
              {f.label}
            </button>
          ))}
        </div>
        <button className="btn btn-primary btn-sm gap-1" onClick={() => setAdding(true)}>
          <UserPlus className="h-4 w-4" /> Add employee
        </button>
      </div>

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr>
              <th>Employee</th><th>Role</th><th>Type</th><th>Hired</th>
              <th className="text-right">Salary</th><th>Status</th><th className="text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i}><td colSpan={7}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((e) => (
                <tr key={e.id}>
                  <td>
                    <div className="font-medium">{e.fullName}</div>
                    <div className="text-xs text-base-content/50">{e.email ?? e.phone ?? ''}</div>
                  </td>
                  <td>
                    <div>{e.jobTitle}</div>
                    {e.department && <div className="text-xs text-base-content/50">{e.department}</div>}
                  </td>
                  <td>{TYPE_LABEL[e.employmentType]}</td>
                  <td className="whitespace-nowrap">{e.hireDate}</td>
                  <td className="text-right font-mono">{e.baseSalary.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                  <td><span className={`badge badge-sm ${STATUS_BADGE[e.status]}`}>{STATUS_LABEL[e.status]}</span></td>
                  <td className="text-right">
                    <button className="btn btn-ghost btn-xs gap-1" onClick={() => setEditing(e)}>
                      <Pencil className="h-3.5 w-3.5" /> Edit
                    </button>
                  </td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={7}>
                <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                  <IdCard className="h-8 w-8" /><p className="text-sm">No employees yet.</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-base-content/60">
          <span>Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} employee{data.totalElements === 1 ? '' : 's'}</span>
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

      {adding && <EmployeeModal onClose={() => setAdding(false)} onSaved={() => { invalidate(); setAdding(false) }} />}
      {editing && <EmployeeModal employee={editing} onClose={() => setEditing(null)} onSaved={() => { invalidate(); setEditing(null) }} />}
    </div>
  )
}

function EmployeeModal({ employee, onClose, onSaved }: { employee?: Employee; onClose: () => void; onSaved: () => void }) {
  const isEdit = !!employee
  const [fullName, setFullName] = useState(employee?.fullName ?? '')
  const [jobTitle, setJobTitle] = useState(employee?.jobTitle ?? '')
  const [department, setDepartment] = useState(employee?.department ?? '')
  const [email, setEmail] = useState(employee?.email ?? '')
  const [phone, setPhone] = useState(employee?.phone ?? '')
  const [employmentType, setEmploymentType] = useState<EmploymentType>(employee?.employmentType ?? 'FULL_TIME')
  const [status, setStatus] = useState<EmployeeStatus>(employee?.status ?? 'ACTIVE')
  const [hireDate, setHireDate] = useState(employee?.hireDate ?? '')
  const [baseSalary, setBaseSalary] = useState(employee ? String(employee.baseSalary) : '')
  const [error, setError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: () => {
      const payload: EmployeePayload = {
        fullName: fullName.trim(),
        jobTitle: jobTitle.trim(),
        department: department.trim() || undefined,
        email: email.trim() || undefined,
        phone: phone.trim() || undefined,
        employmentType,
        status,
        hireDate,
        baseSalary: Number(baseSalary),
      }
      return isEdit ? adminEmployeeApi.update(employee!.id, payload) : adminEmployeeApi.create(payload)
    },
    onSuccess: onSaved,
    onError: (err) => setError(errorMessage(err, 'Could not save the employee.')),
  })

  const salaryInvalid = Number.isNaN(Number(baseSalary)) || Number(baseSalary) < 0
  const invalid = fullName.trim() === '' || jobTitle.trim() === '' || hireDate === '' || baseSalary === '' || salaryInvalid || save.isPending

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">{isEdit ? `Edit ${employee!.fullName}` : 'Add an employee'}</h3>
        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}

        <label className="form-control mt-2">
          <span className="label-text">Full name</span>
          <input value={fullName} onChange={(e) => setFullName(e.target.value)} placeholder="Casey Clerk" className="input input-bordered" />
        </label>

        <div className="mt-2 grid grid-cols-2 gap-3">
          <label className="form-control">
            <span className="label-text">Job title</span>
            <input value={jobTitle} onChange={(e) => setJobTitle(e.target.value)} placeholder="Cashier" className="input input-bordered" />
          </label>
          <label className="form-control">
            <span className="label-text">Department</span>
            <input value={department} onChange={(e) => setDepartment(e.target.value)} placeholder="Sales" className="input input-bordered" />
          </label>
        </div>

        <div className="mt-2 grid grid-cols-2 gap-3">
          <label className="form-control">
            <span className="label-text">Email</span>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="casey@store.com" className="input input-bordered" />
          </label>
          <label className="form-control">
            <span className="label-text">Phone</span>
            <input value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="+231…" className="input input-bordered" />
          </label>
        </div>

        <div className="mt-2 grid grid-cols-2 gap-3">
          <label className="form-control">
            <span className="label-text">Employment type</span>
            <select value={employmentType} onChange={(e) => setEmploymentType(e.target.value as EmploymentType)} className="select select-bordered">
              {EMPLOYMENT_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
            </select>
          </label>
          <label className="form-control">
            <span className="label-text">Status</span>
            <select value={status} onChange={(e) => setStatus(e.target.value as EmployeeStatus)} className="select select-bordered">
              {STATUS_FILTERS.filter((f) => f.value !== 'ALL').map((s) => (
                <option key={s.value} value={s.value}>{s.label}</option>
              ))}
            </select>
          </label>
        </div>

        <div className="mt-2 grid grid-cols-2 gap-3">
          <label className="form-control">
            <span className="label-text">Hire date</span>
            <input type="date" value={hireDate} onChange={(e) => setHireDate(e.target.value)} className="input input-bordered" />
          </label>
          <label className="form-control">
            <span className="label-text">Base salary (monthly)</span>
            <input type="number" min={0} step="0.01" value={baseSalary} onChange={(e) => setBaseSalary(e.target.value)} className="input input-bordered" />
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
