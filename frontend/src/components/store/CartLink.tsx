'use client'

import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { ShoppingCart } from 'lucide-react'
import { cartApi } from '@/lib/api/cart'

/** Cart icon with a live item-count badge, linking to the store's cart page. */
export default function CartLink({ slug }: { slug: string }) {
  const { data } = useQuery({
    queryKey: ['cart', slug],
    queryFn: async () => (await cartApi.view(slug)).data.data,
  })

  const count = data?.itemCount ?? 0

  return (
    <Link href={`/store/${slug}/cart`} className="relative inline-flex items-center text-gray-600 hover:text-gray-900" aria-label="Cart">
      <ShoppingCart className="h-6 w-6" />
      {count > 0 && (
        <span className="absolute -right-2 -top-2 flex h-5 min-w-[1.25rem] items-center justify-center rounded-full bg-gray-900 px-1 text-xs font-medium text-white">
          {count}
        </span>
      )}
    </Link>
  )
}
