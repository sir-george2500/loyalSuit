import type { Metadata } from 'next'
import AuditView from './AuditView'

export const metadata: Metadata = { title: 'Audit Log — Admin' }

export default function AuditPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Audit Log</h1>
        <p className="text-sm text-base-content/60">
          A record of security and account activity in your store
        </p>
      </div>
      <AuditView />
    </div>
  )
}
