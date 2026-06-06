const KEY = 'ls_cart_token'

/**
 * An opaque per-browser cart token. Carts are anonymous and non-sensitive, so a
 * random client-generated token is sufficient; it's persisted in localStorage so
 * the cart survives reloads.
 */
export function getCartToken(): string {
  if (typeof window === 'undefined') return ''
  let token = localStorage.getItem(KEY)
  if (!token) {
    token = crypto.randomUUID()
    localStorage.setItem(KEY, token)
  }
  return token
}
