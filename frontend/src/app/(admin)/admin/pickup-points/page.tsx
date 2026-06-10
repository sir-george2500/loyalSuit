import type { Metadata } from 'next'
import PickupPointsView from './PickupPointsView'

export const metadata: Metadata = { title: 'Pickup Points — Admin' }

export default function PickupPointsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Pickup Points</h1>
        <p className="text-sm text-base-content/60">Each pickup point sets its own delivery zones and fees</p>
      </div>
      <PickupPointsView />
    </div>
  )
}
