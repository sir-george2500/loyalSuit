import type { Metadata } from 'next'
import CouponsView from './CouponsView'

export const metadata: Metadata = { title: 'Coupons — Admin' }

export default function CouponsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Coupons</h1>
        <p className="text-sm text-base-content/60">Discount codes customers can apply at checkout</p>
      </div>
      <CouponsView />
    </div>
  )
}
