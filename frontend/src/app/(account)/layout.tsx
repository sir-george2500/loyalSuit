import { redirect } from 'next/navigation'
import { getServerRole } from '@/lib/auth/server'
import AuthHydrator from '@/components/auth/AuthHydrator'

/** Authenticated area open to any role (customers, vendors, staff) — e.g. seller signup. */
export default async function AccountLayout({ children }: { children: React.ReactNode }) {
  const role = await getServerRole()
  if (!role) redirect('/login?next=/become-seller')

  return (
    <div className="min-h-screen bg-base-200">
      <AuthHydrator />
      {children}
    </div>
  )
}
