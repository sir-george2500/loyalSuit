import type { Metadata } from 'next'
import Link from 'next/link'
import Image from 'next/image'
import { Store as StoreIcon, Package, ArrowRight } from 'lucide-react'
import { getStores } from '@/lib/api/storefront'
import type { StoreSummary } from '@/types'

export const metadata: Metadata = {
  title: 'Marketplace — LoyalSuit',
  description: 'Browse every store on LoyalSuit and shop their products.',
}

export default async function MarketplacePage() {
  const stores = (await getStores()) ?? []

  return (
    <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
      <header className="mb-10 text-center">
        <h1 className="text-3xl font-bold text-gray-900 sm:text-4xl">Marketplace</h1>
        <p className="mt-3 text-lg text-gray-500">
          Browse every store on LoyalSuit and shop their products — no account needed.
        </p>
      </header>

      {stores.length > 0 ? (
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {stores.map((store) => (
            <StoreCard key={store.slug} store={store} />
          ))}
        </div>
      ) : (
        <div className="flex flex-col items-center gap-3 py-24 text-center text-gray-400">
          <StoreIcon className="h-12 w-12" />
          <p className="text-lg">No stores are open yet.</p>
          <p className="text-sm">Check back soon — new sellers are joining all the time.</p>
        </div>
      )}
    </div>
  )
}

function StoreCard({ store }: { store: StoreSummary }) {
  return (
    <Link
      href={`/store/${store.slug}`}
      className="group flex items-center gap-4 rounded-2xl border border-gray-200 bg-white p-5 shadow-sm transition-all hover:border-primary-300 hover:shadow-md"
    >
      <div className="relative h-16 w-16 shrink-0 overflow-hidden rounded-xl bg-gray-100">
        {store.logoUrl ? (
          <Image src={store.logoUrl} alt={store.name} fill sizes="64px" className="object-cover" />
        ) : (
          <div className="flex h-full items-center justify-center text-gray-300">
            <StoreIcon className="h-7 w-7" />
          </div>
        )}
      </div>

      <div className="min-w-0 flex-1">
        <h2 className="truncate text-lg font-semibold text-gray-900">{store.name}</h2>
        <p className="mt-0.5 flex items-center gap-1.5 text-sm text-gray-500">
          <Package className="h-3.5 w-3.5" />
          {store.productCount} {store.productCount === 1 ? 'product' : 'products'}
          {store.country && <span className="text-gray-300">·</span>}
          {store.country && <span>{store.country}</span>}
        </p>
      </div>

      <ArrowRight className="h-5 w-5 shrink-0 text-gray-300 transition-colors group-hover:text-primary-600" />
    </Link>
  )
}
