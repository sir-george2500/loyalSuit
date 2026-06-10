/**
 * Single source of truth for role-based routing. Used by three layers that must
 * agree: the edge middleware (first gate), server layouts (defense-in-depth), and
 * the post-login redirect. The backend independently enforces authorization on
 * every API call — this governs navigation only, never trust.
 */
import type { UserRole } from '@/types'

/**
 * Where each role lands after authenticating. Every target is a route that
 * actually exists today — we never redirect a user into a 404.
 */
const ROLE_HOME: Record<UserRole, string> = {
  SUPER_ADMIN: '/admin/dashboard',
  TENANT_ADMIN: '/admin/dashboard',
  STAFF: '/admin/dashboard',
  VENDOR: '/seller/dashboard',
  CUSTOMER: '/store',
  DELIVERY_AGENT: '/delivery',
}

export function homeForRole(role: UserRole | null | undefined): string {
  if (!role) return '/login'
  return ROLE_HOME[role] ?? '/store'
}

/**
 * Which roles may enter each protected area. A path matches an area when it equals
 * the prefix or sits beneath it. Only areas that are actually built are listed;
 * unlisted paths are treated as public.
 */
const AREA_ROLES: ReadonlyArray<{ prefix: string; roles: readonly UserRole[] }> = [
  { prefix: '/admin', roles: ['SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF'] },
  { prefix: '/seller', roles: ['VENDOR'] },
  { prefix: '/pos', roles: ['SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF'] },
  { prefix: '/delivery', roles: ['DELIVERY_AGENT'] },
]

function areaFor(pathname: string) {
  return AREA_ROLES.find(
    (a) => pathname === a.prefix || pathname.startsWith(a.prefix + '/')
  )
}

/** True when `pathname` is behind one of the protected areas. */
export function isProtectedArea(pathname: string): boolean {
  return areaFor(pathname) !== undefined
}

/** Authorization check for a route. Public routes are always allowed. */
export function canAccess(role: UserRole | null | undefined, pathname: string): boolean {
  const area = areaFor(pathname)
  if (!area) return true
  if (!role) return false
  return area.roles.includes(role)
}

/**
 * Read the `role` claim from a JWT without verifying the signature. Safe for
 * routing decisions only — the signature is verified server-side by the backend.
 * Works in both the edge runtime and Node (both expose `atob`).
 */
export function roleFromToken(token: string | null | undefined): UserRole | null {
  if (!token) return null
  try {
    const payload = JSON.parse(
      atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))
    )
    return (payload.role as UserRole) ?? null
  } catch {
    return null
  }
}
