import type { Metadata } from 'next'
import SellerSettingsView from './SellerSettingsView'

export const metadata: Metadata = { title: 'Store settings — Seller' }

export default function SellerSettingsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Store settings</h1>
        <p className="text-sm text-base-content/60">Edit your storefront profile and logo</p>
      </div>
      <SellerSettingsView />
    </div>
  )
}
