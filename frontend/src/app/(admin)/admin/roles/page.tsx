import type { Metadata } from 'next'
import RolesView from './RolesView'

export const metadata: Metadata = { title: 'Roles & Permissions — Admin' }

export default function RolesPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Roles &amp; Permissions</h1>
        <p className="text-sm text-base-content/60">What each role can do across the platform</p>
      </div>
      <RolesView />
    </div>
  )
}
