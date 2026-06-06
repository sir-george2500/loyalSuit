import type { Metadata } from 'next'
import PayoutsView from './PayoutsView'

export const metadata: Metadata = { title: 'Payouts — Admin' }

export default function PayoutsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Payouts</h1>
        <p className="text-sm text-base-content/60">Review and disburse vendor payout requests</p>
      </div>
      <PayoutsView />
    </div>
  )
}
