import type { Metadata } from 'next'

export const metadata: Metadata = { title: 'POS Terminal' }

export default function PosPage() {
  return (
    <div className="flex h-full">
      <div className="flex-1 p-6 overflow-y-auto">
        <h2 className="text-lg font-semibold mb-4">Products</h2>
        <div className="grid grid-cols-3 gap-4">
          <p className="col-span-full text-gray-400 text-center py-12">
            Select products to add to cart
          </p>
        </div>
      </div>
      <div className="w-96 bg-gray-800 border-l border-gray-700 p-6 flex flex-col">
        <h2 className="text-lg font-semibold mb-4">Current Sale</h2>
        <div className="flex-1 space-y-2">
          <p className="text-gray-400 text-sm text-center py-8">No items yet</p>
        </div>
        <div className="border-t border-gray-700 pt-4 space-y-2">
          <div className="flex justify-between font-bold text-lg">
            <span>Total</span>
            <span>$0.00</span>
          </div>
          <button className="btn btn-primary btn-block btn-lg">Charge</button>
        </div>
      </div>
    </div>
  )
}
