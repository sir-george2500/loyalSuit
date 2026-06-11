import type { Metadata } from 'next'
import AwardsView from './AwardsView'

export const metadata: Metadata = { title: 'Awards — Admin' }

export default function AwardsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Awards</h1>
        <p className="text-sm text-base-content/60">Recognise employees and keep a record on their profile</p>
      </div>
      <AwardsView />
    </div>
  )
}
