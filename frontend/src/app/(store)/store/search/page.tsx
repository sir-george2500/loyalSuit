import type { Metadata } from 'next'
import Link from 'next/link'
import { Package, SearchX, ChevronLeft, ChevronRight } from 'lucide-react'
import { getMarketplace, searchMarketplace } from '@/lib/api/marketplace'
import MarketplaceSearchBar from '@/components/store/MarketplaceSearchBar'
import MarketplaceCard from '@/components/store/MarketplaceCard'

export const metadata: Metadata = { title: 'Search — LoyalSuit' }

type Search = { q?: string; page?: string }

export default async function MarketplaceSearchPage({
  searchParams,
}: {
  searchParams: Promise<Search>
}) {
  const { q, page } = await searchParams
  const query = (q ?? '').trim()
  const pageIndex = Math.max(0, Number(page ?? 0) || 0)

  const store = await getMarketplace()
  const results = query && store ? await searchMarketplace(query, pageIndex) : null

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-8 max-w-2xl">
        <MarketplaceSearchBar defaultValue={query} />
      </div>

      {!query ? (
        <EmptyState
          icon={<Package className="h-10 w-10" />}
          title="Search LoyalSuit"
          subtitle="Find products from LoyalSuit and all our sellers."
        />
      ) : results && store && results.content.length > 0 ? (
        <>
          <p className="mb-6 text-sm text-gray-500">
            {results.totalElements} {results.totalElements === 1 ? 'result' : 'results'} for{' '}
            <span className="font-medium text-gray-900">“{query}”</span>
          </p>

          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
            {results.content.map((p) => (
              <MarketplaceCard key={p.productId} product={p} storeSlug={store.slug} currency={store.currency} />
            ))}
          </div>

          <div className="mt-8 flex items-center justify-between text-sm text-gray-500">
            <span>
              Page {results.page + 1} of {Math.max(results.totalPages, 1)}
            </span>
            <div className="flex gap-2">
              {!results.first && (
                <Link
                  href={pageHref(query, results.page - 1)}
                  className="inline-flex items-center gap-1 rounded border border-gray-300 px-3 py-1.5 hover:bg-gray-50"
                >
                  <ChevronLeft className="h-4 w-4" /> Prev
                </Link>
              )}
              {!results.last && (
                <Link
                  href={pageHref(query, results.page + 1)}
                  className="inline-flex items-center gap-1 rounded border border-gray-300 px-3 py-1.5 hover:bg-gray-50"
                >
                  Next <ChevronRight className="h-4 w-4" />
                </Link>
              )}
            </div>
          </div>
        </>
      ) : (
        <EmptyState
          icon={<SearchX className="h-10 w-10" />}
          title={`No products match “${query}”`}
          subtitle="Try a different word."
        />
      )}
    </div>
  )
}

function pageHref(query: string, page: number): string {
  const params = new URLSearchParams({ q: query })
  if (page > 0) params.set('page', String(page))
  return `/store/search?${params.toString()}`
}

function EmptyState({
  icon,
  title,
  subtitle,
}: {
  icon: React.ReactNode
  title: string
  subtitle: string
}) {
  return (
    <div className="flex flex-col items-center gap-2 py-20 text-center text-gray-400">
      {icon}
      <p className="text-lg text-gray-600">{title}</p>
      <p className="text-sm">{subtitle}</p>
    </div>
  )
}
