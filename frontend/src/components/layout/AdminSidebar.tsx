'use client'

import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { LogOut } from 'lucide-react'
import { clsx } from 'clsx'
import Logo from '@/components/Logo'
import { useAuthStore } from '@/stores/authStore'
import { navForRole } from '@/lib/navigation/adminNav'
import type { UserRole } from '@/types'

const ROLE_LABELS: Record<UserRole, string> = {
  SUPER_ADMIN: 'Super Admin',
  TENANT_ADMIN: 'Admin',
  STAFF: 'Staff',
  VENDOR: 'Vendor',
  CUSTOMER: 'Customer',
  DELIVERY_AGENT: 'Delivery',
}

const ROLE_BADGE: Record<UserRole, string> = {
  SUPER_ADMIN: 'badge-secondary',
  TENANT_ADMIN: 'badge-primary',
  STAFF: 'badge-accent',
  VENDOR: 'badge-info',
  CUSTOMER: 'badge-ghost',
  DELIVERY_AGENT: 'badge-ghost',
}

export default function AdminSidebar() {
  const pathname = usePathname()
  const router = useRouter()
  const user = useAuthStore((s) => s.user)
  const storeSignOut = useAuthStore((s) => s.signOut)

  const sections = navForRole(user?.role)

  const signOut = () => {
    storeSignOut()
    router.push('/login')
    router.refresh()
  }

  return (
    <aside className="flex w-64 flex-col bg-neutral text-neutral-content">
      {/* Brand */}
      <div className="border-b border-white/10 p-5">
        <Logo href="/admin/dashboard" className="h-9" chip />
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto px-3 py-4">
        {sections.map((section) => (
          <div key={section.title} className="mb-4">
            <p className="px-3 pb-1 text-[0.65rem] font-semibold uppercase tracking-wider text-neutral-content/40">
              {section.title}
            </p>
            <ul className="menu menu-sm gap-0.5 p-0">
              {section.items.map((item) => {
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
                      className={clsx('gap-3', isActive && 'active bg-primary text-primary-content')}
                    >
                      <Icon className="h-4 w-4 shrink-0" />
                      {item.label}
                    </Link>
                  </li>
                )
              })}
            </ul>
          </div>
        ))}
      </nav>

      {/* User footer */}
      <div className="border-t border-white/10 p-3">
        {user && (
          <div className="mb-2 flex items-center gap-2 px-2">
            <div className="avatar placeholder">
              <div className="h-9 w-9 rounded-full bg-primary text-primary-content">
                <span className="text-sm font-semibold">
                  {(user.fullName ?? user.email).charAt(0).toUpperCase()}
                </span>
              </div>
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium">{user.fullName ?? user.email}</p>
              <span className={clsx('badge badge-xs', ROLE_BADGE[user.role])}>
                {ROLE_LABELS[user.role]}
              </span>
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
