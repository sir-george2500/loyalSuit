import type { Metadata } from 'next'
import Link from 'next/link'
import Image from 'next/image'
import { notFound } from 'next/navigation'
import { Package, ArrowLeft, Check, X } from 'lucide-react'
import { getStore, getStoreProduct } from '@/lib/api/storefront'
import AddToCart from '@/components/store/AddToCart'
import CartLink from '@/components/store/CartLink'
import ProductReviews from '@/components/store/ProductReviews'

type Params = { slug: string; productSlug: string }

export async function generateMetadata({ params }: { params: Promise<Params> }): Promise<Metadata> {
  const { slug, productSlug } = await params
  const product = await getStoreProduct(slug, productSlug)
  return { title: product ? product.name : 'Product' }
}

export default async function ProductDetailPage({ params }: { params: Promise<Params> }) {
  const { slug, productSlug } = await params

  const [store, product] = await Promise.all([getStore(slug), getStoreProduct(slug, productSlug)])
  if (!store || !product) notFound()

  const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: store.currency || 'RWF' })

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6 flex items-center justify-between">
        <Link href={`/store/${slug}`} className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700">
          <ArrowLeft className="h-4 w-4" /> Back to {store.name}
        </Link>
        <CartLink slug={slug} />
      </div>

      <div className="grid gap-8 md:grid-cols-2">
        {/* Gallery */}
        <div className="space-y-3">
          <div className="relative aspect-square overflow-hidden rounded-xl bg-gray-100">
            {product.images.length > 0 ? (
              <Image
                src={product.images[0]}
                alt={product.name}
                fill
                sizes="(max-width: 768px) 100vw, 50vw"
                className="object-cover"
                priority
              />
            ) : (
              <div className="flex h-full items-center justify-center text-gray-300">
                <Package className="h-16 w-16" />
              </div>
            )}
          </div>
          {product.images.length > 1 && (
            <div className="grid grid-cols-5 gap-2">
              {product.images.slice(1, 6).map((url, i) => (
                <div key={i} className="relative aspect-square overflow-hidden rounded-lg bg-gray-100">
                  <Image src={url} alt="" fill sizes="80px" className="object-cover" />
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Info */}
        <div>
          {product.categoryName && (
            <p className="text-sm text-gray-500">{product.categoryName}</p>
          )}
          <h1 className="mt-1 text-2xl font-bold text-gray-900">{product.name}</h1>

          <p className="mt-1 text-sm text-gray-500">
            Sold by{' '}
            {product.soldBySlug ? (
              <Link href={`/store/vendor/${product.soldBySlug}`} className="text-brand-600 hover:underline">
                {product.soldBy}
              </Link>
            ) : (
              <span className="text-gray-700">{product.soldBy ?? 'Loyal Spare Parts'}</span>
            )}
          </p>

          <div className="mt-3 flex items-center gap-3">
            <span className="text-2xl font-semibold text-gray-900">{money.format(product.price)}</span>
            {product.compareAtPrice && (
              <span className="text-lg text-gray-400 line-through">
                {money.format(product.compareAtPrice)}
              </span>
            )}
          </div>

          <div className="mt-3">
            {product.inStock ? (
              <span className="inline-flex items-center gap-1 rounded-full bg-green-50 px-2.5 py-1 text-sm text-green-700">
                <Check className="h-3.5 w-3.5" /> In stock
              </span>
            ) : (
              <span className="inline-flex items-center gap-1 rounded-full bg-gray-100 px-2.5 py-1 text-sm text-gray-500">
                <X className="h-3.5 w-3.5" /> Out of stock
              </span>
            )}
          </div>

          <AddToCart
            slug={slug}
            productId={product.id}
            variants={product.variants}
            inStock={product.inStock}
          />

          {product.description && (
            <p className="mt-6 whitespace-pre-line text-gray-700">{product.description}</p>
          )}
        </div>
      </div>

      <ProductReviews productId={product.id} />
    </div>
  )
}
