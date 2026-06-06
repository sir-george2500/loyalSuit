import type { Metadata } from 'next'
import Link from 'next/link'
import Image from 'next/image'
import { notFound } from 'next/navigation'
import { Package, ChevronLeft, ChevronRight } from 'lucide-react'
import { getStore, getStoreCategories, getStoreProducts } from '@/lib/api/storefront'
import CartLink from '@/components/store/CartLink'
import type { StoreProductSummary } from '@/types'

type Params = { slug: string }
type Search = { category?: string; page?: string }

export async function generateMetadata({ params }: { params: Promise<Params> }): Promise<Metadata> {
  const { slug } = await params
  const store = await getStore(slug)
  return { title: store ? `${store.name} — Store` : 'Store' }
}

export default async function StoreHomePage({
  params,
  searchParams,
}: {
  params: Promise<Params>
  searchParams: Promise<Search>
}) {
  const { slug } = await params
  const { category, page } = await searchParams

  const store = await getStore(slug)
  if (!store) notFound()

  const pageIndex = Math.max(0, Number(page ?? 0) || 0)
  const [categories, products] = await Promise.all([
    getStoreCategories(slug),
    getStoreProducts(slug, { category, page: pageIndex }),
  ])

  const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: store.currency || 'USD' })

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <header className="mb-6 flex items-center justify-between">
        <h1 className="text-3xl font-bold text-gray-900">{store.name}</h1>
        <div className="flex items-center gap-4">
          <Link href={`/store/${slug}/track`} className="text-sm text-gray-500 hover:text-gray-700">
            Track order
          </Link>
          <CartLink slug={slug} />
        </div>
      </header>

      {/* Category filter */}
      {categories && categories.length > 0 && (
        <nav className="mb-6 flex flex-wrap gap-2">
          <CategoryChip slug={slug} active={!category}>
            All
          </CategoryChip>
          {categories.map((c) => (
            <CategoryChip key={c.slug} slug={slug} category={c.slug} active={category === c.slug}>
              {c.name}
            </CategoryChip>
          ))}
        </nav>
      )}

      {products && products.content.length > 0 ? (
        <>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
            {products.content.map((p) => (
              <ProductCard key={p.id} slug={slug} product={p} price={money.format(p.price)} compareAt={
                p.compareAtPrice ? money.format(p.compareAtPrice) : null
              } />
            ))}
          </div>

          <div className="mt-8 flex items-center justify-between text-sm text-gray-500">
            <span>
              Page {products.page + 1} of {Math.max(products.totalPages, 1)}
            </span>
            <div className="flex gap-2">
              {!products.first && (
                <Link
                  href={pageHref(slug, category, products.page - 1)}
                  className="inline-flex items-center gap-1 rounded border border-gray-300 px-3 py-1.5 hover:bg-gray-50"
                >
                  <ChevronLeft className="h-4 w-4" /> Prev
                </Link>
              )}
              {!products.last && (
                <Link
                  href={pageHref(slug, category, products.page + 1)}
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

function pageHref(slug: string, category: string | undefined, page: number): string {
  const params = new URLSearchParams()
  if (category) params.set('category', category)
  if (page > 0) params.set('page', String(page))
  const qs = params.toString()
  return `/store/${slug}${qs ? `?${qs}` : ''}`
}

function CategoryChip({
  slug,
  category,
  active,
  children,
}: {
  slug: string
  category?: string
  active: boolean
  children: React.ReactNode
}) {
  return (
    <Link
      href={category ? `/store/${slug}?category=${category}` : `/store/${slug}`}
      className={`rounded-full px-3 py-1 text-sm ${
        active ? 'bg-gray-900 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
      }`}
    >
      {children}
    </Link>
  )
}

function ProductCard({
  slug,
  product,
  price,
  compareAt,
}: {
  slug: string
  product: StoreProductSummary
  price: string
  compareAt: string | null
}) {
  return (
    <Link href={`/store/${slug}/products/${product.slug}`} className="group">
      <div className="relative aspect-square overflow-hidden rounded-lg bg-gray-100">
        {product.imageUrl ? (
          <Image
            src={product.imageUrl}
            alt={product.name}
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
        <h3 className="truncate text-sm font-medium text-gray-900">{product.name}</h3>
        <div className="flex items-center gap-2">
          <span className="text-sm font-semibold text-gray-900">{price}</span>
          {compareAt && <span className="text-xs text-gray-400 line-through">{compareAt}</span>}
        </div>
      </div>
    </Link>
  )
}
