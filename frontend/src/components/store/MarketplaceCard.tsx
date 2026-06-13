import Link from 'next/link'
import Image from 'next/image'
import { Package } from 'lucide-react'
import type { MarketplaceProductCard } from '@/types'

/**
 * A product tile on the LoyalSuit marketplace. Links into the existing per-store product page
 * (`/store/{slug}/products/{productSlug}`), which already handles detail, cart and checkout.
 * Shows the seller — the vendor's name, or "LoyalSuit" for house products.
 */
export default function MarketplaceCard({
  product,
  storeSlug,
  currency,
}: {
  product: MarketplaceProductCard
  storeSlug: string
  currency: string
}) {
  const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: currency || 'USD' })
  return (
    <div className="group">
      <Link href={`/store/${storeSlug}/products/${product.slug}`}>
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
            <span className="text-sm font-semibold text-gray-900">{money.format(product.price)}</span>
            {product.compareAtPrice && (
              <span className="text-xs text-gray-400 line-through">{money.format(product.compareAtPrice)}</span>
            )}
          </div>
        </div>
      </Link>
      {/* Seller line lives outside the product link so the vendor name can link to their storefront. */}
      <p className="mt-0.5 truncate text-xs text-gray-500">
        Sold by{' '}
        {product.soldBySlug ? (
          <Link href={`/store/vendor/${product.soldBySlug}`} className="text-primary-600 hover:underline">
            {product.soldBy}
          </Link>
        ) : (
          <span className="text-gray-700">{product.soldBy ?? 'LoyalSuit'}</span>
        )}
      </p>
    </div>
  )
}
