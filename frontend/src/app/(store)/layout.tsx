import Link from 'next/link'
import { User, Store } from 'lucide-react'
import CookieConsent from '@/components/CookieConsent'

export default function StoreLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-white">
      <header className="sticky top-0 z-50 bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <Link href="/store" className="text-xl font-bold text-primary-700">LoyalSuit</Link>
          <div className="flex items-center gap-5">
            <Link
              href="/become-seller"
              className="hidden items-center gap-1.5 text-sm font-medium text-gray-600 hover:text-primary-700 sm:inline-flex"
            >
              <Store className="h-4 w-4" /> Sell on LoyalSuit
            </Link>
            <Link href="/account" aria-label="Account" className="text-gray-500 hover:text-gray-700">
              <User className="h-5 w-5" />
            </Link>
          </div>
        </div>
      </header>
      <main>{children}</main>
      <CookieConsent />
    </div>
  )
}
