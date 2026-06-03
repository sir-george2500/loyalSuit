import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { UserProfile } from '@/types'
import { clearToken, setToken } from '@/lib/auth/session'

interface AuthState {
  user: UserProfile | null
  /** Persist the authenticated session (token cookie + user in memory/localStorage). */
  signIn: (token: string, expiresIn: number, user: UserProfile) => void
  setUser: (user: UserProfile | null) => void
  signOut: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      signIn: (token, expiresIn, user) => {
        setToken(token, expiresIn)
        set({ user })
      },
      setUser: (user) => set({ user }),
      signOut: () => {
        clearToken()
        set({ user: null })
      },
    }),
    {
      name: 'loyalsuit-auth',
      partialize: (state) => ({ user: state.user }),
    }
  )
)
