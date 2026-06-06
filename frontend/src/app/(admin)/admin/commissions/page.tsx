import type { Metadata } from 'next'
import CommissionsView from './CommissionsView'

export const metadata: Metadata = { title: 'Commissions — Admin' }

export default function CommissionsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Commissions</h1>
        <p className="text-sm text-base-content/60">The platform’s commission ledger across all vendors</p>
      </div>
      <CommissionsView />
    </div>
  )
}
