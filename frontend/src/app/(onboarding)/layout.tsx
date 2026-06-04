import { redirect } from 'next/navigation'
import { getServerRole } from '@/lib/auth/server'
import { homeForRole } from '@/lib/auth/roles'
import AuthHydrator from '@/components/auth/AuthHydrator'

/**
 * Full-screen setup flow. Only tenant owners run onboarding; everyone else is sent
 * to their own home. This layout intentionally does NOT gate on onboarding status
 * (that would loop) — the page itself redirects out once setup is complete.
 */
export default async function OnboardingLayout({ children }: { children: React.ReactNode }) {
  const role = await getServerRole()
  if (!role) redirect('/login?next=/onboarding')
  if (role !== 'SUPER_ADMIN' && role !== 'TENANT_ADMIN') redirect(homeForRole(role))

  return (
    <div className="min-h-screen bg-base-200">
      <AuthHydrator />
      {children}
    </div>
  )
}
