import type { Metadata } from 'next'
import TrackOrder from '@/components/store/TrackOrder'

export const metadata: Metadata = { title: 'Track your order' }

export default async function TrackPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params
  return <TrackOrder slug={slug} />
}
