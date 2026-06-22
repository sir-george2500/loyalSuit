'use client'

import { useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { Building2, AlertCircle, ChevronLeft, ChevronRight, Ban, RotateCcw } from 'lucide-react'
import { platformTenantApi } from '@/lib/api/platform'
import type { SubscriptionPlan, TenantAdmin } from '@/types'

const PLANS: SubscriptionPlan[] = ['BASIC', 'PROFESSIONAL', 'ENTERPRISE']

const PLAN_LABEL: Record<SubscriptionPlan, string> = {
  BASIC: 'Basic',
  PROFESSIONAL: 'Professional',
  ENTERPRISE: 'Enterprise',
}

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

function date(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

export default function TenantsView() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [error, setError] = useState<string | null>(null)

  const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['platform-tenants', page],
    queryFn: async () => (await platformTenantApi.list({ page })).data.data,
    placeholderData: keepPreviousData,
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['platform-tenants'] })

  const toggleActive = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      active ? platformTenantApi.deactivate(id) : platformTenantApi.activate(id),
    onSuccess: () => { invalidate(); setError(null) },
    onError: (err) => setError(errorMessage(err, 'Could not update the tenant.')),
  })

  const changePlan = useMutation({
    mutationFn: ({ id, plan }: { id: string; plan: SubscriptionPlan }) => platformTenantApi.changePlan(id, plan),
    onSuccess: () => { invalidate(); setError(null) },
    onError: (err) => setError(errorMessage(err, 'Could not change the plan.')),
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load tenants. Try again.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {isFetching && <span className="loading loading-spinner loading-xs text-primary" />}
      {error && <div role="alert" className="alert alert-error text-sm"><span>{error}</span></div>}

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr><th>Store</th><th>Plan</th><th>Status</th><th>Created</th><th className="text-right">Actions</th></tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i}><td colSpan={5}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((t: TenantAdmin) => (
                <tr key={t.id}>
                  <td>
                    <div className="font-medium">{t.name}</div>
                    <div className="font-mono text-xs text-base-content/50">{t.slug}</div>
                  </td>
                  <td>
                    <select
                      className="select select-bordered select-xs"
                      value={t.subscriptionPlan}
                      disabled={changePlan.isPending}
                      onChange={(e) => { setError(null); changePlan.mutate({ id: t.id, plan: e.target.value as SubscriptionPlan }) }}
                    >
                      {PLANS.map((p) => <option key={p} value={p}>{PLAN_LABEL[p]}</option>)}
                    </select>
                  </td>
                  <td>
                    <span className={`badge badge-sm ${t.active ? 'badge-success' : 'badge-ghost'}`}>
                      {t.active ? 'Active' : 'Suspended'}
                    </span>
                  </td>
                  <td className="text-sm text-base-content/60">{date(t.createdAt)}</td>
                  <td>
                    <div className="flex justify-end">
                      {t.active ? (
                        <button className="btn btn-ghost btn-xs gap-1 text-error" disabled={toggleActive.isPending}
                          onClick={() => { setError(null); toggleActive.mutate({ id: t.id, active: true }) }}>
                          <Ban className="h-3.5 w-3.5" /> Suspend
                        </button>
                      ) : (
                        <button className="btn btn-ghost btn-xs gap-1" disabled={toggleActive.isPending}
                          onClick={() => { setError(null); toggleActive.mutate({ id: t.id, active: false }) }}>
                          <RotateCcw className="h-3.5 w-3.5" /> Reactivate
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={5}>
                <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                  <Building2 className="h-8 w-8" /><p className="text-sm">No tenants yet.</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-base-content/60">
          <span>Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} tenant{data.totalElements === 1 ? '' : 's'}</span>
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
