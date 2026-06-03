import { redirect } from 'next/navigation'
import { isAuthenticated } from '@/lib/auth/server'

export default async function SellerLayout({ children }: { children: React.ReactNode }) {
  if (!(await isAuthenticated())) redirect('/login?next=/seller/dashboard')

  return (
    <div className="flex h-screen bg-gray-50">
      <aside className="w-60 bg-white border-r border-gray-200 p-4">
        <h2 className="font-bold text-gray-900 mb-4">Seller Dashboard</h2>
      </aside>
      <main className="flex-1 overflow-y-auto p-8">{children}</main>
    </div>
  )
}
