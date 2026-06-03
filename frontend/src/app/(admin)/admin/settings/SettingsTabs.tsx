'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { clsx } from 'clsx'

const tabs = [
  { label: 'General', href: '/admin/settings' },
  { label: 'Security', href: '/admin/settings/security' },
]

export default function SettingsTabs() {
  const pathname = usePathname()

  return (
    <div role="tablist" className="tabs tabs-bordered">
      {tabs.map((tab) => (
        <Link
          key={tab.href}
          href={tab.href}
          role="tab"
          className={clsx('tab', pathname === tab.href && 'tab-active font-semibold')}
        >
          {tab.label}
        </Link>
      ))}
      <span role="tab" className="tab cursor-not-allowed opacity-40" aria-disabled="true">
        Notifications
        <span className="badge badge-ghost badge-xs ml-1">soon</span>
      </span>
    </div>
  )
}
