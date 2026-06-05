import type { Metadata } from 'next'
import CategoriesView from './CategoriesView'

export const metadata: Metadata = { title: 'Categories — Admin' }

export default function CategoriesPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Categories</h1>
        <p className="text-sm text-base-content/60">
          Organize your catalog into a browsable tree
        </p>
      </div>
      <CategoriesView />
    </div>
  )
}
