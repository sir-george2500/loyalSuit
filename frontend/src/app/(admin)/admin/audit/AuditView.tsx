'use client'

import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { ScrollText, ShieldAlert, ChevronLeft, ChevronRight, AlertCircle } from 'lucide-react'
import { auditApi } from '@/lib/api/audit'
import type { AuditAction, AuditLog } from '@/types'

const ACTION_LABELS: Record<AuditAction, string> = {
  USER_REGISTERED: 'User registered',
  LOGIN_SUCCEEDED: 'Login succeeded',
  LOGIN_FAILED: 'Login failed',
  PASSWORD_CHANGED: 'Password changed',
  PASSWORD_RESET_REQUESTED: 'Reset requested',
  PASSWORD_RESET_COMPLETED: 'Reset completed',
  TENANT_ONBOARDED: 'Store onboarded',
}

const ACTIONS = Object.keys(ACTION_LABELS) as AuditAction[]

const dateFmt = new Intl.DateTimeFormat('en-US', {
  month: 'short',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

export default function AuditView() {
  const [page, setPage] = useState(0)
  const [action, setAction] = useState<AuditAction | ''>('')

  const { data, isLoading, isError, error, isFetching } = useQuery({
    queryKey: ['audit', page, action],
    queryFn: async () => (await auditApi.list({ page, action })).data.data,
    placeholderData: keepPreviousData,
  })

  const status = (error as AxiosError | undefined)?.response?.status
  if (isError && status === 403) {
    return (
      <EmptyCard
        icon={<ShieldAlert className="h-6 w-6" />}
        title="Not authorized"
        body="Only store owners and admins can view the audit log."
        tone="warning"
      />
    )
  }
  if (isError) {
    return (
      <EmptyCard
        icon={<AlertCircle className="h-6 w-6" />}
        title="Couldn’t load the audit log"
        body="Something went wrong fetching activity. Try again in a moment."
        tone="error"
      />
    )
  }

  const onFilterChange = (value: AuditAction | '') => {
    setAction(value)
    setPage(0)
  }

  return (
    <div className="space-y-4">
      {/* Filter */}
      <div className="flex items-center gap-3">
        <label className="text-sm text-base-content/60" htmlFor="action-filter">
          Filter
        </label>
        <select
          id="action-filter"
          value={action}
          onChange={(e) => onFilterChange(e.target.value as AuditAction | '')}
          className="select select-bordered select-sm"
        >
          <option value="">All activity</option>
          {ACTIONS.map((a) => (
            <option key={a} value={a}>
              {ACTION_LABELS[a]}
            </option>
          ))}
        </select>
        {isFetching && <span className="loading loading-spinner loading-xs text-primary" />}
      </div>

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr>
              <th>Time</th>
              <th>Action</th>
              <th>Outcome</th>
              <th>Actor</th>
              <th>Source IP</th>
              <th>Detail</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 8 }).map((_, i) => (
                <tr key={i}>
                  <td colSpan={6}>
                    <div className="h-4 w-full animate-pulse rounded bg-base-200" />
                  </td>
                </tr>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((row) => <AuditRow key={row.id} row={row} />)
            ) : (
              <tr>
                <td colSpan={6}>
                  <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                    <ScrollText className="h-8 w-8" />
                    <p className="text-sm">No activity recorded yet.</p>
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-base-content/60">
          <span>
            Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} event
            {data.totalElements === 1 ? '' : 's'}
          </span>
          <div className="join">
            <button
              className="btn btn-sm join-item"
              onClick={() => setPage((p) => Math.max(p - 1, 0))}
              disabled={data.first}
            >
              <ChevronLeft className="h-4 w-4" /> Prev
            </button>
            <button
              className="btn btn-sm join-item"
              onClick={() => setPage((p) => p + 1)}
              disabled={data.last}
            >
              Next <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

function AuditRow({ row }: { row: AuditLog }) {
  return (
    <tr>
      <td className="whitespace-nowrap text-base-content/70">{dateFmt.format(new Date(row.occurredAt))}</td>
      <td>{ACTION_LABELS[row.action] ?? row.action}</td>
      <td>
        <span
          className={`badge badge-sm ${row.outcome === 'SUCCESS' ? 'badge-success' : 'badge-error'} badge-outline`}
        >
          {row.outcome === 'SUCCESS' ? 'Success' : 'Failure'}
        </span>
      </td>
      <td>
        <div className="flex flex-col">
          <span className="truncate">{row.actorEmail ?? '—'}</span>
          {row.actorRole && (
            <span className="text-xs text-base-content/40">{row.actorRole}</span>
          )}
        </div>
      </td>
      <td className="whitespace-nowrap font-mono text-xs text-base-content/60">{row.ipAddress ?? '—'}</td>
      <td className="max-w-xs truncate text-base-content/70" title={row.detail ?? ''}>
        {row.detail ?? '—'}
      </td>
    </tr>
  )
}

function EmptyCard({
  icon,
  title,
  body,
  tone,
}: {
  icon: React.ReactNode
  title: string
  body: string
  tone: 'warning' | 'error'
}) {
  const toneClass = tone === 'warning' ? 'bg-warning/10 text-warning' : 'bg-error/10 text-error'
  return (
    <div className="card border border-base-300 bg-base-100">
      <div className="card-body items-center text-center">
        <div className={`flex h-12 w-12 items-center justify-center rounded-full ${toneClass}`}>
          {icon}
        </div>
        <h2 className="text-lg font-semibold">{title}</h2>
        <p className="max-w-sm text-sm text-base-content/60">{body}</p>
      </div>
    </div>
  )
}
