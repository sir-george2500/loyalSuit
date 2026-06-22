import type { Metadata } from 'next'
import SystemView from './SystemView'

export const metadata: Metadata = { title: 'System Health — Admin' }

export default function SystemPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">System Health</h1>
        <p className="text-sm text-base-content/60">Live status of the platform’s database and runtime</p>
      </div>
      <SystemView />
    </div>
  )
}
