import type { Metadata } from 'next'
import OrdersView from './OrdersView'

export const metadata: Metadata = { title: 'Orders — Admin' }

export default function OrdersPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Orders</h1>
        <p className="text-sm text-base-content/60">Fulfil orders and record cash payments</p>
      </div>
      <OrdersView />
    </div>
  )
}
