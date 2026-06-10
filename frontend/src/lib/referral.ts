const KEY = 'ls_ref'

/** Capture a ?ref=CODE from the current URL into localStorage (persists to checkout). */
export function captureReferralFromUrl(): void {
  if (typeof window === 'undefined') return
  const ref = new URLSearchParams(window.location.search).get('ref')
  if (ref && ref.trim()) localStorage.setItem(KEY, ref.trim().toUpperCase())
}

export function getReferral(): string | null {
  return typeof window === 'undefined' ? null : localStorage.getItem(KEY)
}

export function clearReferral(): void {
  if (typeof window !== 'undefined') localStorage.removeItem(KEY)
}
