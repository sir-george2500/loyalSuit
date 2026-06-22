'use client'

import { useQuery } from '@tanstack/react-query'
import { ShieldCheck, AlertCircle, Check } from 'lucide-react'
import { roleApi } from '@/lib/api/staff'

export default function RolesView() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['roles'],
    queryFn: async () => (await roleApi.list()).data.data,
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load roles. Try again.</p>
        </div>
      </div>
    )
  }

  if (isLoading) {
    return (
      <div className="grid gap-4 md:grid-cols-2">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="card border border-base-300 bg-base-100">
            <div className="card-body"><div className="h-24 w-full animate-pulse rounded bg-base-200" /></div>
          </div>
        ))}
      </div>
    )
  }

  return (
    <div className="grid gap-4 md:grid-cols-2">
      {data?.map((r) => (
        <div key={r.role} className="card border border-base-300 bg-base-100">
          <div className="card-body">
            <div className="flex items-center gap-2">
              <ShieldCheck className="h-5 w-5 text-primary" />
              <h2 className="card-title text-base">{r.label}</h2>
            </div>
            <p className="text-sm text-base-content/60">{r.description}</p>
            <ul className="mt-2 space-y-1">
              {r.permissions.map((p) => (
                <li key={p} className="flex items-start gap-2 text-sm">
                  <Check className="mt-0.5 h-4 w-4 shrink-0 text-success" />
                  <span>{p}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      ))}
    </div>
  )
}
