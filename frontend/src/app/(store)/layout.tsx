import Link from 'next/link'
import { Store, User } from 'lucide-react'
import CookieConsent from '@/components/CookieConsent'
import MarketplaceSearchBar from '@/components/store/MarketplaceSearchBar'

export default function StoreLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-white">
      <header className="sticky top-0 z-50 bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center gap-4">
          <Link href="/store" className="shrink-0 text-xl font-bold text-primary-700">LoyalSuit</Link>
          <div className="hidden flex-1 justify-center sm:flex">
            <div className="w-full max-w-md">
              <MarketplaceSearchBar />
            </div>
          </div>
          <div className="ml-auto flex items-center gap-4 sm:ml-0">
            <Link href="/store" aria-label="All stores" className="text-gray-500 hover:text-gray-700">
              <Store className="w-5 h-5" />
            </Link>
            <Link href="/account" aria-label="Account" className="text-gray-500 hover:text-gray-700">
              <User className="w-5 h-5" />
            </Link>
          </div>
        </div>
      </header>
      <main>{children}</main>
      <CookieConsent />
    </div>
  )
}
