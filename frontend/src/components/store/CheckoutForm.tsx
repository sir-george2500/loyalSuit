'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import type { AxiosError } from 'axios'
import { ArrowLeft, Loader2, CheckCircle2, Banknote } from 'lucide-react'
import { cartApi } from '@/lib/api/cart'
import { checkoutApi, type CheckoutPayload } from '@/lib/api/checkout'
import type { OrderResponse } from '@/types'

export default function CheckoutForm({ slug }: { slug: string }) {
  const queryClient = useQueryClient()
  const [order, setOrder] = useState<OrderResponse | null>(null)
  const [serverError, setServerError] = useState<string | null>(null)
  // One idempotency key per checkout attempt: reused on retry so a double-submit
  // can never create two orders.
  const [idempotencyKey] = useState(() => crypto.randomUUID())

  const { data: cart, isLoading } = useQuery({
    queryKey: ['cart', slug],
    queryFn: async () => (await cartApi.view(slug)).data.data,
  })

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CheckoutPayload>()

  const place = useMutation({
    mutationFn: (payload: CheckoutPayload) => checkoutApi.placeOrder(slug, payload, idempotencyKey),
    onSuccess: ({ data }) => {
      setOrder(data.data)
      queryClient.invalidateQueries({ queryKey: ['cart', slug] })
    },
    onError: (err) =>
      setServerError(
        (err as AxiosError<{ message?: string }>)?.response?.data?.message ??
          'We couldn’t place your order. Please try again.'
      ),
  })

  const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: cart?.currency || 'USD' })

  // ---- confirmation -------------------------------------------------------
  if (order) {
    return (
      <div className="mx-auto max-w-lg px-4 py-16 text-center">
        <CheckCircle2 className="mx-auto h-14 w-14 text-green-600" />
        <h1 className="mt-4 text-2xl font-bold text-gray-900">Order placed</h1>
        <p className="mt-1 text-gray-600">
          Thanks, {order.customerName}. Your order is <span className="font-mono font-medium">{order.orderNumber}</span>.
        </p>
        <div className="mt-6 rounded-lg border border-gray-200 p-4 text-left">
          <div className="flex justify-between">
            <span className="text-gray-600">Total</span>
            <span className="font-semibold">
              {new Intl.NumberFormat('en-US', { style: 'currency', currency: order.currency }).format(order.total)}
            </span>
          </div>
          <div className="mt-2 flex items-center gap-2 rounded bg-amber-50 px-3 py-2 text-sm text-amber-800">
            <Banknote className="h-4 w-4" /> Pay with cash on delivery.
          </div>
        </div>
        <Link href={`/store/${slug}`} className="mt-6 inline-block text-gray-700 underline">
          Continue shopping
        </Link>
      </div>
    )
  }

  // ---- empty cart guard ---------------------------------------------------
  if (!isLoading && (!cart || cart.items.length === 0)) {
    return (
      <div className="mx-auto max-w-lg px-4 py-16 text-center text-gray-500">
        <p>Your cart is empty.</p>
        <Link href={`/store/${slug}`} className="mt-2 inline-block text-gray-700 underline">
          Browse products
        </Link>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8 sm:px-6 lg:px-8">
      <Link href={`/store/${slug}/cart`} className="mb-6 inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700">
        <ArrowLeft className="h-4 w-4" /> Back to cart
      </Link>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">Checkout</h1>

      {serverError && (
        <div role="alert" className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          {serverError}
        </div>
      )}

      <div className="grid gap-8 md:grid-cols-[1fr_18rem]">
        <form onSubmit={handleSubmit((data) => { setServerError(null); place.mutate(data) })} noValidate className="space-y-4">
          <Field label="Full name" error={errors.customerName?.message}>
            <input {...register('customerName', { required: 'Name is required' })} className={input(!!errors.customerName)} />
          </Field>

          <div className="grid grid-cols-2 gap-4">
            <Field label="Email" hint="optional" error={errors.customerEmail?.message}>
              <input
                type="email"
                {...register('customerEmail', {
                  pattern: { value: /^[^@\s]+@[^@\s]+\.[^@\s]+$/, message: 'Enter a valid email' },
                })}
                className={input(!!errors.customerEmail)}
              />
            </Field>
            <Field label="Phone" hint="optional">
              <input {...register('customerPhone')} className={input(false)} />
            </Field>
          </div>

          <Field label="Address" error={errors.addressLine1?.message}>
            <input {...register('addressLine1', { required: 'A delivery address is required' })} className={input(!!errors.addressLine1)} placeholder="Street address" />
          </Field>
          <input {...register('addressLine2')} className={`${input(false)}`} placeholder="Apartment, suite (optional)" />

          <div className="grid grid-cols-2 gap-4">
            <Field label="City" error={errors.city?.message}>
              <input {...register('city', { required: 'City is required' })} className={input(!!errors.city)} />
            </Field>
            <Field label="State / region" hint="optional">
              <input {...register('state')} className={input(false)} />
            </Field>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Postal code" hint="optional">
              <input {...register('postalCode')} className={input(false)} />
            </Field>
            <Field label="Country" hint="optional">
              <input {...register('country')} className={input(false)} />
            </Field>
          </div>

          <Field label="Order notes" hint="optional">
            <textarea {...register('notes')} rows={2} className={input(false)} />
          </Field>

          <button
            type="submit"
            disabled={place.isPending}
            className="flex w-full items-center justify-center gap-2 rounded-lg bg-gray-900 px-4 py-3 font-medium text-white hover:bg-gray-800 disabled:opacity-60"
          >
            {place.isPending ? <Loader2 className="h-5 w-5 animate-spin" /> : <Banknote className="h-5 w-5" />}
            Place order — pay cash on delivery
          </button>
        </form>

        {/* Order summary */}
        <aside className="h-fit rounded-lg border border-gray-200 p-4">
          <h2 className="mb-3 font-medium text-gray-900">Order summary</h2>
          {cart && (
            <ul className="space-y-2 text-sm">
              {cart.items.map((item) => (
                <li key={`${item.productId}-${item.variantId ?? ''}`} className="flex justify-between gap-2">
                  <span className="truncate text-gray-600">
                    {item.productName}
                    {item.variantName ? ` · ${item.variantName}` : ''} × {item.quantity}
                  </span>
                  <span className="shrink-0 text-gray-900">{money.format(item.lineTotal)}</span>
                </li>
              ))}
            </ul>
          )}
          <div className="mt-4 flex justify-between border-t border-gray-200 pt-3 font-semibold">
            <span>Total</span>
            <span>{money.format(cart?.subtotal ?? 0)}</span>
          </div>
        </aside>
      </div>
    </div>
  )
}

function input(hasError: boolean): string {
  return `w-full rounded-lg border px-3 py-2 ${hasError ? 'border-red-400' : 'border-gray-300'}`
}

function Field({
  label,
  hint,
  error,
  children,
}: {
  label: string
  hint?: string
  error?: string
  children: React.ReactNode
}) {
  return (
    <label className="block">
      <span className="text-sm font-medium text-gray-700">
        {label}
        {hint && <span className="ml-1 text-xs font-normal text-gray-400">({hint})</span>}
      </span>
      <div className="mt-1">{children}</div>
      {error && <span className="mt-1 block text-xs text-red-600">{error}</span>}
    </label>
  )
}
