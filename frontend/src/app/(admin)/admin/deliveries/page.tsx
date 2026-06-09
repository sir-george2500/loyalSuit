import type { Metadata } from 'next'
import DeliveriesView from './DeliveriesView'

export const metadata: Metadata = { title: 'Deliveries — Admin' }

export default function DeliveriesPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Deliveries</h1>
        <p className="text-sm text-base-content/60">Assign orders to couriers and track them to the door</p>
      </div>
      <DeliveriesView />
    </div>
  )
}
