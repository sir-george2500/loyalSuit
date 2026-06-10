import type { Metadata } from 'next'
import AffiliateDashboard from './AffiliateDashboard'

export const metadata: Metadata = { title: 'Affiliate dashboard' }

export default function AffiliatePage() {
  return <AffiliateDashboard />
}
