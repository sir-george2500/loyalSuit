import type { Metadata } from 'next'
import SellerReviewsView from './SellerReviewsView'

export const metadata: Metadata = { title: 'Reviews — Seller' }

export default function SellerReviewsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Reviews</h1>
        <p className="text-sm text-base-content/60">What customers say about your products</p>
      </div>
      <SellerReviewsView />
    </div>
  )
}
