import type { Metadata } from 'next'
import NotificationsInbox from './NotificationsInbox'

export const metadata: Metadata = { title: 'Notifications' }

export default function NotificationsPage() {
  return <NotificationsInbox />
}
