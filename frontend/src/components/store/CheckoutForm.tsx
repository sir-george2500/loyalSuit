'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import type { AxiosError } from 'axios'
import { ArrowLeft, Loader2, CheckCircle2, Banknote, Truck, Ticket, X } from 'lucide-react'
import { cartApi } from '@/lib/api/cart'
import { checkoutApi, type CheckoutPayload } from '@/lib/api/checkout'
import { storePickupApi } from '@/lib/api/pickup'
import { storeCouponApi } from '@/lib/api/coupons'
import { loyaltyApi } from '@/lib/api/loyalty'
import type { CouponPreview, OrderResponse } from '@/types'

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

  const { data: pickupPoints } = useQuery({
    queryKey: ['store-pickup-points', slug],
    queryFn: async () => (await storePickupApi.list(slug)).data.data,
  })

  // Only points that actually have a zone can price a delivery.
  const deliverablePoints = (pickupPoints ?? []).filter((p) => p.zones.length > 0)
  const hasDelivery = deliverablePoints.length > 0
  const [pointId, setPointId] = useState('')
  const [zoneId, setZoneId] = useState('')

  // Auto-pick the only point so the customer just chooses a zone.
  useEffect(() => {
    if (deliverablePoints.length === 1 && pointId === '') setPointId(deliverablePoints[0].id)
  }, [deliverablePoints, pointId])

  const selectedPoint = deliverablePoints.find((p) => p.id === pointId)
  const selectedZone = selectedPoint?.zones.find((z) => z.id === zoneId)
  const shipping = selectedZone?.fee ?? 0

  const [couponInput, setCouponInput] = useState('')
  const [coupon, setCoupon] = useState<CouponPreview | null>(null)
  const [couponError, setCouponError] = useState<string | null>(null)
  const couponDiscount = coupon?.discountAmount ?? 0

  // Loyalty points: the endpoint is the auth gate — it only resolves for a signed-in customer.
  const { data: loyalty } = useQuery({
    queryKey: ['loyalty-me'],
    queryFn: async () => (await loyaltyApi.me()).data.data,
    retry: false,
  })
  const [usePoints, setUsePoints] = useState(false)
  const pointValue = loyalty && loyalty.points > 0 ? loyalty.redeemableValue / loyalty.points : 0.01
  const affordable = Math.max((cart?.subtotal ?? 0) - couponDiscount, 0)
  const pointsToUse = usePoints && loyalty ? Math.min(loyalty.points, Math.floor(affordable / pointValue)) : 0
  const pointsDiscount = pointsToUse * pointValue
  const discount = couponDiscount + pointsDiscount

  const applyCoupon = useMutation({
    mutationFn: () => storeCouponApi.preview(slug, couponInput.trim()),
    onSuccess: ({ data }) => { setCoupon(data.data); setCouponError(null) },
    onError: (err) =>
      setCouponError((err as AxiosError<{ message?: string }>)?.response?.data?.message ?? 'That code isn’t valid.'),
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
        <form
          onSubmit={handleSubmit((data) => {
            setServerError(null)
            place.mutate({
              ...data,
              deliveryZoneId: zoneId || undefined,
              couponCode: coupon?.code || undefined,
              pointsToRedeem: pointsToUse || undefined,
            })
          })}
          noValidate
          className="space-y-4"
        >
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

          {hasDelivery && (
            <div className="rounded-lg border border-gray-200 p-4">
              <div className="mb-2 flex items-center gap-2 text-sm font-medium text-gray-700">
                <Truck className="h-4 w-4" /> Delivery
              </div>
              {deliverablePoints.length > 1 && (
                <select
                  value={pointId}
                  onChange={(e) => { setPointId(e.target.value); setZoneId('') }}
                  className={`${input(false)} mb-2`}
                >
                  <option value="" disabled>Choose a pickup point…</option>
                  {deliverablePoints.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
                </select>
              )}
              {selectedPoint && (
                <div className="space-y-1">
                  {selectedPoint.zones.map((z) => (
                    <label
                      key={z.id}
                      className={`flex cursor-pointer items-center justify-between rounded-lg border px-3 py-2 text-sm ${zoneId === z.id ? 'border-gray-900 bg-gray-50' : 'border-gray-200'}`}
                    >
                      <span className="flex items-center gap-2">
                        <input type="radio" name="deliveryZone" checked={zoneId === z.id} onChange={() => setZoneId(z.id)} />
                        {z.name}
                      </span>
                      <span className="font-medium">{money.format(z.fee)}</span>
                    </label>
                  ))}
                </div>
              )}
            </div>
          )}

          <div className="rounded-lg border border-gray-200 p-4">
            <div className="mb-2 flex items-center gap-2 text-sm font-medium text-gray-700">
              <Ticket className="h-4 w-4" /> Discount code
            </div>
            {coupon ? (
              <div className="flex items-center justify-between rounded-lg bg-green-50 px-3 py-2 text-sm text-green-800">
                <span><span className="font-mono font-medium">{coupon.code}</span> applied — {money.format(discount)} off</span>
                <button
                  type="button"
                  onClick={() => { setCoupon(null); setCouponInput(''); setCouponError(null) }}
                  className="text-green-700 hover:text-green-900"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
            ) : (
              <div className="flex gap-2">
                <input
                  value={couponInput}
                  onChange={(e) => { setCouponInput(e.target.value); setCouponError(null) }}
                  placeholder="Enter code"
                  className={`${input(false)} uppercase`}
                />
                <button
                  type="button"
                  disabled={!couponInput.trim() || applyCoupon.isPending}
                  onClick={() => applyCoupon.mutate()}
                  className="rounded-lg border border-gray-300 px-4 text-sm font-medium hover:bg-gray-50 disabled:opacity-60"
                >
                  {applyCoupon.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : 'Apply'}
                </button>
              </div>
            )}
            {couponError && <p className="mt-1 text-xs text-red-600">{couponError}</p>}
          </div>

          {loyalty && loyalty.points > 0 && (
            <label className="flex cursor-pointer items-center justify-between rounded-lg border border-gray-200 p-4 text-sm">
              <span className="flex items-center gap-2">
                <input type="checkbox" checked={usePoints} onChange={(e) => setUsePoints(e.target.checked)} />
                Redeem my {loyalty.points} loyalty points
              </span>
              <span className="text-gray-600">
                {pointsToUse > 0 ? `−${money.format(pointsDiscount)}` : `worth ${money.format(loyalty.redeemableValue)}`}
              </span>
            </label>
          )}

          <Field label="Order notes" hint="optional">
            <textarea {...register('notes')} rows={2} className={input(false)} />
          </Field>

          {hasDelivery && !selectedZone && (
            <p className="text-sm text-gray-500">Choose a delivery zone to continue.</p>
          )}
          <button
            type="submit"
            disabled={place.isPending || (hasDelivery && !selectedZone)}
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
          <div className="mt-4 space-y-1 border-t border-gray-200 pt-3 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-600">Subtotal</span>
              <span>{money.format(cart?.subtotal ?? 0)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Shipping</span>
              <span>{selectedZone ? money.format(shipping) : hasDelivery ? 'Select a zone' : 'Free'}</span>
            </div>
            {couponDiscount > 0 && (
              <div className="flex justify-between text-green-700">
                <span>Discount{coupon ? ` (${coupon.code})` : ''}</span>
                <span>−{money.format(couponDiscount)}</span>
              </div>
            )}
            {pointsDiscount > 0 && (
              <div className="flex justify-between text-green-700">
                <span>Points ({pointsToUse})</span>
                <span>−{money.format(pointsDiscount)}</span>
              </div>
            )}
            <div className="flex justify-between border-t border-gray-200 pt-2 text-base font-semibold">
              <span>Total</span>
              <span>{money.format(Math.max((cart?.subtotal ?? 0) + shipping - discount, 0))}</span>
            </div>
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
