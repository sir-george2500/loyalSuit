import type { Metadata } from 'next'
import PlansView from './PlansView'

export const metadata: Metadata = { title: 'Subscription Plans — Admin' }

export default function PlansPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Subscription Plans</h1>
        <p className="text-sm text-base-content/60">Define the plans tenants can subscribe to</p>
      </div>
      <PlansView />
    </div>
  )
}
