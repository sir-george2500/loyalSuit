import type { Metadata } from 'next'

export const metadata: Metadata = { title: 'Seller Dashboard' }

export default function SellerDashboardPage() {
  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Your Store</h1>
      <p className="text-gray-500">Manage your products, orders, and payouts.</p>
    </div>
  )
}
