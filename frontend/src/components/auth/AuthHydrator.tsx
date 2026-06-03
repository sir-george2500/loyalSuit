'use client'

import { useEffect } from 'react'
import { useAuthStore } from '@/stores/authStore'
import { authApi } from '@/lib/api/auth'
import { getToken } from '@/lib/auth/session'

/**
 * Ensures the in-memory user is populated when a valid session cookie exists but
 * the store is empty (e.g. a fresh device or cleared localStorage). Role-aware UI
 * depends on this; backend authorization is independent of it.
 */
export default function AuthHydrator() {
  const user = useAuthStore((s) => s.user)
  const setUser = useAuthStore((s) => s.setUser)

  useEffect(() => {
    if (user || !getToken()) return
    let cancelled = false
    authApi
      .me()
      .then((res) => {
        if (!cancelled) setUser(res.data.data)
      })
      .catch(() => {
        /* interceptor handles 401; nothing else to do */
      })
    return () => {
      cancelled = true
    }
  }, [user, setUser])

  return null
}
