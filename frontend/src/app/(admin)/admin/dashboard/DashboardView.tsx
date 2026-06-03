'use client'

import { useQuery } from '@tanstack/react-query'
import {
  DollarSign,
  ShoppingCart,
  Receipt,
  Users,
  Package,
  Store,
  AlertTriangle,
  ArrowUpRight,
  ArrowDownRight,
  AlertCircle,
  Inbox,
} from 'lucide-react'
import { dashboardApi } from '@/lib/api/dashboard'
import Sparkline from '@/components/charts/Sparkline'
import type { DashboardStats, KpiMetric, OrderStatus, RecentOrder } from '@/types'

const currency = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  maximumFractionDigits: 0,
})
const currencyPrecise = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })
const number = new Intl.NumberFormat('en-US')

function TrendBadge({ metric }: { metric: KpiMetric }) {
  const up = metric.changePercent >= 0
  const flat = metric.changePercent === 0
  return (
    <span
      className={`inline-flex items-center gap-0.5 rounded-full px-1.5 py-0.5 text-xs font-medium ${
        flat ? 'bg-base-200 text-base-content/50' : up ? 'bg-success/10 text-success' : 'bg-error/10 text-error'
      }`}
      title="vs previous 30 days"
    >
      {!flat && (up ? <ArrowUpRight className="h-3 w-3" /> : <ArrowDownRight className="h-3 w-3" />)}
      {up ? '+' : ''}
      {metric.changePercent}%
    </span>
  )
}

const STATUS_BADGE: Record<OrderStatus, string> = {
  PENDING: 'badge-warning',
  CONFIRMED: 'badge-info',
  PROCESSING: 'badge-info',
  SHIPPED: 'badge-primary',
  DELIVERED: 'badge-success',
  CANCELLED: 'badge-ghost',
  REFUNDED: 'badge-error',
}

function KpiCard({
  label,
  value,
  metric,
  icon: Icon,
  tone,
}: {
  label: string
  value: string
  metric?: KpiMetric
  icon: typeof DollarSign
  tone: string
}) {
  return (
    <div className="card bg-base-100 shadow-sm">
      <div className="card-body gap-2 p-5">
        <div className="flex items-center justify-between">
          <div className={`flex h-10 w-10 items-center justify-center rounded-xl ${tone}`}>
            <Icon className="h-5 w-5" />
          </div>
          {metric && <TrendBadge metric={metric} />}
        </div>
        <div>
          <p className="text-sm text-base-content/60">{label}</p>
          <p className="text-2xl font-bold">{value}</p>
        </div>
      </div>
    </div>
  )
}

function RecentOrdersTable({ orders }: { orders: RecentOrder[] }) {
  if (orders.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center gap-2 py-10 text-center text-base-content/40">
        <Inbox className="h-8 w-8" />
        <p className="text-sm">No orders yet. They&apos;ll show up here as soon as you start selling.</p>
      </div>
    )
  }
  return (
    <div className="overflow-x-auto">
      <table className="table table-sm">
        <thead>
          <tr>
            <th>Order</th>
            <th>Customer</th>
            <th>Status</th>
            <th className="text-right">Total</th>
          </tr>
        </thead>
        <tbody>
          {orders.map((o) => (
            <tr key={o.orderNumber}>
              <td className="font-mono text-xs">{o.orderNumber}</td>
              <td className="max-w-[12rem] truncate">{o.customerName ?? '—'}</td>
              <td>
                <span className={`badge badge-sm ${STATUS_BADGE[o.status] ?? 'badge-ghost'}`}>
                  {o.status.toLowerCase()}
                </span>
              </td>
              <td className="text-right font-medium">{currencyPrecise.format(o.total)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function OrderStatusBreakdown({ data }: { data: DashboardStats['ordersByStatus'] }) {
  const total = data.reduce((sum, s) => sum + s.count, 0)
  if (total === 0) {
    return <p className="py-6 text-center text-sm text-base-content/40">No orders to break down yet.</p>
  }
  return (
    <ul className="space-y-3">
      {data.map((s) => {
        const pct = Math.round((s.count / total) * 100)
        return (
          <li key={s.status}>
            <div className="mb-1 flex justify-between text-xs">
              <span className="capitalize text-base-content/70">{s.status.toLowerCase()}</span>
              <span className="text-base-content/50">
                {s.count} · {pct}%
              </span>
            </div>
            <progress className="progress progress-primary h-1.5 w-full" value={pct} max={100} />
          </li>
        )
      })}
    </ul>
  )
}

export default function DashboardView() {
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['dashboard-stats'],
    queryFn: async () => (await dashboardApi.getStats()).data.data,
  })

  if (isError) {
    return (
      <div role="alert" className="alert alert-error">
        <AlertCircle className="h-5 w-5" />
        <span>Couldn&apos;t load dashboard analytics.</span>
        <button onClick={() => refetch()} className="btn btn-sm">
          Retry
        </button>
      </div>
    )
  }

  if (isLoading || !data) {
    return (
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="card bg-base-100 shadow-sm">
            <div className="card-body gap-3 p-5">
              <div className="skeleton h-10 w-10 rounded-xl" />
              <div className="skeleton h-4 w-20" />
              <div className="skeleton h-7 w-24" />
            </div>
          </div>
        ))}
      </div>
    )
  }

  const trendValues = data.revenueTrend.map((p) => p.amount)
  const periodRevenue = trendValues.reduce((a, b) => a + b, 0)

  return (
    <div className="space-y-6">
      {/* Headline KPIs with period-over-period trend */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <KpiCard
          label="Revenue (30d)"
          value={currency.format(data.revenue.current)}
          metric={data.revenue}
          icon={DollarSign}
          tone="bg-success/10 text-success"
        />
        <KpiCard
          label="Orders (30d)"
          value={number.format(data.orders.current)}
          metric={data.orders}
          icon={ShoppingCart}
          tone="bg-info/10 text-info"
        />
        <KpiCard
          label="Avg. Order Value"
          value={currencyPrecise.format(data.averageOrderValue)}
          icon={Receipt}
          tone="bg-primary/10 text-primary"
        />
        <KpiCard
          label="New Customers (30d)"
          value={number.format(data.customers.current)}
          metric={data.customers}
          icon={Users}
          tone="bg-secondary/10 text-secondary"
        />
      </div>

      {/* Revenue trend + order status */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className="card bg-base-100 shadow-sm lg:col-span-2">
          <div className="card-body">
            <div className="flex items-start justify-between">
              <div>
                <h2 className="card-title text-base">Revenue — last 14 days</h2>
                <p className="text-2xl font-bold">{currency.format(periodRevenue)}</p>
              </div>
            </div>
            <Sparkline data={trendValues} className="mt-2 h-20 w-full" />
            <div className="flex justify-between text-xs text-base-content/40">
              <span>{data.revenueTrend[0]?.date}</span>
              <span>{data.revenueTrend[data.revenueTrend.length - 1]?.date}</span>
            </div>
          </div>
        </div>

        <div className="card bg-base-100 shadow-sm">
          <div className="card-body">
            <h2 className="card-title text-base">Orders by status</h2>
            <OrderStatusBreakdown data={data.ordersByStatus} />
          </div>
        </div>
      </div>

      {/* Operational stats */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <KpiCard
          label="Active Products"
          value={`${number.format(data.activeProducts)} / ${number.format(data.totalProducts)}`}
          icon={Package}
          tone="bg-primary/10 text-primary"
        />
        <KpiCard
          label="Low Stock"
          value={number.format(data.lowStockCount)}
          icon={AlertTriangle}
          tone="bg-warning/10 text-warning"
        />
        <KpiCard
          label="Vendors"
          value={number.format(data.totalVendors)}
          icon={Store}
          tone="bg-accent/10 text-accent"
        />
        <KpiCard
          label="Total Products"
          value={number.format(data.totalProducts)}
          icon={Package}
          tone="bg-base-200 text-base-content/70"
        />
      </div>

      {/* Recent orders */}
      <div className="card bg-base-100 shadow-sm">
        <div className="card-body">
          <h2 className="card-title text-base">Recent orders</h2>
          <RecentOrdersTable orders={data.recentOrders} />
        </div>
      </div>
    </div>
  )
}
