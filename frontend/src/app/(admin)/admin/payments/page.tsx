import type { Metadata } from 'next'
import PaymentsView from './PaymentsView'

export const metadata: Metadata = { title: 'Payments — Admin' }

export default function PaymentsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Payments</h1>
        <p className="text-sm text-base-content/60">Every payment captured against an order</p>
      </div>
      <PaymentsView />
    </div>
  )
}
