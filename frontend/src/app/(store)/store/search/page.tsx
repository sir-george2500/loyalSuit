import type { Metadata } from 'next'
import Link from 'next/link'
import Image from 'next/image'
import { Package, SearchX, ChevronLeft, ChevronRight } from 'lucide-react'
import { searchProducts } from '@/lib/api/storefront'
import MarketplaceSearchBar from '@/components/store/MarketplaceSearchBar'
import type { MarketplaceProductResult } from '@/types'

export const metadata: Metadata = { title: 'Search — LoyalSuit Marketplace' }

type Search = { q?: string; page?: string }

export default async function MarketplaceSearchPage({
  searchParams,
}: {
  searchParams: Promise<Search>
}) {
  const { q, page } = await searchParams
  const query = (q ?? '').trim()
  const pageIndex = Math.max(0, Number(page ?? 0) || 0)

  const results = query ? await searchProducts(query, pageIndex) : null

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-8 max-w-2xl">
        <MarketplaceSearchBar defaultValue={query} />
      </div>

      {!query ? (
        <EmptyState
          icon={<Package className="h-10 w-10" />}
          title="Search the marketplace"
          subtitle="Find products from every store in one place."
        />
      ) : results && results.content.length > 0 ? (
        <>
          <p className="mb-6 text-sm text-gray-500">
            {results.totalElements} {results.totalElements === 1 ? 'result' : 'results'} for{' '}
            <span className="font-medium text-gray-900">“{query}”</span>
          </p>

          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
            {results.content.map((p) => (
              <ResultCard key={p.productId} product={p} />
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
          subtitle="Try a different word, or browse stores from the marketplace."
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

function ResultCard({ product }: { product: MarketplaceProductResult }) {
  const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: product.currency || 'USD' })
  return (
    <Link href={`/store/${product.storeSlug}/products/${product.productSlug}`} className="group">
      <div className="relative aspect-square overflow-hidden rounded-lg bg-gray-100">
        {product.imageUrl ? (
          <Image
            src={product.imageUrl}
            alt={product.productName}
            fill
            sizes="(max-width: 640px) 50vw, 25vw"
            className="object-cover transition group-hover:scale-105"
          />
        ) : (
          <div className="flex h-full items-center justify-center text-gray-300">
            <Package className="h-10 w-10" />
          </div>
        )}
      </div>
      <div className="mt-2">
        <h3 className="truncate text-sm font-medium text-gray-900">{product.productName}</h3>
        <div className="flex items-center gap-2">
          <span className="text-sm font-semibold text-gray-900">{money.format(product.price)}</span>
          {product.compareAtPrice && (
            <span className="text-xs text-gray-400 line-through">{money.format(product.compareAtPrice)}</span>
          )}
        </div>
        <p className="mt-0.5 truncate text-xs text-gray-500">{product.storeName}</p>
      </div>
    </Link>
  )
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
