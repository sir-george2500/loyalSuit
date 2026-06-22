import type { Metadata } from 'next'
import InvoicesView from './InvoicesView'

export const metadata: Metadata = { title: 'Invoices — Admin' }

export default function InvoicesPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Invoices</h1>
        <p className="text-sm text-base-content/60">Printable invoices generated from your orders</p>
      </div>
      <InvoicesView />
    </div>
  )
}
