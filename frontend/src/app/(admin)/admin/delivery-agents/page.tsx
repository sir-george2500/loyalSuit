import type { Metadata } from 'next'
import DeliveryAgentsView from './DeliveryAgentsView'

export const metadata: Metadata = { title: 'Delivery Agents — Admin' }

export default function DeliveryAgentsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Delivery Agents</h1>
        <p className="text-sm text-base-content/60">Onboard couriers and manage your delivery roster</p>
      </div>
      <DeliveryAgentsView />
    </div>
  )
}
