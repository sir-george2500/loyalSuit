import type { Metadata } from 'next'
import SellerEarningsView from './SellerEarningsView'

export const metadata: Metadata = { title: 'Earnings — Seller' }

export default function SellerEarningsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Earnings</h1>
        <p className="text-sm text-base-content/60">Your commission balance and ledger</p>
      </div>
      <SellerEarningsView />
    </div>
  )
}
