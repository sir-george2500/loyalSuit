import Link from 'next/link'
import {
  ShoppingCart,
  BarChart3,
  Users,
  Store,
  CreditCard,
  MonitorSmartphone,
  Warehouse,
  ArrowRight,
  CheckCircle2,
  Star,
} from 'lucide-react'

const features = [
  {
    icon: Store,
    title: 'Multi-Vendor Marketplace',
    description: 'Let third-party sellers list products in your store. Manage commissions, payouts, and approvals from one dashboard.',
  },
  {
    icon: MonitorSmartphone,
    title: 'Point of Sale (POS)',
    description: 'Full-featured POS terminal for in-store sales. Works offline, supports barcode scanning and receipt printing.',
  },
  {
    icon: Warehouse,
    title: 'Inventory & Warehouses',
    description: 'Track stock across multiple warehouses. Get low-stock alerts, barcode scanning, and bulk CSV import.',
  },
  {
    icon: CreditCard,
    title: '20+ Payment Gateways',
    description: 'Stripe, PayPal, Razorpay, Flutterwave, bKash, and more. Sell globally with local payment methods.',
  },
  {
    icon: Users,
    title: 'HRM & Payroll',
    description: 'Manage employees, track attendance, process payroll, approve leaves — all inside LoyalSuit.',
  },
  {
    icon: BarChart3,
    title: 'Reports & Analytics',
    description: 'Real-time dashboards for revenue, inventory, seller performance, and customer insights.',
  },
]

const plans = [
  {
    name: 'Basic',
    price: '$29',
    description: 'For small stores just getting started',
    features: ['Up to 500 products', '1 warehouse', '3 staff accounts', 'Email support', '5 payment gateways'],
    cta: 'Start free trial',
    highlight: false,
  },
  {
    name: 'Professional',
    price: '$79',
    description: 'For growing businesses with a marketplace',
    features: ['Unlimited products', '5 warehouses', '15 staff accounts', 'Multi-vendor marketplace', '20+ payment gateways', 'Priority support'],
    cta: 'Start free trial',
    highlight: true,
  },
  {
    name: 'Enterprise',
    price: 'Custom',
    description: 'For large operations with custom needs',
    features: ['Everything in Professional', 'Unlimited warehouses', 'Unlimited staff', 'HRM & payroll module', 'Custom integrations', 'Dedicated support'],
    cta: 'Contact sales',
    highlight: false,
  },
]

const roles = [
  {
    role: 'Business Owner',
    headline: 'Run your entire operation from one place',
    points: ['Manage inventory across warehouses', 'Track revenue and profit in real time', 'Approve vendors and manage commissions'],
    color: 'from-blue-600 to-blue-800',
  },
  {
    role: 'Marketplace Seller',
    headline: 'List products and grow your revenue',
    points: ['Set up your seller storefront in minutes', 'Manage orders and track payouts', 'Access analytics for your products'],
    color: 'from-purple-600 to-purple-800',
  },
  {
    role: 'Store Staff',
    headline: 'Handle daily operations with ease',
    points: ['Process sales at the POS terminal', 'Track attendance and leave requests', 'Manage stock and purchase orders'],
    color: 'from-green-600 to-green-800',
  },
]

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-white">
      {/* ─── Navigation ─── */}
      <header className="sticky top-0 z-50 border-b border-gray-100 bg-white/95 backdrop-blur-sm">
        <nav className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-600">
              <ShoppingCart className="h-5 w-5 text-white" />
            </div>
            <span className="text-xl font-bold text-gray-900">LoyalSuit</span>
          </div>

          <div className="hidden items-center gap-8 md:flex">
            <a href="#features" className="text-sm font-medium text-gray-600 hover:text-gray-900 transition-colors">Features</a>
            <a href="#for-who" className="text-sm font-medium text-gray-600 hover:text-gray-900 transition-colors">Who is it for?</a>
            <a href="#pricing" className="text-sm font-medium text-gray-600 hover:text-gray-900 transition-colors">Pricing</a>
          </div>

          <div className="flex items-center gap-3">
            <Link href="/login" className="text-sm font-medium text-gray-600 hover:text-gray-900 transition-colors">
              Sign in
            </Link>
            <Link href="/register" className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 transition-colors">
              Get started free
            </Link>
          </div>
        </nav>
      </header>

      {/* ─── Hero ─── */}
      <section className="relative overflow-hidden bg-gradient-to-br from-brand-950 via-brand-900 to-brand-800 px-4 pb-24 pt-20 sm:px-6 lg:px-8">
        <div className="absolute inset-0 bg-[url('data:image/svg+xml,%3Csvg width=%2260%22 height=%2260%22 viewBox=%220 0 60 60%22 xmlns=%22http://www.w3.org/2000/svg%22%3E%3Cg fill=%22none%22 fill-rule=%22evenodd%22%3E%3Cg fill=%22%23ffffff%22 fill-opacity=%220.03%22%3E%3Cpath d=%22M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z%22/%3E%3C/g%3E%3C/g%3E%3C/svg%3E')] opacity-40" />

        <div className="relative mx-auto max-w-4xl text-center">
          <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-4 py-1.5 text-sm text-white/80">
            <Star className="h-3.5 w-3.5 fill-yellow-400 text-yellow-400" />
            Now in Phase 1 — Auth & Foundation
          </div>

          <h1 className="text-4xl font-bold tracking-tight text-white sm:text-5xl lg:text-6xl">
            The Commerce Platform{' '}
            <span className="text-brand-300">Built for Growth</span>
          </h1>

          <p className="mx-auto mt-6 max-w-2xl text-lg text-brand-200">
            Run your store, marketplace, POS, inventory, and team — all from one subscription.
            No more switching between 5 different tools.
          </p>

          <div className="mt-10 flex flex-col items-center gap-4 sm:flex-row sm:justify-center">
            <Link
              href="/register"
              className="flex items-center gap-2 rounded-xl bg-white px-6 py-3 text-base font-semibold text-brand-700 shadow-lg hover:bg-brand-50 transition-colors"
            >
              Start free trial <ArrowRight className="h-4 w-4" />
            </Link>
            <Link
              href="/login"
              className="rounded-xl border border-white/30 px-6 py-3 text-base font-medium text-white hover:bg-white/10 transition-colors"
            >
              Sign in to your account
            </Link>
          </div>

          <p className="mt-4 text-sm text-brand-400">No credit card required · 14-day free trial</p>
        </div>

        {/* Stats bar */}
        <div className="relative mx-auto mt-16 max-w-4xl">
          <div className="grid grid-cols-2 gap-4 rounded-2xl border border-white/10 bg-white/5 p-6 backdrop-blur-sm sm:grid-cols-4">
            {[
              { value: '10k+', label: 'Businesses' },
              { value: '50+', label: 'Countries' },
              { value: '20+', label: 'Payment Methods' },
              { value: '99.9%', label: 'Uptime SLA' },
            ].map((s) => (
              <div key={s.label} className="text-center">
                <p className="text-2xl font-bold text-white">{s.value}</p>
                <p className="text-sm text-brand-300">{s.label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ─── Features ─── */}
      <section id="features" className="px-4 py-24 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-7xl">
          <div className="mb-16 text-center">
            <h2 className="text-3xl font-bold text-gray-900 sm:text-4xl">Everything you need, nothing you don&apos;t</h2>
            <p className="mt-4 text-lg text-gray-500">One platform to replace your store, marketplace, POS, inventory, and HR tools.</p>
          </div>

          <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-3">
            {features.map(({ icon: Icon, title, description }) => (
              <div key={title} className="group rounded-2xl border border-gray-100 p-6 shadow-sm hover:shadow-md hover:border-brand-200 transition-all">
                <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-brand-50 group-hover:bg-brand-100 transition-colors">
                  <Icon className="h-6 w-6 text-brand-600" />
                </div>
                <h3 className="mb-2 text-lg font-semibold text-gray-900">{title}</h3>
                <p className="text-sm leading-relaxed text-gray-500">{description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ─── For Who ─── */}
      <section id="for-who" className="bg-gray-50 px-4 py-24 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-7xl">
          <div className="mb-16 text-center">
            <h2 className="text-3xl font-bold text-gray-900 sm:text-4xl">Built for every role in your business</h2>
            <p className="mt-4 text-lg text-gray-500">One platform, multiple dashboards — each designed for how that person actually works.</p>
          </div>

          <div className="grid gap-6 md:grid-cols-3">
            {roles.map(({ role, headline, points, color }) => (
              <div key={role} className="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm">
                <div className={`bg-gradient-to-br ${color} p-6`}>
                  <p className="text-xs font-semibold uppercase tracking-wider text-white/70">{role}</p>
                  <h3 className="mt-2 text-xl font-bold text-white">{headline}</h3>
                </div>
                <div className="p-6">
                  <ul className="space-y-3">
                    {points.map((p) => (
                      <li key={p} className="flex items-start gap-2.5 text-sm text-gray-600">
                        <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-green-500" />
                        {p}
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ─── Pricing ─── */}
      <section id="pricing" className="px-4 py-24 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-7xl">
          <div className="mb-16 text-center">
            <h2 className="text-3xl font-bold text-gray-900 sm:text-4xl">Simple, transparent pricing</h2>
            <p className="mt-4 text-lg text-gray-500">All plans include a 14-day free trial. No credit card required.</p>
          </div>

          <div className="grid gap-8 md:grid-cols-3">
            {plans.map(({ name, price, description, features: planFeatures, cta, highlight }) => (
              <div
                key={name}
                className={`relative rounded-2xl border p-8 ${
                  highlight
                    ? 'border-brand-600 bg-brand-950 text-white shadow-xl ring-2 ring-brand-600'
                    : 'border-gray-200 bg-white shadow-sm'
                }`}
              >
                {highlight && (
                  <div className="absolute -top-4 left-1/2 -translate-x-1/2 rounded-full bg-brand-600 px-4 py-1 text-xs font-semibold text-white">
                    Most Popular
                  </div>
                )}
                <p className={`text-sm font-semibold uppercase tracking-wider ${highlight ? 'text-brand-300' : 'text-brand-600'}`}>{name}</p>
                <p className={`mt-2 text-4xl font-bold ${highlight ? 'text-white' : 'text-gray-900'}`}>
                  {price}
                  {price !== 'Custom' && <span className={`text-base font-normal ${highlight ? 'text-brand-300' : 'text-gray-400'}`}>/mo</span>}
                </p>
                <p className={`mt-2 text-sm ${highlight ? 'text-brand-200' : 'text-gray-500'}`}>{description}</p>

                <ul className="my-8 space-y-3">
                  {planFeatures.map((f) => (
                    <li key={f} className="flex items-center gap-2.5 text-sm">
                      <CheckCircle2 className={`h-4 w-4 shrink-0 ${highlight ? 'text-brand-300' : 'text-green-500'}`} />
                      <span className={highlight ? 'text-brand-100' : 'text-gray-600'}>{f}</span>
                    </li>
                  ))}
                </ul>

                <Link
                  href={name === 'Enterprise' ? '/contact' : '/register'}
                  className={`block w-full rounded-xl py-3 text-center text-sm font-semibold transition-colors ${
                    highlight
                      ? 'bg-white text-brand-700 hover:bg-brand-50'
                      : 'bg-brand-600 text-white hover:bg-brand-700'
                  }`}
                >
                  {cta}
                </Link>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ─── Final CTA ─── */}
      <section className="bg-brand-950 px-4 py-20 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-3xl text-center">
          <h2 className="text-3xl font-bold text-white sm:text-4xl">Ready to unify your commerce stack?</h2>
          <p className="mt-4 text-lg text-brand-300">
            Join thousands of businesses already running on LoyalSuit.
          </p>
          <div className="mt-8 flex flex-col items-center gap-4 sm:flex-row sm:justify-center">
            <Link href="/register" className="flex items-center gap-2 rounded-xl bg-white px-8 py-3 text-base font-semibold text-brand-700 hover:bg-brand-50 transition-colors">
              Start your free trial <ArrowRight className="h-4 w-4" />
            </Link>
            <Link href="/login" className="rounded-xl border border-white/20 px-8 py-3 text-base font-medium text-white hover:bg-white/10 transition-colors">
              Sign in
            </Link>
          </div>
        </div>
      </section>

      {/* ─── Footer ─── */}
      <footer className="border-t border-gray-100 bg-white px-4 py-10 sm:px-6 lg:px-8">
        <div className="mx-auto flex max-w-7xl flex-col items-center justify-between gap-4 sm:flex-row">
          <div className="flex items-center gap-2">
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-brand-600">
              <ShoppingCart className="h-4 w-4 text-white" />
            </div>
            <span className="font-bold text-gray-900">LoyalSuit</span>
          </div>
          <p className="text-sm text-gray-400">© {new Date().getFullYear()} LoyalSuit. All rights reserved.</p>
          <div className="flex gap-6">
            <a href="#" className="text-sm text-gray-400 hover:text-gray-600">Privacy</a>
            <a href="#" className="text-sm text-gray-400 hover:text-gray-600">Terms</a>
            <Link href="/login" className="text-sm text-gray-400 hover:text-gray-600">Sign in</Link>
          </div>
        </div>
      </footer>
    </div>
  )
}
