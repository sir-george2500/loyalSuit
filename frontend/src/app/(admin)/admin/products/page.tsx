import type { Metadata } from 'next'
import ProductsView from './ProductsView'

export const metadata: Metadata = { title: 'Products — Admin' }

export default function ProductsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Products</h1>
        <p className="text-sm text-base-content/60">Build and manage your catalog</p>
      </div>
      <ProductsView />
    </div>
  )
}
