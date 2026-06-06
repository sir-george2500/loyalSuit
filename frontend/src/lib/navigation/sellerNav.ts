import type { LucideIcon } from 'lucide-react'
import {
  LayoutDashboard,
  Package,
  ShoppingCart,
  Wallet,
  Banknote,
  Star,
  Settings,
} from 'lucide-react'

export type NavStatus = 'available' | 'soon'

export interface SellerNavItem {
  label: string
  href: string
  icon: LucideIcon
  /** "soon" items render disabled (planned scope) so we never ship broken links. */
  status: NavStatus
}

/**
 * Navigation for the vendor (seller) area. The marketplace domain (products,
 * orders, payouts) lands in a later phase, so those items are visibly marked
 * "soon" rather than linking to half-built pages.
 */
export const SELLER_NAV: SellerNavItem[] = [
  { label: 'Dashboard', href: '/seller/dashboard', icon: LayoutDashboard, status: 'available' },
  { label: 'Products', href: '/seller/products', icon: Package, status: 'available' },
  { label: 'Orders', href: '/seller/orders', icon: ShoppingCart, status: 'soon' },
  { label: 'Earnings', href: '/seller/earnings', icon: Wallet, status: 'available' },
  { label: 'Payouts', href: '/seller/payouts', icon: Banknote, status: 'available' },
  { label: 'Reviews', href: '/seller/reviews', icon: Star, status: 'soon' },
  { label: 'Settings', href: '/seller/settings', icon: Settings, status: 'soon' },
]
