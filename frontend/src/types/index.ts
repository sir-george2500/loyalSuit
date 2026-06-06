export type UserRole =
  | 'SUPER_ADMIN'
  | 'TENANT_ADMIN'
  | 'STAFF'
  | 'VENDOR'
  | 'CUSTOMER'
  | 'DELIVERY_AGENT'

export type ProductStatus = 'DRAFT' | 'ACTIVE' | 'INACTIVE' | 'ARCHIVED'
export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED' | 'REFUNDED'
export type VendorStatus = 'PENDING' | 'ACTIVE' | 'SUSPENDED' | 'REJECTED'
export type SubscriptionPlan = 'BASIC' | 'PROFESSIONAL' | 'ENTERPRISE'

export interface Product {
  id: string
  name: string
  slug: string
  description?: string
  price: number
  compareAtPrice?: number
  sku?: string
  barcode?: string
  categoryId?: string
  vendorId?: string
  digital: boolean
  status: ProductStatus
  createdAt: string
  updatedAt: string
}

export interface Category {
  id: string
  name: string
  slug: string
  description?: string
  imageUrl?: string
  parentId?: string
  active: boolean
  sortOrder: number
  createdAt: string
}

export interface Vendor {
  id: string
  userId: string
  storeName: string
  slug: string
  description?: string
  logoUrl?: string
  commissionRate: number
  status: VendorStatus
  createdAt: string
}

export interface Order {
  id: string
  orderNumber: string
  customerId: string
  status: OrderStatus
  subtotal: number
  shippingAmount: number
  taxAmount: number
  discountAmount: number
  total: number
  currency: string
  createdAt: string
}

export interface Tenant {
  id: string
  name: string
  slug: string
  logoUrl?: string
  domain?: string
  subscriptionPlan: SubscriptionPlan
  active: boolean
  createdAt: string
}

export interface AppUser {
  id: string
  email: string
  fullName?: string
  avatarUrl?: string
  phone?: string
  role: UserRole
  active: boolean
}

export interface UserProfile {
  id: string
  email: string
  fullName?: string
  avatarUrl?: string
  role: UserRole
  tenantId: string
}

export interface AuthResponse {
  token: string
  tokenType: string
  expiresIn: number
  user: UserProfile
}

export interface OnboardingStatus {
  onboarded: boolean
  businessName: string
  currency: string
  country?: string | null
  timezone: string
  phone?: string | null
}

export interface CompleteOnboardingRequest {
  businessName: string
  phone?: string
  country?: string
  currency: string
  timezone: string
  warehouseName: string
  warehouseAddress?: string
}

export interface KpiMetric {
  current: number
  previous: number
  changePercent: number
}

export interface RevenuePoint {
  date: string
  amount: number
}

export interface StatusCount {
  status: string
  count: number
}

export interface RecentOrder {
  orderNumber: string
  total: number
  status: OrderStatus
  createdAt: string
  customerName: string | null
}

export interface DashboardStats {
  revenue: KpiMetric
  orders: KpiMetric
  customers: KpiMetric
  averageOrderValue: number
  totalProducts: number
  activeProducts: number
  lowStockCount: number
  totalVendors: number
  revenueTrend: RevenuePoint[]
  ordersByStatus: StatusCount[]
  recentOrders: RecentOrder[]
}

export type AuditAction =
  | 'USER_REGISTERED'
  | 'LOGIN_SUCCEEDED'
  | 'LOGIN_FAILED'
  | 'PASSWORD_CHANGED'
  | 'PASSWORD_RESET_REQUESTED'
  | 'PASSWORD_RESET_COMPLETED'
  | 'TENANT_ONBOARDED'

export type AuditOutcome = 'SUCCESS' | 'FAILURE'

export interface AuditLog {
  id: string
  action: AuditAction
  outcome: AuditOutcome
  actorId?: string | null
  actorEmail?: string | null
  actorRole?: string | null
  resourceType?: string | null
  resourceId?: string | null
  ipAddress?: string | null
  detail?: string | null
  occurredAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface ApiResponse<T> {
  success: boolean
  message?: string
  data: T
}

export interface ProductWriteRequest {
  name: string
  slug: string
  description?: string
  price: number
  compareAtPrice?: number
  sku?: string
  barcode?: string
  categoryId?: string
  digital?: boolean
}

// vendorId is never sent by the client — the backend derives ownership from the
// authenticated principal. Create and update share the same writable shape.
export type CreateProductRequest = ProductWriteRequest
export type UpdateProductRequest = ProductWriteRequest

export interface ProductVariant {
  id: string
  productId: string
  name: string
  sku?: string | null
  price: number
  createdAt: string
}

export interface VariantRequest {
  name: string
  sku?: string
  price: number
}

export interface ProductMedia {
  id: string
  productId: string
  url: string
  primary: boolean
  sortOrder: number
}

export interface Warehouse {
  id: string
  name: string
  address?: string | null
  isDefault: boolean
  active: boolean
  createdAt: string
}

export interface WarehouseRequest {
  name: string
  address?: string
  isDefault?: boolean
}

export interface Stock {
  id: string
  productId: string
  variantId?: string | null
  warehouseId: string
  quantity: number
  lowStockThreshold: number
  lowStock: boolean
}

export interface SetStockRequest {
  productId: string
  variantId?: string
  warehouseId: string
  quantity: number
  lowStockThreshold?: number
}

// ---- Public storefront (customer-facing read model) ----
export interface StoreView {
  name: string
  slug: string
  currency: string
  logoUrl?: string | null
}

export interface StoreCategory {
  name: string
  slug: string
}

export interface StoreProductSummary {
  id: string
  name: string
  slug: string
  price: number
  compareAtPrice?: number | null
  imageUrl?: string | null
  categoryName?: string | null
}

export interface StoreVariant {
  id: string
  name: string
  price: number
}

export interface StoreProductDetail {
  id: string
  name: string
  slug: string
  description?: string | null
  price: number
  compareAtPrice?: number | null
  digital: boolean
  categoryName?: string | null
  images: string[]
  variants: StoreVariant[]
  inStock: boolean
}

export interface CartItemView {
  productId: string
  variantId?: string | null
  productName: string
  productSlug: string
  variantName?: string | null
  imageUrl?: string | null
  unitPrice: number
  quantity: number
  lineTotal: number
}

export interface CartView {
  items: CartItemView[]
  subtotal: number
  itemCount: number
  currency: string
}

export interface OrderItemResponse {
  productId: string
  variantId?: string | null
  quantity: number
  unitPrice: number
  total: number
}

export type PaymentStatus = 'UNPAID' | 'PAID' | 'REFUNDED'

export interface OrderResponse {
  id: string
  orderNumber: string
  status: OrderStatus
  paymentMethod: string
  paymentStatus: PaymentStatus
  customerName: string
  customerEmail?: string | null
  customerPhone?: string | null
  shippingAddress?: Record<string, string> | null
  notes?: string | null
  subtotal: number
  total: number
  currency: string
  items: OrderItemResponse[]
  createdAt: string
}

export interface OrderSummary {
  id: string
  orderNumber: string
  status: OrderStatus
  paymentStatus: PaymentStatus
  customerName: string
  total: number
  currency: string
  createdAt: string
}

export type ReturnStatus = 'REQUESTED' | 'APPROVED' | 'REJECTED'

export interface ReturnResponse {
  id: string
  orderId: string
  orderNumber: string
  status: ReturnStatus
  reason: string
  resolutionNote?: string | null
  createdAt: string
}

export interface CreateCategoryRequest {
  name: string
  slug: string
  description?: string
  imageUrl?: string
  parentId?: string
  sortOrder?: number
}
