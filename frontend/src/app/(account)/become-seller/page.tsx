import type { Metadata } from 'next'
import BecomeSeller from '@/components/account/BecomeSeller'

export const metadata: Metadata = { title: 'Become a seller' }

export default function BecomeSellerPage() {
  return <BecomeSeller />
}
