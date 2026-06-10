import { Truck } from 'lucide-react'
import { guardArea } from '@/lib/auth/server'

export default async function DeliveryLayout({ children }: { children: React.ReactNode }) {
  await guardArea('/delivery')

  return (
    <div className="min-h-screen bg-gray-900 text-white">
      <header className="flex items-center justify-between border-b border-gray-700 bg-gray-800 px-5 py-3">
        <span className="flex items-center gap-2 text-lg font-bold">
          <Truck className="h-5 w-5" /> Deliveries
        </span>
        <span className="text-sm text-gray-400">LoyalSuit</span>
      </header>
      <main className="mx-auto max-w-2xl p-4">{children}</main>
    </div>
  )
}
