import type { Metadata } from 'next'
import TenantsView from './TenantsView'

export const metadata: Metadata = { title: 'Tenants — Admin' }

export default function TenantsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Tenants</h1>
        <p className="text-sm text-base-content/60">Every store running on the platform</p>
      </div>
      <TenantsView />
    </div>
  )
}
