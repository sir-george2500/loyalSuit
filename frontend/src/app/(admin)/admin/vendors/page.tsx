import type { Metadata } from 'next'
import VendorsView from './VendorsView'

export const metadata: Metadata = { title: 'Vendors — Admin' }

export default function VendorsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Vendors</h1>
        <p className="text-sm text-base-content/60">Review applications and manage your marketplace sellers</p>
      </div>
      <VendorsView />
    </div>
  )
}
