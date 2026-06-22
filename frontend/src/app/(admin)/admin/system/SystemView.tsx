'use client'

import { useQuery } from '@tanstack/react-query'
import { Activity, AlertCircle, Database, Cpu, MemoryStick, Clock, CheckCircle2, XCircle } from 'lucide-react'
import { systemHealthApi } from '@/lib/api/platform'

const COUNT_LABEL: Record<string, string> = {
  tenants: 'Tenants',
  app_users: 'Users',
  products: 'Products',
  orders: 'Orders',
  vendors: 'Vendors',
  reviews: 'Reviews',
}

function uptime(ms: number): string {
  const s = Math.floor(ms / 1000)
  const d = Math.floor(s / 86400)
  const h = Math.floor((s % 86400) / 3600)
  const m = Math.floor((s % 3600) / 60)
  if (d > 0) return `${d}d ${h}h`
  if (h > 0) return `${h}h ${m}m`
  return `${m}m`
}

export default function SystemView() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['system-health'],
    queryFn: async () => (await systemHealthApi.get()).data.data,
    refetchInterval: 15000,
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load system health. Try again.</p>
        </div>
      </div>
    )
  }

  if (isLoading || !data) {
    return (
      <div className="grid gap-4 md:grid-cols-2">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="card border border-base-300 bg-base-100"><div className="card-body"><div className="h-20 animate-pulse rounded bg-base-200" /></div></div>
        ))}
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={<Database className="h-5 w-5" />} label="Database">
          {data.databaseUp ? (
            <span className="flex items-center gap-1 text-success"><CheckCircle2 className="h-5 w-5" /> Online</span>
          ) : (
            <span className="flex items-center gap-1 text-error"><XCircle className="h-5 w-5" /> Down</span>
          )}
        </StatCard>
        <StatCard icon={<Clock className="h-5 w-5" />} label="Uptime">
          <span className="text-xl font-bold">{uptime(data.jvm.uptimeMs)}</span>
        </StatCard>
        <StatCard icon={<MemoryStick className="h-5 w-5" />} label="Memory">
          <span className="text-xl font-bold">{data.jvm.memoryUsedMb}<span className="text-sm font-normal text-base-content/50"> / {data.jvm.memoryMaxMb} MB</span></span>
        </StatCard>
        <StatCard icon={<Cpu className="h-5 w-5" />} label="Runtime">
          <span className="text-sm font-medium">Java {data.jvm.javaVersion}</span>
          <span className="block text-xs text-base-content/50">{data.jvm.availableProcessors} vCPU</span>
        </StatCard>
      </div>

      <div className="card border border-base-300 bg-base-100">
        <div className="card-body">
          <h2 className="flex items-center gap-2 text-base font-semibold"><Activity className="h-5 w-5 text-primary" /> Record counts</h2>
          <div className="mt-2 grid gap-4 sm:grid-cols-3 lg:grid-cols-6">
            {Object.entries(data.counts).map(([key, value]) => (
              <div key={key} className="rounded-box border border-base-300 p-3 text-center">
                <div className="text-2xl font-bold">{value.toLocaleString()}</div>
                <div className="text-xs text-base-content/60">{COUNT_LABEL[key] ?? key}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

function StatCard({ icon, label, children }: { icon: React.ReactNode; label: string; children: React.ReactNode }) {
  return (
    <div className="card border border-base-300 bg-base-100">
      <div className="card-body gap-1 p-4">
        <div className="flex items-center gap-2 text-base-content/60">{icon}<span className="text-sm">{label}</span></div>
        <div className="text-lg">{children}</div>
      </div>
    </div>
  )
}
