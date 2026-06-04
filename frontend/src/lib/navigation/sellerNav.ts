import type { LucideIcon } from 'lucide-react'
import {
  LayoutDashboard,
  Package,
  ShoppingCart,
  Wallet,
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
  { label: 'Products', href: '/seller/products', icon: Package, status: 'soon' },
  { label: 'Orders', href: '/seller/orders', icon: ShoppingCart, status: 'soon' },
  { label: 'Payouts', href: '/seller/payouts', icon: Wallet, status: 'soon' },
  { label: 'Reviews', href: '/seller/reviews', icon: Star, status: 'soon' },
  { label: 'Settings', href: '/seller/settings', icon: Settings, status: 'soon' },
]
