import type { Metadata } from 'next'
import CheckoutForm from '@/components/store/CheckoutForm'

export const metadata: Metadata = { title: 'Checkout' }

export default async function CheckoutPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params
  return <CheckoutForm slug={slug} />
}
