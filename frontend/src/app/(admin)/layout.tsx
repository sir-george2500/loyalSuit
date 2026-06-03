import { redirect } from 'next/navigation'
import { isAuthenticated } from '@/lib/auth/server'
import AdminSidebar from '@/components/layout/AdminSidebar'
import AuthHydrator from '@/components/auth/AuthHydrator'

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  if (!(await isAuthenticated())) redirect('/login?next=/admin/dashboard')

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
