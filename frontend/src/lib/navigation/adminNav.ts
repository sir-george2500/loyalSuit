import type { LucideIcon } from 'lucide-react'
import {
  LayoutDashboard,
  Package,
  Tags,
  Boxes,
  ShoppingCart,
  Undo2,
  MonitorSmartphone,
  Store,
  Wallet,
  Percent,
  Users,
  UserCog,
  CreditCard,
  FileText,
  Building2,
  Layers,
  Activity,
  Flag,
  ShieldCheck,
  ScrollText,
  KeyRound,
  Settings,
  Bell,
} from 'lucide-react'
import type { UserRole } from '@/types'

export type NavStatus = 'available' | 'soon'

export interface NavItem {
  label: string
  href: string
  icon: LucideIcon
  /** Roles allowed to see this item. */
  roles: UserRole[]
  /** "soon" items render disabled (planned scope) so we never ship broken links. */
  status: NavStatus
}

export interface NavSection {
  title: string
  items: NavItem[]
}

const STORE_ROLES: UserRole[] = ['SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF']
const OWNER_ROLES: UserRole[] = ['SUPER_ADMIN', 'TENANT_ADMIN']
const PLATFORM_ROLES: UserRole[] = ['SUPER_ADMIN']

/**
 * Single source of truth for admin navigation. Items are scoped by role and by
 * build status. The backend independently enforces authorization on every API
 * call — this config governs presentation only.
 */
export const ADMIN_NAV: NavSection[] = [
  {
    title: 'Overview',
    items: [
      { label: 'Dashboard', href: '/admin/dashboard', icon: LayoutDashboard, roles: STORE_ROLES, status: 'available' },
    ],
  },
  {
    title: 'Catalog',
    items: [
      { label: 'Products', href: '/admin/products', icon: Package, roles: STORE_ROLES, status: 'soon' },
      { label: 'Categories', href: '/admin/categories', icon: Tags, roles: STORE_ROLES, status: 'soon' },
      { label: 'Inventory', href: '/admin/inventory', icon: Boxes, roles: STORE_ROLES, status: 'soon' },
    ],
  },
  {
    title: 'Sales',
    items: [
      { label: 'Orders', href: '/admin/orders', icon: ShoppingCart, roles: STORE_ROLES, status: 'soon' },
      { label: 'Returns', href: '/admin/returns', icon: Undo2, roles: STORE_ROLES, status: 'soon' },
      { label: 'POS Terminal', href: '/pos', icon: MonitorSmartphone, roles: STORE_ROLES, status: 'available' },
    ],
  },
  {
    title: 'Marketplace',
    items: [
      { label: 'Vendors', href: '/admin/vendors', icon: Store, roles: OWNER_ROLES, status: 'soon' },
      { label: 'Payouts', href: '/admin/payouts', icon: Wallet, roles: OWNER_ROLES, status: 'soon' },
      { label: 'Commissions', href: '/admin/commissions', icon: Percent, roles: OWNER_ROLES, status: 'soon' },
    ],
  },
  {
    title: 'People',
    items: [
      { label: 'Customers', href: '/admin/customers', icon: Users, roles: STORE_ROLES, status: 'soon' },
      { label: 'Staff & Roles', href: '/admin/staff', icon: UserCog, roles: OWNER_ROLES, status: 'soon' },
    ],
  },
  {
    title: 'Finance',
    items: [
      { label: 'Payments', href: '/admin/payments', icon: CreditCard, roles: OWNER_ROLES, status: 'soon' },
      { label: 'Invoices', href: '/admin/invoices', icon: FileText, roles: OWNER_ROLES, status: 'soon' },
    ],
  },
  {
    title: 'Platform',
    items: [
      { label: 'Tenants', href: '/admin/tenants', icon: Building2, roles: PLATFORM_ROLES, status: 'soon' },
      { label: 'Subscription Plans', href: '/admin/plans', icon: Layers, roles: PLATFORM_ROLES, status: 'soon' },
      { label: 'System Health', href: '/admin/system', icon: Activity, roles: PLATFORM_ROLES, status: 'soon' },
      { label: 'Feature Flags', href: '/admin/flags', icon: Flag, roles: PLATFORM_ROLES, status: 'soon' },
    ],
  },
  {
    title: 'Security & Access',
    items: [
      { label: 'Roles & Permissions', href: '/admin/roles', icon: ShieldCheck, roles: OWNER_ROLES, status: 'soon' },
      { label: 'Audit Log', href: '/admin/audit', icon: ScrollText, roles: OWNER_ROLES, status: 'available' },
      { label: 'API Keys', href: '/admin/api-keys', icon: KeyRound, roles: OWNER_ROLES, status: 'soon' },
    ],
  },
  {
    title: 'Settings',
    items: [
      { label: 'General', href: '/admin/settings', icon: Settings, roles: STORE_ROLES, status: 'available' },
      { label: 'Security', href: '/admin/settings/security', icon: ShieldCheck, roles: STORE_ROLES, status: 'available' },
      { label: 'Notifications', href: '/admin/settings/notifications', icon: Bell, roles: STORE_ROLES, status: 'soon' },
    ],
  },
]

/** Returns the nav filtered to a role, dropping any section left empty. */
export function navForRole(role: UserRole | undefined): NavSection[] {
  if (!role) return []
  return ADMIN_NAV
    .map((section) => ({
      ...section,
      items: section.items.filter((item) => item.roles.includes(role)),
    }))
    .filter((section) => section.items.length > 0)
}
