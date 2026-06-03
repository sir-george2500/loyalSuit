import { NextResponse, type NextRequest } from 'next/server'

const TOKEN_COOKIE = 'ls_token'
const PROTECTED_ROUTES = ['/admin', '/seller', '/pos', '/account']
const AUTH_ROUTES = ['/login', '/register']
const DEFAULT_REDIRECT = '/admin/dashboard'

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
  const { pathname } = request.nextUrl

  const isProtected = PROTECTED_ROUTES.some((r) => pathname.startsWith(r))
  const isAuthRoute = AUTH_ROUTES.some((r) => pathname.startsWith(r))

  if (isProtected && !authenticated) {
    const url = request.nextUrl.clone()
    url.pathname = '/login'
    url.searchParams.set('next', pathname)
    const res = NextResponse.redirect(url)
    if (token) res.cookies.delete(TOKEN_COOKIE) // clear an expired token
    return res
  }

  if (isAuthRoute && authenticated) {
    const url = request.nextUrl.clone()
    url.pathname = DEFAULT_REDIRECT
    return NextResponse.redirect(url)
  }

  return NextResponse.next()
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)'],
}
