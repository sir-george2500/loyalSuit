import type { Metadata } from 'next'
import Link from 'next/link'
import Image from 'next/image'
import { notFound } from 'next/navigation'
import { Store, Package, ChevronLeft, ChevronRight } from 'lucide-react'
import { getMarketplace, getVendor, getVendorProducts } from '@/lib/api/marketplace'
import MarketplaceCard from '@/components/store/MarketplaceCard'

type Params = { slug: string }
type Search = { page?: string }

export async function generateMetadata({ params }: { params: Promise<Params> }): Promise<Metadata> {
  const { slug } = await params
  const vendor = await getVendor(slug)
  return { title: vendor ? `${vendor.storeName} — Loyal Spare Parts` : 'Vendor — Loyal Spare Parts' }
}

export default async function VendorStorefrontPage({
  params,
  searchParams,
}: {
  params: Promise<Params>
  searchParams: Promise<Search>
}) {
  const { slug } = await params
  const { page } = await searchParams
  const pageIndex = Math.max(0, Number(page ?? 0) || 0)

  const [store, vendor] = await Promise.all([getMarketplace(), getVendor(slug)])
  if (!store || !vendor) notFound()

  const products = await getVendorProducts(slug, pageIndex)

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <p className="mb-4 text-sm text-gray-400">
        <Link href="/store" className="hover:text-gray-600">Loyal Spare Parts</Link> · Seller
      </p>

      <header className="mb-8 flex items-center gap-4">
        <div className="relative h-20 w-20 shrink-0 overflow-hidden rounded-2xl bg-gray-100">
          {vendor.logoUrl ? (
            <Image src={vendor.logoUrl} alt={vendor.storeName} fill sizes="80px" className="object-cover" />
          ) : (
            <div className="flex h-full items-center justify-center text-gray-300">
              <Store className="h-9 w-9" />
            </div>
          )}
        </div>
        <div className="min-w-0">
          <h1 className="text-2xl font-bold text-gray-900">{vendor.storeName}</h1>
          <p className="text-sm text-gray-400">Seller on Loyal Spare Parts</p>
          {vendor.description && <p className="mt-1 line-clamp-2 text-sm text-gray-600">{vendor.description}</p>}
        </div>
      </header>

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
                  href={pageHref(slug, products.page - 1)}
                  className="inline-flex items-center gap-1 rounded border border-gray-300 px-3 py-1.5 hover:bg-gray-50"
                >
                  <ChevronLeft className="h-4 w-4" /> Prev
                </Link>
              )}
              {!products.last && (
                <Link
                  href={pageHref(slug, products.page + 1)}
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
          <p>This seller has no products yet.</p>
        </div>
      )}
    </div>
  )
}

function pageHref(slug: string, page: number): string {
  return page > 0 ? `/store/vendor/${slug}?page=${page}` : `/store/vendor/${slug}`
}
