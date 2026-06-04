import { guardArea, requireOnboarded } from '@/lib/auth/server'
import AdminSidebar from '@/components/layout/AdminSidebar'
import AuthHydrator from '@/components/auth/AuthHydrator'

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const role = await guardArea('/admin')
  // Owners must finish setup before reaching the admin shell.
  await requireOnboarded(role)

  return (
    <div className="flex h-screen bg-base-200">
      <AuthHydrator />
      <AdminSidebar />
      <main className="flex-1 overflow-y-auto">
        <div className="p-6 lg:p-8">{children}</div>
      </main>
    </div>
  )
}
