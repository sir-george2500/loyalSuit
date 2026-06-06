import type { Metadata } from 'next'
import SellerProductsView from './SellerProductsView'

export const metadata: Metadata = { title: 'My products — Seller' }

export default function SellerProductsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">My products</h1>
        <p className="text-sm text-base-content/60">Create and manage the products you sell</p>
      </div>
      <SellerProductsView />
    </div>
  )
}
