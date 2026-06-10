import type { Metadata } from 'next'
import AffiliatesView from './AffiliatesView'

export const metadata: Metadata = { title: 'Affiliates — Admin' }

export default function AffiliatesPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Affiliates</h1>
        <p className="text-sm text-base-content/60">Referral codes that reward users for orders they bring in</p>
      </div>
      <AffiliatesView />
    </div>
  )
}
