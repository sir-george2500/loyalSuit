'use client'

import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { UserCog, AlertCircle, ChevronLeft, ChevronRight, Ban, RotateCcw } from 'lucide-react'
import { staffApi } from '@/lib/api/staff'
import type { StaffMember, UserRole } from '@/types'

/** Roles a store owner can assign to back-office staff. */
const ASSIGNABLE_ROLES: { value: UserRole; label: string }[] = [
  { value: 'TENANT_ADMIN', label: 'Administrator' },
  { value: 'STAFF', label: 'Staff' },
]

const ROLE_LABEL: Record<string, string> = {
  SUPER_ADMIN: 'Super Admin',
  TENANT_ADMIN: 'Administrator',
  STAFF: 'Staff',
  VENDOR: 'Vendor',
  CUSTOMER: 'Customer',
  DELIVERY_AGENT: 'Delivery Agent',
}

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

function date(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

export default function StaffView() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [error, setError] = useState<string | null>(null)

  const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['staff', page],
    queryFn: async () => (await staffApi.list({ page })).data.data,
    placeholderData: keepPreviousData,
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['staff'] })

  const changeRole = useMutation({
    mutationFn: ({ id, role }: { id: string; role: UserRole }) => staffApi.changeRole(id, role),
    onSuccess: () => { invalidate(); setError(null) },
    onError: (err) => setError(errorMessage(err, 'Could not change the role.')),
  })

  const toggleActive = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      active ? staffApi.deactivate(id) : staffApi.activate(id),
    onSuccess: () => { invalidate(); setError(null) },
    onError: (err) => setError(errorMessage(err, 'Could not update the staff member.')),
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load staff. Try again.</p>
        </div>
      </div>
    )
  }

  const canEdit = (m: StaffMember) => m.role !== 'SUPER_ADMIN'

  return (
    <div className="space-y-4">
      {isFetching && <span className="loading loading-spinner loading-xs text-primary" />}
      {error && <div role="alert" className="alert alert-error text-sm"><span>{error}</span></div>}

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr><th>Member</th><th>Role</th><th>Status</th><th>Joined</th><th className="text-right">Actions</th></tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i}><td colSpan={5}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((m) => (
                <tr key={m.id}>
                  <td>
                    <div className="font-medium">{m.fullName}</div>
                    <div className="text-xs text-base-content/50">{m.email}</div>
                  </td>
                  <td>
                    {canEdit(m) ? (
                      <select
                        className="select select-bordered select-xs"
                        value={ASSIGNABLE_ROLES.some((r) => r.value === m.role) ? m.role : ''}
                        disabled={changeRole.isPending}
                        onChange={(e) => { setError(null); changeRole.mutate({ id: m.id, role: e.target.value as UserRole }) }}
                      >
                        {!ASSIGNABLE_ROLES.some((r) => r.value === m.role) && (
                          <option value="" disabled>{ROLE_LABEL[m.role] ?? m.role}</option>
                        )}
                        {ASSIGNABLE_ROLES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
                      </select>
                    ) : (
                      <span className="badge badge-neutral badge-sm">{ROLE_LABEL[m.role] ?? m.role}</span>
                    )}
                  </td>
                  <td>
                    <span className={`badge badge-sm ${m.active ? 'badge-success' : 'badge-ghost'}`}>
                      {m.active ? 'Active' : 'Disabled'}
                    </span>
                  </td>
                  <td className="text-sm text-base-content/60">{date(m.joinedAt)}</td>
                  <td>
                    <div className="flex justify-end">
                      {canEdit(m) && (
                        m.active ? (
                          <button className="btn btn-ghost btn-xs gap-1 text-error" disabled={toggleActive.isPending}
                            onClick={() => { setError(null); toggleActive.mutate({ id: m.id, active: true }) }}>
                            <Ban className="h-3.5 w-3.5" /> Disable
                          </button>
                        ) : (
                          <button className="btn btn-ghost btn-xs gap-1" disabled={toggleActive.isPending}
                            onClick={() => { setError(null); toggleActive.mutate({ id: m.id, active: false }) }}>
                            <RotateCcw className="h-3.5 w-3.5" /> Enable
                          </button>
                        )
                      )}
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={5}>
                <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                  <UserCog className="h-8 w-8" /><p className="text-sm">No staff members yet.</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-base-content/60">
          <span>Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} member{data.totalElements === 1 ? '' : 's'}</span>
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
    </div>
  )
}
