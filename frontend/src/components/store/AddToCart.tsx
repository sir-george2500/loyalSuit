'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ShoppingCart, Check, Loader2 } from 'lucide-react'
import type { AxiosError } from 'axios'
import { cartApi } from '@/lib/api/cart'
import type { StoreVariant } from '@/types'

export default function AddToCart({
  slug,
  productId,
  variants,
  inStock,
}: {
  slug: string
  productId: string
  variants: StoreVariant[]
  inStock: boolean
}) {
  const queryClient = useQueryClient()
  const [variantId, setVariantId] = useState<string>('')
  const [quantity, setQuantity] = useState(1)
  const [added, setAdded] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const add = useMutation({
    mutationFn: () =>
      cartApi.addItem(slug, { productId, variantId: variantId || undefined, quantity }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cart', slug] })
      setError(null)
      setAdded(true)
      setTimeout(() => setAdded(false), 2500)
    },
    onError: (err) =>
      setError(
        (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? 'Could not add to cart.'
      ),
  })

  if (!inStock) {
    return (
      <button disabled className="mt-6 w-full rounded-lg bg-gray-200 px-4 py-3 font-medium text-gray-500">
        Out of stock
      </button>
    )
  }

  return (
    <div className="mt-6 space-y-3">
      {variants.length > 0 && (
        <label className="block">
          <span className="text-sm font-medium text-gray-700">Option</span>
          <select
            value={variantId}
            onChange={(e) => setVariantId(e.target.value)}
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2"
          >
            <option value="">Default</option>
            {variants.map((v) => (
              <option key={v.id} value={v.id}>
                {v.name}
              </option>
            ))}
          </select>
        </label>
      )}

      <div className="flex items-center gap-3">
        <input
          type="number"
          min={1}
          value={quantity}
          onChange={(e) => setQuantity(Math.max(1, Number(e.target.value) || 1))}
          className="w-20 rounded-lg border border-gray-300 px-3 py-2 text-center"
          aria-label="Quantity"
        />
        <button
          onClick={() => add.mutate()}
          disabled={add.isPending}
          className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-gray-900 px-4 py-3 font-medium text-white hover:bg-gray-800 disabled:opacity-60"
        >
          {add.isPending ? (
            <Loader2 className="h-5 w-5 animate-spin" />
          ) : added ? (
            <Check className="h-5 w-5" />
          ) : (
            <ShoppingCart className="h-5 w-5" />
          )}
          {added ? 'Added' : 'Add to cart'}
        </button>
      </div>

      {error && <p className="text-sm text-red-600">{error}</p>}

      {added && (
        <Link href={`/store/${slug}/cart`} className="block text-center text-sm text-gray-600 underline">
          View cart
        </Link>
      )}
    </div>
  )
}
