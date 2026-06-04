'use client'

import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { Store, LogOut } from 'lucide-react'
import { clsx } from 'clsx'
import { useAuthStore } from '@/stores/authStore'
import { SELLER_NAV } from '@/lib/navigation/sellerNav'

export default function SellerSidebar() {
  const pathname = usePathname()
  const router = useRouter()
  const user = useAuthStore((s) => s.user)
  const storeSignOut = useAuthStore((s) => s.signOut)

  const signOut = () => {
    storeSignOut()
    router.push('/login')
    router.refresh()
  }

  return (
    <aside className="flex w-64 flex-col bg-neutral text-neutral-content">
      <div className="border-b border-white/10 p-5">
        <Link href="/seller/dashboard" className="flex items-center gap-2">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-info text-info-content">
            <Store className="h-5 w-5" />
          </div>
          <span className="text-lg font-bold">Seller Center</span>
        </Link>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-4">
        <ul className="menu menu-sm gap-0.5 p-0">
          {SELLER_NAV.map((item) => {
            const Icon = item.icon
            const isActive = pathname === item.href

            if (item.status === 'soon') {
              return (
                <li key={item.href}>
                  <span
                    className="cursor-not-allowed justify-between opacity-40"
                    aria-disabled="true"
                    title="Coming soon"
                  >
                    <span className="flex items-center gap-3">
                      <Icon className="h-4 w-4 shrink-0" />
                      {item.label}
                    </span>
                    <span className="badge badge-ghost badge-xs">soon</span>
                  </span>
                </li>
              )
            }

            return (
              <li key={item.href}>
                <Link
                  href={item.href}
                  className={clsx('gap-3', isActive && 'active bg-info text-info-content')}
                >
                  <Icon className="h-4 w-4 shrink-0" />
                  {item.label}
                </Link>
              </li>
            )
          })}
        </ul>
      </nav>

      <div className="border-t border-white/10 p-3">
        {user && (
          <div className="mb-2 flex items-center gap-2 px-2">
            <div className="avatar placeholder">
              <div className="h-9 w-9 rounded-full bg-info text-info-content">
                <span className="text-sm font-semibold">
                  {(user.fullName ?? user.email).charAt(0).toUpperCase()}
                </span>
              </div>
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium">{user.fullName ?? user.email}</p>
              <span className="badge badge-info badge-xs">Vendor</span>
            </div>
          </div>
        )}
        <button onClick={signOut} className="btn btn-ghost btn-sm w-full justify-start gap-3">
          <LogOut className="h-4 w-4" />
          Sign out
        </button>
      </div>
    </aside>
  )
}
