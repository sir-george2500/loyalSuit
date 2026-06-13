import type { Metadata } from 'next'
import Link from 'next/link'
import { notFound } from 'next/navigation'
import { Package, ChevronLeft, ChevronRight } from 'lucide-react'
import { getMarketplace, getMarketplaceCategories, getMarketplaceProducts } from '@/lib/api/marketplace'
import MarketplaceSearchBar from '@/components/store/MarketplaceSearchBar'
import MarketplaceCard from '@/components/store/MarketplaceCard'

export const metadata: Metadata = {
  title: 'Loyal Spare Parts — Shop',
  description: 'Shop Loyal Spare Parts and its sellers — one marketplace, no account needed.',
}

type Search = { category?: string; page?: string }

export default async function MarketplacePage({
  searchParams,
}: {
  searchParams: Promise<Search>
}) {
  const { category, page } = await searchParams
  const pageIndex = Math.max(0, Number(page ?? 0) || 0)

  const store = await getMarketplace()
  if (!store) notFound()

  const [categories, products] = await Promise.all([
    getMarketplaceCategories(),
    getMarketplaceProducts({ category, page: pageIndex }),
  ])

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <header className="mb-8 text-center">
        <h1 className="text-3xl font-bold text-gray-900 sm:text-4xl">{store.name}</h1>
        <p className="mt-2 text-gray-500">Everything from Loyal Spare Parts and our sellers, in one place.</p>
        <div className="mx-auto mt-6 max-w-xl">
          <MarketplaceSearchBar />
        </div>
      </header>

      {/* Category filter */}
      {categories && categories.length > 0 && (
        <nav className="mb-6 flex flex-wrap justify-center gap-2">
          <CategoryChip active={!category}>All</CategoryChip>
          {categories.map((c) => (
            <CategoryChip key={c.slug} category={c.slug} active={category === c.slug}>
              {c.name}
            </CategoryChip>
          ))}
        </nav>
      )}

      {products && products.content.length > 0 ? (
        <>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
            {products.content.map((p) => (
              <MarketplaceCard key={p.productId} product={p} storeSlug={store.slug} currency={store.currency} />
            ))}
          </div>

          <div className="mt-8 flex items-center justify-between text-sm text-gray-500">
            <span>
              Page {products.page + 1} of {Math.max(products.totalPages, 1)}
            </span>
            <div className="flex gap-2">
              {!products.first && (
                <Link
                  href={pageHref(category, products.page - 1)}
                  className="inline-flex items-center gap-1 rounded border border-gray-300 px-3 py-1.5 hover:bg-gray-50"
                >
                  <ChevronLeft className="h-4 w-4" /> Prev
                </Link>
              )}
              {!products.last && (
                <Link
                  href={pageHref(category, products.page + 1)}
                  className="inline-flex items-center gap-1 rounded border border-gray-300 px-3 py-1.5 hover:bg-gray-50"
                >
                  Next <ChevronRight className="h-4 w-4" />
                </Link>
              )}
            </div>
          </div>
        </>
      ) : (
        <div className="flex flex-col items-center gap-2 py-20 text-center text-gray-400">
          <Package className="h-10 w-10" />
          <p>No products here yet.</p>
        </div>
      )}
    </div>
  )
}

function pageHref(category: string | undefined, page: number): string {
  const params = new URLSearchParams()
  if (category) params.set('category', category)
  if (page > 0) params.set('page', String(page))
  const qs = params.toString()
  return `/store${qs ? `?${qs}` : ''}`
}

function CategoryChip({
  category,
  active,
  children,
}: {
  category?: string
  active: boolean
  children: React.ReactNode
}) {
  return (
    <Link
      href={category ? `/store?category=${category}` : '/store'}
      className={`rounded-full px-3 py-1 text-sm ${
        active ? 'bg-gray-900 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
      }`}
    >
      {children}
    </Link>
  )
}
