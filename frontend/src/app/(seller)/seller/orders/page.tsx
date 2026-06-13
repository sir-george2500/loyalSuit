import type { Metadata } from 'next'
import SellerOrdersView from './SellerOrdersView'

export const metadata: Metadata = { title: 'Orders — Seller' }

export default function SellerOrdersPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Orders</h1>
        <p className="text-sm text-base-content/60">Orders that include your products — showing your items and earnings.</p>
      </div>
      <SellerOrdersView />
    </div>
  )
}
