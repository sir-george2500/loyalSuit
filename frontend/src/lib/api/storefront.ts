import type {
  ApiResponse,
  PageResponse,
  StoreCategory,
  StoreProductDetail,
  StoreProductSummary,
  StoreSummary,
  StoreView,
} from '@/types'

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

/**
 * Server-side fetch for the public storefront. These endpoints are anonymous, so
 * no auth header is sent. Responses are cached briefly (ISR) — storefront content
 * changes rarely relative to traffic. Returns null on 404 so callers can render
 * a not-found page.
 */
async function storeFetch<T>(path: string): Promise<T | null> {
  const res = await fetch(`${API_URL}/api/v1/store${path}`, {
    next: { revalidate: 60 },
  })
  if (res.status === 404) return null
  if (!res.ok) throw new Error(`Storefront request failed: ${res.status}`)
  const body = (await res.json()) as ApiResponse<T>
  return body.data
}

/** The public marketplace index — every browsable store. Never 404s; worst case an empty list. */
export function getStores() {
  return storeFetch<StoreSummary[]>('')
}

export function getStore(slug: string) {
  return storeFetch<StoreView>(`/${encodeURIComponent(slug)}`)
}

export function getStoreCategories(slug: string) {
  return storeFetch<StoreCategory[]>(`/${encodeURIComponent(slug)}/categories`)
}

export function getStoreProducts(slug: string, opts: { category?: string; page?: number } = {}) {
  const params = new URLSearchParams()
  if (opts.category) params.set('category', opts.category)
  if (opts.page) params.set('page', String(opts.page))
  const qs = params.toString()
  return storeFetch<PageResponse<StoreProductSummary>>(
    `/${encodeURIComponent(slug)}/products${qs ? `?${qs}` : ''}`
  )
}

export function getStoreProduct(slug: string, productSlug: string) {
  return storeFetch<StoreProductDetail>(
    `/${encodeURIComponent(slug)}/products/${encodeURIComponent(productSlug)}`
  )
}
