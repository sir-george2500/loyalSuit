import { cookies } from 'next/headers'
import { redirect } from 'next/navigation'
import type { OnboardingStatus, UserRole } from '@/types'
import { TOKEN_COOKIE } from './session'
import { canAccess, homeForRole, roleFromToken } from './roles'

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

/** Roles that own tenant setup — only these run (and are gated by) the wizard. */
const OWNER_ROLES: ReadonlyArray<UserRole> = ['SUPER_ADMIN', 'TENANT_ADMIN']

/** Read the auth token from cookies in a Server Component / layout. */
export async function getServerToken(): Promise<string | null> {
  const store = await cookies()
  return store.get(TOKEN_COOKIE)?.value ?? null
}

/** True when a non-empty token cookie is present (middleware enforces expiry). */
export async function isAuthenticated(): Promise<boolean> {
  return (await getServerToken()) !== null
}

/** The role carried by the current session's JWT, or null if unauthenticated. */
export async function getServerRole(): Promise<UserRole | null> {
  return roleFromToken(await getServerToken())
}

/**
 * Server-side area guard, run from a protected layout. Mirrors the edge middleware
 * so the page never renders for the wrong role even if middleware is bypassed.
 * Redirects to login when unauthenticated, or to the caller's own home when the
 * area belongs to a different role. Returns the role on success.
 */
export async function guardArea(areaPrefix: string): Promise<UserRole> {
  const role = await getServerRole()
  if (!role) redirect(`/login?next=${encodeURIComponent(areaPrefix)}`)
  if (!canAccess(role, areaPrefix)) redirect(homeForRole(role))
  return role
}

/**
 * Fetch onboarding status server-side with the session token. Returns null on any
 * failure (unauthenticated, network, non-200) so callers can fail open — a backend
 * blip must never wall the user out of their own dashboard.
 */
export async function getServerOnboardingStatus(): Promise<OnboardingStatus | null> {
  const token = await getServerToken()
  if (!token) return null
  try {
    const res = await fetch(`${API_URL}/api/v1/onboarding/status`, {
      headers: { Authorization: `Bearer ${token}` },
      cache: 'no-store',
    })
    if (!res.ok) return null
    const body = await res.json()
    return (body?.data as OnboardingStatus) ?? null
  } catch {
    return null
  }
}

/**
 * Redirect owner roles into the setup wizard until onboarding is complete. No-op
 * for non-owner roles (they can't onboard) and when status can't be determined.
 */
export async function requireOnboarded(role: UserRole): Promise<void> {
  if (!OWNER_ROLES.includes(role)) return
  const status = await getServerOnboardingStatus()
  if (status && !status.onboarded) redirect('/onboarding')
}
