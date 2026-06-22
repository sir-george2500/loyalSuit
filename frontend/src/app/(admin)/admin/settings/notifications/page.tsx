import type { Metadata } from 'next'
import NotificationsView from './NotificationsView'

export const metadata: Metadata = { title: 'Notifications — Admin' }

export default function NotificationsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Notifications</h1>
        <p className="text-sm text-base-content/60">Choose which emails and alerts your store sends</p>
      </div>
      <NotificationsView />
    </div>
  )
}
