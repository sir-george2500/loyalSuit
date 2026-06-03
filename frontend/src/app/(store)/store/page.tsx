import type { Metadata } from 'next'

export const metadata: Metadata = { title: 'Shop — LoyalSuit' }

export default function StorePage() {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <h1 className="text-3xl font-bold text-gray-900 mb-8">Shop All Products</h1>
      <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 lg:grid-cols-4">
        <p className="col-span-full text-center text-gray-400 py-16">
          Products will appear here
        </p>
      </div>
    </div>
  )
}
