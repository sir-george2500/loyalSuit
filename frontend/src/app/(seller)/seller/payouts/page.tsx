import type { Metadata } from 'next'
import SellerPayoutsView from './SellerPayoutsView'

export const metadata: Metadata = { title: 'Payouts — Seller' }

export default function SellerPayoutsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Payouts</h1>
        <p className="text-sm text-base-content/60">Withdraw your settled earnings</p>
      </div>
      <SellerPayoutsView />
    </div>
  )
}
