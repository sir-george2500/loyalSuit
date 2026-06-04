import type { Metadata } from 'next'
import { Package, ShoppingCart, Wallet, Rocket } from 'lucide-react'

export const metadata: Metadata = { title: 'Seller Dashboard' }

const STEPS = [
  { label: 'Complete your store profile', done: false },
  { label: 'Add your first product', done: false },
  { label: 'Get approved to sell', done: false },
  { label: 'Receive your first order', done: false },
]

const PLACEHOLDER_STATS = [
  { label: 'Products', icon: Package },
  { label: 'Open orders', icon: ShoppingCart },
  { label: 'Available balance', icon: Wallet },
]

export default function SellerDashboardPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Your Store</h1>
        <p className="text-sm text-base-content/60">
          Sell your products inside the marketplace, track orders, and request payouts.
        </p>
      </div>

      {/* Honest placeholders — no fabricated numbers. These light up once the
          marketplace domain ships (see PHASES.md, Phase 5). */}
      <div className="grid gap-4 sm:grid-cols-3">
        {PLACEHOLDER_STATS.map(({ label, icon: Icon }) => (
          <div key={label} className="card bg-base-100 shadow-sm">
            <div className="card-body p-5">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-base-200">
                  <Icon className="h-5 w-5 text-base-content/50" />
                </div>
                <div>
                  <p className="text-sm text-base-content/60">{label}</p>
                  <p className="text-lg font-semibold text-base-content/40">—</p>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Onboarding checklist */}
      <div className="card bg-base-100 shadow-sm">
        <div className="card-body">
          <div className="flex items-start gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-info/10 text-info">
              <Rocket className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-lg font-semibold">Get set up to sell</h2>
              <p className="text-sm text-base-content/60">
                Vendor tools are being rolled out. Here&apos;s what onboarding will look like.
              </p>
            </div>
          </div>

          <ul className="mt-4 space-y-2">
            {STEPS.map((step, i) => (
              <li key={step.label} className="flex items-center gap-3">
                <span className="flex h-6 w-6 items-center justify-center rounded-full border border-base-300 text-xs font-medium text-base-content/50">
                  {i + 1}
                </span>
                <span className="text-sm text-base-content/70">{step.label}</span>
                <span className="badge badge-ghost badge-xs ml-auto">soon</span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}
