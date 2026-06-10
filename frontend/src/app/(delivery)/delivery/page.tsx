import type { Metadata } from 'next'
import DeliveryQueue from './DeliveryQueue'

export const metadata: Metadata = { title: 'My Deliveries' }

export default function DeliveryPage() {
  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-bold">My deliveries</h1>
        <p className="text-sm text-gray-400">Your active runs — update each as you go</p>
      </div>
      <DeliveryQueue />
    </div>
  )
}
