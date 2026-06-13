import Link from 'next/link'
import {
  ShieldCheck,
  BadgeCheck,
  Truck,
  Sparkles,
  Store,
  Search,
  ArrowRight,
  CheckCircle2,
  MapPin,
  Phone,
  Mail,
} from 'lucide-react'
import Logo from '@/components/Logo'
import { BRAND } from '@/lib/brand'

const values = [
  {
    icon: ShieldCheck,
    title: 'Integrity',
    description: 'Always honest, always transparent. Fair, upfront pricing on every part.',
  },
  {
    icon: BadgeCheck,
    title: 'Quality',
    description: 'Verified parts, guaranteed performance — sourced and checked before they reach you.',
  },
  {
    icon: Truck,
    title: 'Convenience',
    description: 'Easy online ordering with dependable delivery across Rwanda.',
  },
  {
    icon: Sparkles,
    title: 'Innovation',
    description: 'Always evolving, always improving — a modern way to buy auto parts.',
  },
]

const audiences = [
  {
    role: 'Vehicle Owners',
    headline: 'Find the right part, the first time',
    points: ['Browse verified parts by category', 'Fair, transparent prices', 'Delivered to your door'],
    color: 'from-brand-700 to-brand-900',
  },
  {
    role: 'Mechanics & Garages',
    headline: 'Source parts fast, keep cars moving',
    points: ['Trusted, genuine components', 'Order online in minutes', 'Reliable nationwide supply'],
    color: 'from-flame-500 to-flame-700',
  },
  {
    role: 'Sellers',
    headline: 'Reach more buyers on Loyal Spare Parts',
    points: ['Open your storefront on the marketplace', 'List your parts, manage orders', 'Get paid as you sell'],
    color: 'from-brand-500 to-brand-700',
  },
]

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-white">
      {/* ─── Navigation ─── */}
      <header className="sticky top-0 z-50 border-b border-gray-100 bg-white/95 backdrop-blur-sm">
        <nav className="mx-auto flex max-w-7xl items-center justify-between px-4 py-3 sm:px-6 lg:px-8">
          <Logo href="/" className="h-11" priority />

          <div className="hidden items-center gap-8 md:flex">
            <Link href="/store" className="text-sm font-medium text-gray-600 transition-colors hover:text-brand-700">Marketplace</Link>
            <a href="#values" className="text-sm font-medium text-gray-600 transition-colors hover:text-brand-700">Why us</a>
            <a href="#sell" className="text-sm font-medium text-gray-600 transition-colors hover:text-brand-700">Sell</a>
            <a href="#contact" className="text-sm font-medium text-gray-600 transition-colors hover:text-brand-700">Contact</a>
          </div>

          <div className="flex items-center gap-3">
            <Link href="/login" className="text-sm font-medium text-gray-600 transition-colors hover:text-brand-700">
              Sign in
            </Link>
            <Link href="/store" className="rounded-lg bg-flame-500 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-flame-600">
              Shop now
            </Link>
          </div>
        </nav>
      </header>

      {/* ─── Hero ─── */}
      <section className="relative overflow-hidden bg-gradient-to-br from-brand-900 via-brand-800 to-flame-600 px-4 pb-24 pt-20 sm:px-6 lg:px-8">
        <div className="relative mx-auto max-w-4xl text-center">
          <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-4 py-1.5 text-sm text-white/90">
            <MapPin className="h-3.5 w-3.5" />
            {BRAND.location}
          </div>

          <h1 className="text-4xl font-bold tracking-tight text-white sm:text-5xl lg:text-6xl">
            Quality Auto Spare Parts{' '}
            <span className="text-flame-300">You Can Trust</span>
          </h1>

          <p className="mx-auto mt-6 max-w-2xl text-lg text-brand-100">
            {BRAND.description}
          </p>

          <div className="mt-10 flex flex-col items-center gap-4 sm:flex-row sm:justify-center">
            <Link
              href="/store"
              className="flex items-center gap-2 rounded-xl bg-flame-500 px-6 py-3 text-base font-semibold text-white shadow-lg transition-colors hover:bg-flame-600"
            >
              <Search className="h-4 w-4" /> Shop the marketplace
            </Link>
            <Link
              href="/register"
              className="flex items-center gap-2 rounded-xl border border-white/30 px-6 py-3 text-base font-medium text-white transition-colors hover:bg-white/10"
            >
              <Store className="h-4 w-4" /> Sell on Loyal Spare Parts
            </Link>
          </div>

          <p className="mt-6 text-base font-medium italic text-flame-200">“{BRAND.motto}”</p>
        </div>
      </section>

      {/* ─── Values ─── */}
      <section id="values" className="px-4 py-24 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-7xl">
          <div className="mb-16 text-center">
            <h2 className="text-3xl font-bold text-brand-900 sm:text-4xl">Why buy from Loyal Spare Parts</h2>
            <p className="mt-4 text-lg text-gray-500">The values behind every part we sell.</p>
          </div>

          <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-4">
            {values.map(({ icon: Icon, title, description }) => (
              <div key={title} className="group rounded-2xl border border-gray-100 p-6 shadow-sm transition-all hover:border-flame-200 hover:shadow-md">
                <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-brand-50 transition-colors group-hover:bg-flame-50">
                  <Icon className="h-6 w-6 text-flame-500" />
                </div>
                <h3 className="mb-2 text-lg font-semibold text-brand-900">{title}</h3>
                <p className="text-sm leading-relaxed text-gray-500">{description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ─── Audiences ─── */}
      <section className="bg-gray-50 px-4 py-24 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-7xl">
          <div className="mb-16 text-center">
            <h2 className="text-3xl font-bold text-brand-900 sm:text-4xl">Built for everyone on the road</h2>
            <p className="mt-4 text-lg text-gray-500">One marketplace for owners, mechanics, and sellers.</p>
          </div>

          <div className="grid gap-6 md:grid-cols-3">
            {audiences.map(({ role, headline, points, color }) => (
              <div key={role} className="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm">
                <div className={`bg-gradient-to-br ${color} p-6`}>
                  <p className="text-xs font-semibold uppercase tracking-wider text-white/70">{role}</p>
                  <h3 className="mt-2 text-xl font-bold text-white">{headline}</h3>
                </div>
                <div className="p-6">
                  <ul className="space-y-3">
                    {points.map((p) => (
                      <li key={p} className="flex items-start gap-2.5 text-sm text-gray-600">
                        <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-flame-500" />
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

      {/* ─── Sell ─── */}
      <section id="sell" className="px-4 py-24 sm:px-6 lg:px-8">
        <div className="mx-auto grid max-w-5xl items-center gap-10 md:grid-cols-2">
          <div>
            <h2 className="text-3xl font-bold text-brand-900 sm:text-4xl">Sell on Loyal Spare Parts</h2>
            <p className="mt-4 text-lg text-gray-500">
              Open your own storefront inside Rwanda’s trusted spare-parts marketplace. List your
              parts, reach more buyers, and manage orders — we handle the storefront and checkout.
            </p>
            <ul className="mt-6 space-y-3">
              {['Create an account in minutes', 'Apply to become a seller', 'List products and start selling'].map((s, i) => (
                <li key={s} className="flex items-center gap-3 text-gray-700">
                  <span className="flex h-7 w-7 items-center justify-center rounded-full bg-brand-600 text-sm font-bold text-white">{i + 1}</span>
                  {s}
                </li>
              ))}
            </ul>
            <Link href="/register" className="mt-8 inline-flex items-center gap-2 rounded-xl bg-flame-500 px-6 py-3 text-base font-semibold text-white transition-colors hover:bg-flame-600">
              Get started <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
          <div className="rounded-2xl border border-gray-200 bg-gradient-to-br from-brand-50 to-flame-50 p-8">
            <p className="text-sm font-semibold uppercase tracking-wider text-flame-600">Our mission</p>
            <p className="mt-2 text-xl font-semibold text-brand-900">
              To simplify buying auto spare parts through transparency, trust, and innovative online solutions.
            </p>
            <p className="mt-6 text-sm font-semibold uppercase tracking-wider text-flame-600">Our vision</p>
            <p className="mt-2 text-lg text-brand-800">Rwanda’s #1 trusted platform for automotive spare parts by 2030.</p>
          </div>
        </div>
      </section>

      {/* ─── Contact / Final CTA ─── */}
      <section id="contact" className="bg-gradient-to-br from-brand-900 via-brand-800 to-flame-600 px-4 py-20 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-3xl text-center">
          <h2 className="text-3xl font-bold text-white sm:text-4xl">Get in touch</h2>
          <p className="mt-4 text-lg text-brand-100">Questions about a part or selling with us? We’re here to help.</p>
          <div className="mt-8 flex flex-col items-center justify-center gap-4 text-white/90 sm:flex-row sm:gap-8">
            <span className="inline-flex items-center gap-2"><Phone className="h-4 w-4 text-flame-300" /> {BRAND.phone}</span>
            <span className="inline-flex items-center gap-2"><Mail className="h-4 w-4 text-flame-300" /> {BRAND.email}</span>
            <span className="inline-flex items-center gap-2"><MapPin className="h-4 w-4 text-flame-300" /> {BRAND.location}</span>
          </div>
          <div className="mt-10">
            <Link href="/store" className="inline-flex items-center gap-2 rounded-xl bg-white px-8 py-3 text-base font-semibold text-brand-700 transition-colors hover:bg-brand-50">
              Browse the marketplace <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
        </div>
      </section>

      {/* ─── Footer ─── */}
      <footer className="border-t border-gray-100 bg-white px-4 py-10 sm:px-6 lg:px-8">
        <div className="mx-auto flex max-w-7xl flex-col items-center justify-between gap-4 sm:flex-row">
          <Logo href="/" className="h-12" />
          <p className="text-sm text-gray-400">© {new Date().getFullYear()} {BRAND.legalName}. {BRAND.motto}</p>
          <div className="flex gap-6">
            <Link href="/store" className="text-sm text-gray-400 hover:text-brand-700">Marketplace</Link>
            <Link href="/login" className="text-sm text-gray-400 hover:text-brand-700">Sign in</Link>
          </div>
        </div>
      </footer>
    </div>
  )
}
