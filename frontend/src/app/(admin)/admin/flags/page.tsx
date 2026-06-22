import type { Metadata } from 'next'
import FlagsView from './FlagsView'

export const metadata: Metadata = { title: 'Feature Flags — Admin' }

export default function FlagsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Feature Flags</h1>
        <p className="text-sm text-base-content/60">Toggle platform capabilities without a deploy</p>
      </div>
      <FlagsView />
    </div>
  )
}
