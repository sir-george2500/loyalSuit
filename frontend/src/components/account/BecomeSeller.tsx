'use client'

import Link from 'next/link'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import type { AxiosError } from 'axios'
import { Store, Loader2, Clock, CheckCircle2, XCircle, Ban } from 'lucide-react'
import { vendorApi, type ApplyVendorPayload } from '@/lib/api/vendors'
import type { VendorStatus } from '@/types'

const STATUS_VIEW: Record<VendorStatus, { icon: React.ReactNode; title: string; body: string }> = {
  PENDING: {
    icon: <Clock className="h-10 w-10 text-amber-500" />,
    title: 'Application under review',
    body: 'Thanks for applying. An admin will review your store shortly.',
  },
  ACTIVE: {
    icon: <CheckCircle2 className="h-10 w-10 text-green-600" />,
    title: 'You’re an approved seller',
    body: 'Your store is active. Head to the seller area to manage your products.',
  },
  SUSPENDED: {
    icon: <Ban className="h-10 w-10 text-red-600" />,
    title: 'Your store is suspended',
    body: 'Contact the store admin for details.',
  },
  REJECTED: {
    icon: <XCircle className="h-10 w-10 text-gray-500" />,
    title: 'Application not approved',
    body: 'Your vendor application was not approved.',
  },
}

export default function BecomeSeller() {
  const queryClient = useQueryClient()

  const { data: vendor, isLoading } = useQuery({
    queryKey: ['vendor-me'],
    // 404 simply means "not a vendor yet" — don't retry it into an error spinner.
    queryFn: async () => (await vendorApi.me()).data.data,
    retry: false,
  })

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ApplyVendorPayload>()

  const apply = useMutation({
    mutationFn: (payload: ApplyVendorPayload) => vendorApi.apply(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['vendor-me'] }),
  })

  const content = () => {
    if (isLoading) {
      return <div className="flex justify-center py-10"><Loader2 className="h-6 w-6 animate-spin text-primary" /></div>
    }

    if (vendor) {
      const view = STATUS_VIEW[vendor.status]
      return (
        <div className="text-center">
          <div className="mx-auto mb-3 flex justify-center">{view.icon}</div>
          <h2 className="text-xl font-bold">{view.title}</h2>
          <p className="mt-1 text-sm text-base-content/60">{view.body}</p>
          {vendor.status === 'ACTIVE' && (
            <Link href="/seller/dashboard" className="btn btn-primary btn-sm mt-4">Go to seller area</Link>
          )}
        </div>
      )
    }

    return (
      <>
        <div className="mb-4 text-center">
          <Store className="mx-auto h-10 w-10 text-primary" />
          <h2 className="mt-2 text-xl font-bold">Sell on Loyal Spare Parts</h2>
          <p className="text-sm text-base-content/60">Open your own store on the Loyal Spare Parts marketplace. An admin reviews each application.</p>
        </div>
        {apply.isError && (
          <div role="alert" className="alert alert-error mb-3 text-sm">
            <span>{(apply.error as AxiosError<{ message?: string }>)?.response?.data?.message ?? 'Could not submit your application.'}</span>
          </div>
        )}
        <form onSubmit={handleSubmit((data) => apply.mutate(data))} noValidate className="space-y-3">
          <div className="form-control">
            <label className="label" htmlFor="storeName"><span className="label-text font-medium">Store name</span></label>
            <input
              id="storeName"
              {...register('storeName', { required: 'Store name is required' })}
              className={`input input-bordered w-full ${errors.storeName ? 'input-error' : ''}`}
              placeholder="Jane's Crafts"
            />
            {errors.storeName && <span className="mt-1 text-xs text-error">{errors.storeName.message}</span>}
          </div>
          <div className="form-control">
            <label className="label" htmlFor="description">
              <span className="label-text font-medium">About your store</span>
              <span className="label-text-alt text-base-content/40">optional</span>
            </label>
            <textarea id="description" {...register('description')} rows={3} className="textarea textarea-bordered w-full" />
          </div>
          <button type="submit" disabled={apply.isPending} className="btn btn-primary w-full">
            {apply.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
            Submit application
          </button>
        </form>
      </>
    )
  }

  return (
    <div className="mx-auto flex min-h-screen max-w-md items-center justify-center p-4">
      <div className="card w-full bg-base-100 shadow-xl">
        <div className="card-body">{content()}</div>
      </div>
    </div>
  )
}
