import type {
  ApiResponse,
  MarketplaceProductCard,
  PageResponse,
  StoreCategory,
  StoreView,
  VendorStorefront,
} from '@/types'

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

/**
 * Server-side fetch for the public LoyalSuit marketplace — the single flagship storefront.
 * Anonymous (no auth header). The backend resolves the canonical store, so the frontend never
 * needs to know its slug. Briefly cached (ISR). Returns null on 404.
 */
async function mpFetch<T>(path: string): Promise<T | null> {
  const res = await fetch(`${API_URL}/api/v1/marketplace${path}`, {
    next: { revalidate: 60 },
  })
  if (res.status === 404) return null
  if (!res.ok) throw new Error(`Marketplace request failed: ${res.status}`)
  const body = (await res.json()) as ApiResponse<T>
  return body.data
}

/** The marketplace store profile (name, slug, currency, logo). */
export function getMarketplace() {
  return mpFetch<StoreView>('')
}

export function getMarketplaceCategories() {
  return mpFetch<StoreCategory[]>('/categories')
}

/** Browse the catalogue, house-first, optionally filtered by category. */
export function getMarketplaceProducts(opts: { category?: string; page?: number } = {}) {
  const params = new URLSearchParams()
  if (opts.category) params.set('category', opts.category)
  if (opts.page) params.set('page', String(opts.page))
  const qs = params.toString()
  return mpFetch<PageResponse<MarketplaceProductCard>>(`/products${qs ? `?${qs}` : ''}`)
}

/** Search the catalogue by product name. A blank query returns an empty page (server-enforced). */
export function searchMarketplace(query: string, page = 0) {
  const params = new URLSearchParams({ q: query })
  if (page) params.set('page', String(page))
  return mpFetch<PageResponse<MarketplaceProductCard>>(`/search?${params.toString()}`)
}

/** A vendor's public storefront profile (null on 404 — unknown or not-yet-approved vendor). */
export function getVendor(slug: string) {
  return mpFetch<VendorStorefront>(`/vendors/${encodeURIComponent(slug)}`)
}

/** A vendor's products. */
export function getVendorProducts(slug: string, page = 0) {
  const params = new URLSearchParams()
  if (page) params.set('page', String(page))
  const qs = params.toString()
  return mpFetch<PageResponse<MarketplaceProductCard>>(
    `/vendors/${encodeURIComponent(slug)}/products${qs ? `?${qs}` : ''}`
  )
}
