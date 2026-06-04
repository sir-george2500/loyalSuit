import { NextResponse, type NextRequest } from 'next/server'
import { canAccess, homeForRole, isProtectedArea, roleFromToken } from '@/lib/auth/roles'

const TOKEN_COOKIE = 'ls_token'
const AUTH_ROUTES = ['/login', '/register']

/** Decode a JWT payload at the edge (no verification — only to read exp). */
function isExpired(token: string): boolean {
  try {
    const payload = JSON.parse(
      atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))
    )
    return typeof payload.exp !== 'number' || payload.exp * 1000 <= Date.now()
  } catch {
    return true
  }
}

export function middleware(request: NextRequest) {
  const token = request.cookies.get(TOKEN_COOKIE)?.value
  const authenticated = !!token && !isExpired(token)
  const role = authenticated ? roleFromToken(token) : null
  const { pathname } = request.nextUrl

  const isProtected = isProtectedArea(pathname)
  const isAuthRoute = AUTH_ROUTES.some((r) => pathname.startsWith(r))

  // Unauthenticated user hitting a protected area → bounce to login, remember target.
  if (isProtected && !authenticated) {
    const url = request.nextUrl.clone()
    url.pathname = '/login'
    url.searchParams.set('next', pathname)
    const res = NextResponse.redirect(url)
    if (token) res.cookies.delete(TOKEN_COOKIE) // clear an expired token
    return res
  }

  // Authenticated, but this area isn't theirs → send them to their own home,
  // never leak one role's shell to another (a CUSTOMER must not see /admin).
  if (isProtected && authenticated && !canAccess(role, pathname)) {
    const url = request.nextUrl.clone()
    url.pathname = homeForRole(role)
    url.search = ''
    return NextResponse.redirect(url)
  }

  // Already signed in but on /login or /register → forward to role-appropriate home.
  if (isAuthRoute && authenticated) {
    const url = request.nextUrl.clone()
    url.pathname = homeForRole(role)
    url.search = ''
    return NextResponse.redirect(url)
  }

  return NextResponse.next()
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)'],
}
