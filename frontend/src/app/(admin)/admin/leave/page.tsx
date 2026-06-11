import type { Metadata } from 'next'
import LeaveView from './LeaveView'

export const metadata: Metadata = { title: 'Leave — Admin' }

export default function LeavePage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Leave</h1>
        <p className="text-sm text-base-content/60">Leave types, requests, and per-employee balances</p>
      </div>
      <LeaveView />
    </div>
  )
}
