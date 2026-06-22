import type { Metadata } from 'next'
import StaffView from './StaffView'

export const metadata: Metadata = { title: 'Staff & Roles — Admin' }

export default function StaffPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Staff &amp; Roles</h1>
        <p className="text-sm text-base-content/60">Manage who can access the back office and what they can do</p>
      </div>
      <StaffView />
    </div>
  )
}
