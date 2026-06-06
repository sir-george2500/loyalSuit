'use client'

import Link from 'next/link'
import Image from 'next/image'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Trash2, Minus, Plus, ShoppingCart, ArrowLeft, Loader2 } from 'lucide-react'
import { cartApi } from '@/lib/api/cart'
import type { CartItemView } from '@/types'

export default function CartContents({ slug }: { slug: string }) {
  const queryClient = useQueryClient()
  const cartKey = ['cart', slug]

  const { data: cart, isLoading } = useQuery({
    queryKey: cartKey,
    queryFn: async () => (await cartApi.view(slug)).data.data,
  })

  const money = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: cart?.currency || 'USD',
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: cartKey })

  const updateQty = useMutation({
    mutationFn: (vars: { item: CartItemView; quantity: number }) =>
      cartApi.updateItem(slug, {
        productId: vars.item.productId,
        variantId: vars.item.variantId ?? undefined,
        quantity: vars.quantity,
      }),
    onSuccess: invalidate,
  })

  const removeItem = useMutation({
    mutationFn: (item: CartItemView) =>
      cartApi.removeItem(slug, item.productId, item.variantId ?? undefined),
    onSuccess: invalidate,
  })

  const busy = updateQty.isPending || removeItem.isPending

  return (
    <div className="mx-auto max-w-3xl px-4 py-8 sm:px-6 lg:px-8">
      <Link href={`/store/${slug}`} className="mb-6 inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700">
        <ArrowLeft className="h-4 w-4" /> Continue shopping
      </Link>

      <h1 className="mb-6 text-2xl font-bold text-gray-900">Your cart</h1>

      {isLoading ? (
        <div className="flex justify-center py-20">
          <Loader2 className="h-6 w-6 animate-spin text-gray-400" />
        </div>
      ) : cart && cart.items.length > 0 ? (
        <>
          <ul className="divide-y divide-gray-200 rounded-lg border border-gray-200">
            {cart.items.map((item) => (
              <li key={`${item.productId}-${item.variantId ?? ''}`} className="flex items-center gap-4 p-4">
                <div className="relative h-16 w-16 shrink-0 overflow-hidden rounded bg-gray-100">
                  {item.imageUrl ? (
                    <Image src={item.imageUrl} alt={item.productName} fill sizes="64px" className="object-cover" />
                  ) : (
                    <div className="flex h-full items-center justify-center text-gray-300">
                      <ShoppingCart className="h-6 w-6" />
                    </div>
                  )}
                </div>

                <div className="min-w-0 flex-1">
                  <Link href={`/store/${slug}/products/${item.productSlug}`} className="truncate font-medium text-gray-900 hover:underline">
                    {item.productName}
                  </Link>
                  {item.variantName && <p className="text-sm text-gray-500">{item.variantName}</p>}
                  <p className="text-sm text-gray-500">{money.format(item.unitPrice)} each</p>
                </div>

                <div className="flex items-center gap-1">
                  <button
                    className="rounded border border-gray-300 p-1 hover:bg-gray-50 disabled:opacity-50"
                    disabled={busy}
                    onClick={() => updateQty.mutate({ item, quantity: item.quantity - 1 })}
                    aria-label="Decrease quantity"
                  >
                    <Minus className="h-4 w-4" />
                  </button>
                  <span className="w-8 text-center text-sm">{item.quantity}</span>
                  <button
                    className="rounded border border-gray-300 p-1 hover:bg-gray-50 disabled:opacity-50"
                    disabled={busy}
                    onClick={() => updateQty.mutate({ item, quantity: item.quantity + 1 })}
                    aria-label="Increase quantity"
                  >
                    <Plus className="h-4 w-4" />
                  </button>
                </div>

                <div className="w-20 text-right font-medium text-gray-900">{money.format(item.lineTotal)}</div>

                <button
                  className="text-gray-400 hover:text-red-600 disabled:opacity-50"
                  disabled={busy}
                  onClick={() => removeItem.mutate(item)}
                  aria-label="Remove item"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </li>
            ))}
          </ul>

          <div className="mt-6 flex items-center justify-between border-t border-gray-200 pt-4">
            <span className="text-lg font-medium text-gray-900">Subtotal</span>
            <span className="text-xl font-bold text-gray-900">{money.format(cart.subtotal)}</span>
          </div>

          <Link
            href={`/store/${slug}/checkout`}
            className="mt-4 block w-full rounded-lg bg-gray-900 px-4 py-3 text-center font-medium text-white hover:bg-gray-800"
          >
            Proceed to checkout
          </Link>
        </>
      ) : (
        <div className="flex flex-col items-center gap-3 py-20 text-center text-gray-400">
          <ShoppingCart className="h-12 w-12" />
          <p>Your cart is empty.</p>
          <Link href={`/store/${slug}`} className="text-gray-700 underline">
            Browse products
          </Link>
        </div>
      )}
    </div>
  )
}
