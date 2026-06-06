'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import type { AxiosError } from 'axios'
import { ArrowLeft, Loader2, PackageSearch, Undo2, CheckCircle2 } from 'lucide-react'
import { orderTrackingApi } from '@/lib/api/orderTracking'
import { returnsApi } from '@/lib/api/returns'
import type { OrderResponse } from '@/types'

interface LookupForm {
  orderNumber: string
  email: string
}

const STATUS_TONE: Record<string, string> = {
  PENDING: 'bg-gray-100 text-gray-700',
  CONFIRMED: 'bg-blue-100 text-blue-700',
  PROCESSING: 'bg-amber-100 text-amber-700',
  SHIPPED: 'bg-indigo-100 text-indigo-700',
  DELIVERED: 'bg-green-100 text-green-700',
  CANCELLED: 'bg-red-100 text-red-700',
  REFUNDED: 'bg-gray-200 text-gray-700',
}

export default function TrackOrder({ slug }: { slug: string }) {
  const [order, setOrder] = useState<OrderResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  const { register, handleSubmit, getValues } = useForm<LookupForm>()

  const lookup = useMutation({
    mutationFn: (form: LookupForm) => orderTrackingApi.track(slug, form.orderNumber.trim(), form.email.trim()),
    onSuccess: ({ data }) => { setOrder(data.data); setError(null) },
    onError: (err) => {
      setOrder(null)
      const status = (err as AxiosError)?.response?.status
      setError(
        status === 404
          ? 'No order matches that number and email. Double-check both.'
          : 'Something went wrong. Please try again.'
      )
    },
  })

  const money = order
    ? new Intl.NumberFormat('en-US', { style: 'currency', currency: order.currency })
    : null

  return (
    <div className="mx-auto max-w-lg px-4 py-8 sm:px-6 lg:px-8">
      <Link href={`/store/${slug}`} className="mb-6 inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700">
        <ArrowLeft className="h-4 w-4" /> Back to store
      </Link>
      <h1 className="mb-1 text-2xl font-bold text-gray-900">Track your order</h1>
      <p className="mb-6 text-sm text-gray-500">Enter your order number and the email you used at checkout.</p>

      <form onSubmit={handleSubmit((f) => lookup.mutate(f))} className="space-y-3">
        <input
          {...register('orderNumber', { required: true })}
          placeholder="Order number (e.g. ORD-XXXX)"
          className="w-full rounded-lg border border-gray-300 px-3 py-2 font-mono"
        />
        <input
          {...register('email', { required: true })}
          type="email"
          placeholder="Email used at checkout"
          className="w-full rounded-lg border border-gray-300 px-3 py-2"
        />
        <button
          type="submit"
          disabled={lookup.isPending}
          className="flex w-full items-center justify-center gap-2 rounded-lg bg-gray-900 px-4 py-3 font-medium text-white hover:bg-gray-800 disabled:opacity-60"
        >
          {lookup.isPending ? <Loader2 className="h-5 w-5 animate-spin" /> : <PackageSearch className="h-5 w-5" />}
          Find my order
        </button>
      </form>

      {error && <p className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}

      {order && money && (
        <div className="mt-6 rounded-lg border border-gray-200 p-4">
          <div className="flex items-center justify-between">
            <span className="font-mono text-sm text-gray-500">{order.orderNumber}</span>
            <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${STATUS_TONE[order.status] ?? 'bg-gray-100 text-gray-700'}`}>
              {order.status.charAt(0) + order.status.slice(1).toLowerCase()}
            </span>
          </div>
          <p className="mt-2 text-sm text-gray-600">
            Payment: {order.paymentStatus === 'PAID' ? 'Paid' : order.paymentStatus === 'REFUNDED' ? 'Refunded' : 'Cash on delivery (unpaid)'}
          </p>
          <ul className="mt-3 space-y-1 border-t border-gray-100 pt-3 text-sm">
            {order.items.map((it, i) => (
              <li key={i} className="flex justify-between text-gray-600">
                <span>{it.quantity} × {money.format(it.unitPrice)}</span>
                <span>{money.format(it.total)}</span>
              </li>
            ))}
          </ul>
          <div className="mt-3 flex justify-between border-t border-gray-100 pt-3 font-semibold">
            <span>Total</span>
            <span>{money.format(order.total)}</span>
          </div>

          {order.status === 'DELIVERED' && (
            <ReturnRequestPanel slug={slug} orderNumber={order.orderNumber} email={getValues('email').trim()} />
          )}
        </div>
      )}
    </div>
  )
}

function ReturnRequestPanel({ slug, orderNumber, email }: { slug: string; orderNumber: string; email: string }) {
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState('')
  const [done, setDone] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const submit = useMutation({
    mutationFn: () => returnsApi.request(slug, orderNumber, { email, reason: reason.trim() }),
    onSuccess: () => { setDone(true); setError(null) },
    onError: (err) =>
      setError((err as AxiosError<{ message?: string }>)?.response?.data?.message ?? 'Could not submit the return.'),
  })

  if (done) {
    return (
      <div className="mt-4 flex items-center gap-2 rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">
        <CheckCircle2 className="h-4 w-4" /> Return requested. The store will review it.
      </div>
    )
  }

  return (
    <div className="mt-4 border-t border-gray-100 pt-3">
      {!open ? (
        <button onClick={() => setOpen(true)} className="inline-flex items-center gap-1 text-sm text-gray-600 underline">
          <Undo2 className="h-4 w-4" /> Request a return
        </button>
      ) : (
        <div className="space-y-2">
          <textarea
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={2}
            placeholder="Why are you returning this order?"
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
          />
          {error && <p className="text-sm text-red-600">{error}</p>}
          <button
            onClick={() => submit.mutate()}
            disabled={submit.isPending || reason.trim().length === 0}
            className="flex items-center justify-center gap-2 rounded-lg bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-800 disabled:opacity-60"
          >
            {submit.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
            Submit return request
          </button>
        </div>
      )}
    </div>
  )
}
