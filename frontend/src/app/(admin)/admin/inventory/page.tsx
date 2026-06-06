import type { Metadata } from 'next'
import WarehousesView from './WarehousesView'

export const metadata: Metadata = { title: 'Inventory — Admin' }

export default function InventoryPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Inventory</h1>
        <p className="text-sm text-base-content/60">Manage the warehouses that hold your stock</p>
      </div>
      <WarehousesView />
    </div>
  )
}
