import type { Metadata } from 'next'
import CartContents from '@/components/store/CartContents'

export const metadata: Metadata = { title: 'Your cart' }

export default async function CartPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params
  return <CartContents slug={slug} />
}
