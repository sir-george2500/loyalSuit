import type { Metadata } from 'next'
import DashboardView from './DashboardView'

export const metadata: Metadata = { title: 'Dashboard — Admin' }

export default function AdminDashboardPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Dashboard</h1>
        <p className="text-sm text-base-content/60">Your store&apos;s performance at a glance</p>
      </div>
      <DashboardView />
    </div>
  )
}
