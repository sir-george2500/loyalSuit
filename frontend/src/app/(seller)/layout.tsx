import { guardArea } from '@/lib/auth/server'
import SellerSidebar from '@/components/layout/SellerSidebar'
import AuthHydrator from '@/components/auth/AuthHydrator'

export default async function SellerLayout({ children }: { children: React.ReactNode }) {
  await guardArea('/seller')

  return (
    <div className="flex h-screen bg-base-200">
      <AuthHydrator />
      <SellerSidebar />
      <main className="flex-1 overflow-y-auto">
        <div className="p-6 lg:p-8">{children}</div>
      </main>
    </div>
  )
}
