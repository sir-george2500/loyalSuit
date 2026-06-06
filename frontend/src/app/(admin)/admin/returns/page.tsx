import type { Metadata } from 'next'
import ReturnsView from './ReturnsView'

export const metadata: Metadata = { title: 'Returns — Admin' }

export default function ReturnsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Returns</h1>
        <p className="text-sm text-base-content/60">Review return requests — approving refunds and restocks the order</p>
      </div>
      <ReturnsView />
    </div>
  )
}
