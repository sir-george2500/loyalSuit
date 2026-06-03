import { redirect } from 'next/navigation'
import { isAuthenticated } from '@/lib/auth/server'

export default async function PosLayout({ children }: { children: React.ReactNode }) {
  if (!(await isAuthenticated())) redirect('/login?next=/pos')

  return (
    <div className="min-h-screen bg-gray-900 text-white">
      <header className="bg-gray-800 px-6 py-3 flex items-center justify-between border-b border-gray-700">
        <span className="font-bold text-lg">POS Terminal</span>
        <span className="text-sm text-gray-400">LoyalSuit</span>
      </header>
      <main className="h-[calc(100vh-52px)]">{children}</main>
    </div>
  )
}
