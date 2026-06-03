import type { Metadata } from 'next'
import SettingsTabs from './SettingsTabs'

export const metadata: Metadata = { title: 'Settings — Admin' }

export default function SettingsLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Settings</h1>
        <p className="text-sm text-base-content/60">Manage your account and security</p>
      </div>
      <SettingsTabs />
      {children}
    </div>
  )
}
