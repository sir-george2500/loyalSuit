'use client'

import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { Package, Store, ShoppingCart, CheckCircle2, Circle, Clock, Ban, XCircle, ArrowRight, Loader2 } from 'lucide-react'
import { vendorApi } from '@/lib/api/vendors'
import { productApi } from '@/lib/api/catalog'
import { sellerOrderApi } from '@/lib/api/sellerOrders'
import type { Vendor, VendorStatus } from '@/types'

const STATUS_VIEW: Record<VendorStatus, { label: string; badge: string; icon: typeof Clock }> = {
  PENDING: { label: 'Application under review', badge: 'badge-warning', icon: Clock },
  ACTIVE: { label: 'Approved seller', badge: 'badge-success', icon: CheckCircle2 },
  SUSPENDED: { label: 'Suspended', badge: 'badge-error', icon: Ban },
  REJECTED: { label: 'Not approved', badge: 'badge-ghost', icon: XCircle },
}

export default function SellerDashboardView() {
  // 404 from /vendor/me just means "hasn't applied yet" — don't treat it as an error.
  const { data: vendor, isLoading: vendorLoading } = useQuery<Vendor | null>({
    queryKey: ['vendor-me'],
    queryFn: async () => {
      try {
        return (await vendorApi.me()).data.data
      } catch {
        return null
      }
    },
    retry: false,
  })

  const isApproved = vendor?.status === 'ACTIVE'

  const { data: products } = useQuery({
    queryKey: ['seller-products', 0],
    queryFn: async () => (await productApi.list(0)).data.data,
    enabled: !!vendor, // only a vendor has a catalogue
  })

  const { data: orders } = useQuery({
    queryKey: ['seller-orders', 0],
    queryFn: async () => (await sellerOrderApi.list(0)).data.data,
    enabled: isApproved, // orders only exist once they're selling
  })

  const productCount = products?.totalElements ?? 0
  const orderCount = orders?.totalElements ?? 0
  const status = vendor ? STATUS_VIEW[vendor.status] : null

  const steps = [
    { label: 'Create your account', done: true },
    { label: 'Apply to sell on Loyal Spare Parts', done: !!vendor },
    { label: 'Get approved by an admin', done: isApproved },
    { label: 'Add your first product', done: productCount > 0 },
  ]

  if (vendorLoading) {
    return (
      <div className="flex justify-center py-16">
        <Loader2 className="h-6 w-6 animate-spin text-primary" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Stat cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="card bg-base-100 shadow-sm">
          <div className="card-body p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <Store className="h-5 w-5" />
              </div>
              <div>
                <p className="text-sm text-base-content/60">Store</p>
                <p className="truncate text-lg font-semibold">{vendor?.storeName ?? 'Not a seller yet'}</p>
              </div>
            </div>
          </div>
        </div>

        <div className="card bg-base-100 shadow-sm">
          <div className="card-body p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-secondary/10 text-secondary">
                <Package className="h-5 w-5" />
              </div>
              <div>
                <p className="text-sm text-base-content/60">Products</p>
                <p className="text-lg font-semibold">{vendor ? productCount : '—'}</p>
              </div>
            </div>
          </div>
        </div>

        <div className="card bg-base-100 shadow-sm">
          <div className="card-body p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-accent/10 text-accent">
                <ShoppingCart className="h-5 w-5" />
              </div>
              <div>
                <p className="text-sm text-base-content/60">Orders</p>
                <p className="text-lg font-semibold">{isApproved ? orderCount : '—'}</p>
              </div>
            </div>
          </div>
        </div>

        <div className="card bg-base-100 shadow-sm">
          <div className="card-body p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-base-200 text-base-content/60">
                {status ? <status.icon className="h-5 w-5" /> : <Circle className="h-5 w-5" />}
              </div>
              <div>
                <p className="text-sm text-base-content/60">Status</p>
                {status ? (
                  <span className={`badge ${status.badge} badge-sm`}>{status.label}</span>
                ) : (
                  <p className="text-lg font-semibold text-base-content/40">—</p>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Primary action depends on where the seller is in the journey */}
      {!vendor && (
        <div className="alert bg-primary/5">
          <Store className="h-5 w-5 text-primary" />
          <span>You haven’t opened a store yet. Apply to start selling on Loyal Spare Parts.</span>
          <Link href="/become-seller" className="btn btn-primary btn-sm">Apply to sell</Link>
        </div>
      )}
      {isApproved && (
        <div className="alert bg-success/5">
          <CheckCircle2 className="h-5 w-5 text-success" />
          <span>Your store is live. Manage your products and they’ll appear in the marketplace.</span>
          <Link href="/seller/products" className="btn btn-primary btn-sm gap-1">
            Manage products <ArrowRight className="h-4 w-4" />
          </Link>
        </div>
      )}

      {/* Onboarding checklist — reflects real progress */}
      <div className="card bg-base-100 shadow-sm">
        <div className="card-body">
          <h2 className="text-lg font-semibold">Your seller journey</h2>
          <ul className="mt-2 space-y-2">
            {steps.map((step) => (
              <li key={step.label} className="flex items-center gap-3">
                {step.done ? (
                  <CheckCircle2 className="h-5 w-5 text-success" />
                ) : (
                  <Circle className="h-5 w-5 text-base-content/30" />
                )}
                <span className={`text-sm ${step.done ? 'text-base-content/80' : 'text-base-content/50'}`}>
                  {step.label}
                </span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}
