import type { Metadata } from 'next'
import CustomersView from './CustomersView'

export const metadata: Metadata = { title: 'Customers — Admin' }

export default function CustomersPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Customers</h1>
        <p className="text-sm text-base-content/60">Everyone who has shopped with your store and what they’ve spent</p>
      </div>
      <CustomersView />
    </div>
  )
}
