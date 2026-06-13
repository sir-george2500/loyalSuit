import type { Metadata } from 'next'
import SellerDashboardView from './SellerDashboardView'

export const metadata: Metadata = { title: 'Seller Dashboard' }

export default function SellerDashboardPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Your Store</h1>
        <p className="text-sm text-base-content/60">
          Sell your products inside the Loyal Spare Parts marketplace, track orders, and request payouts.
        </p>
      </div>
      <SellerDashboardView />
    </div>
  )
}
