import { describe, it, expect } from 'vitest'
import { homeForRole, canAccess, isProtectedArea, roleFromToken } from './roles'
import type { UserRole } from '@/types'

/** Build a JWT-shaped string carrying the given payload (signature is irrelevant here). */
function jwtWith(payload: Record<string, unknown>): string {
  const body = Buffer.from(JSON.stringify(payload)).toString('base64url')
  return `header.${body}.signature`
}

describe('homeForRole', () => {
  it('routes each role to a real, existing landing page', () => {
    // Arrange
    const cases: Array<[UserRole, string]> = [
      ['SUPER_ADMIN', '/admin/dashboard'],
      ['TENANT_ADMIN', '/admin/dashboard'],
      ['STAFF', '/admin/dashboard'],
      ['VENDOR', '/seller/dashboard'],
      ['CUSTOMER', '/store'],
      ['DELIVERY_AGENT', '/store'],
    ]

    // Act + Assert
    for (const [role, expected] of cases) {
      expect(homeForRole(role)).toBe(expected)
    }
  })

  it('sends an unauthenticated (null) role to login', () => {
    // Arrange + Act
    const home = homeForRole(null)

    // Assert
    expect(home).toBe('/login')
  })
})

describe('canAccess', () => {
  it('allows admin roles into /admin and keeps customers out', () => {
    // Arrange
    const path = '/admin/dashboard'

    // Act + Assert
    expect(canAccess('TENANT_ADMIN', path)).toBe(true)
    expect(canAccess('STAFF', path)).toBe(true)
    expect(canAccess('CUSTOMER', path)).toBe(false)
    expect(canAccess('VENDOR', path)).toBe(false)
  })

  it('scopes the seller area to vendors only', () => {
    // Arrange
    const path = '/seller/dashboard'

    // Act + Assert
    expect(canAccess('VENDOR', path)).toBe(true)
    expect(canAccess('TENANT_ADMIN', path)).toBe(false)
  })

  it('treats unlisted paths as public for any role, including null', () => {
    // Arrange + Act + Assert
    expect(canAccess('CUSTOMER', '/store')).toBe(true)
    expect(canAccess(null, '/')).toBe(true)
  })

  it('denies a protected area when the role is unknown', () => {
    // Arrange + Act
    const allowed = canAccess(null, '/admin')

    // Assert
    expect(allowed).toBe(false)
  })
})

describe('isProtectedArea', () => {
  it('recognizes protected areas and their descendants', () => {
    // Arrange + Act + Assert
    expect(isProtectedArea('/admin')).toBe(true)
    expect(isProtectedArea('/seller/dashboard')).toBe(true)
    expect(isProtectedArea('/pos')).toBe(true)
  })

  it('treats public routes as unprotected', () => {
    // Arrange + Act + Assert
    expect(isProtectedArea('/store')).toBe(false)
    expect(isProtectedArea('/')).toBe(false)
    expect(isProtectedArea('/login')).toBe(false)
  })

  it('does not match an area that is only a string prefix of the path', () => {
    // Arrange + Act — "/administrator" must not match the "/admin" area
    const result = isProtectedArea('/administrator')

    // Assert
    expect(result).toBe(false)
  })
})

describe('roleFromToken', () => {
  it('extracts the role claim from a well-formed token', () => {
    // Arrange
    const token = jwtWith({ role: 'VENDOR', exp: 9999999999 })

    // Act
    const role = roleFromToken(token)

    // Assert
    expect(role).toBe('VENDOR')
  })

  it('returns null for malformed or empty tokens', () => {
    // Arrange + Act + Assert
    expect(roleFromToken('not-a-jwt')).toBeNull()
    expect(roleFromToken('')).toBeNull()
    expect(roleFromToken(null)).toBeNull()
  })

  it('returns null when the token has no role claim', () => {
    // Arrange
    const token = jwtWith({ email: 'someone@acme.dev' })

    // Act + Assert
    expect(roleFromToken(token)).toBeNull()
  })
})
