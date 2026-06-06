'use client'

import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { AlertCircle, ChevronLeft, ChevronRight, Percent } from 'lucide-react'
import type { ApiResponse, CommissionEntry, CommissionStatus, PageResponse } from '@/types'
import type { AxiosResponse } from 'axios'
import { useState } from 'react'

const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })

const STATUS_BADGE: Record<CommissionStatus, string> = {
  EARNED: 'badge-success',
  REVERSED: 'badge-neutral',
}

/**
 * A paginated, read-only commission ledger. The {@code fetchPage} callback decides
 * which ledger is shown (a vendor's own, or the tenant-wide admin view), so the same
 * table serves both. {@code showVendor} adds the vendor column for the admin view.
 */
export default function CommissionLedger({
  queryKey,
  fetchPage,
  showVendor = false,
}: {
  queryKey: string
  fetchPage: (page: number) => Promise<AxiosResponse<ApiResponse<PageResponse<CommissionEntry>>>>
  showVendor?: boolean
}) {
  const [page, setPage] = useState(0)

  const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: [queryKey, page],
    queryFn: async () => (await fetchPage(page)).data.data,
    placeholderData: keepPreviousData,
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load the commission ledger. Try again.</p>
        </div>
      </div>
    )
  }

  const colSpan = showVendor ? 7 : 6

  return (
    <div className="space-y-4">
      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr>
              <th>Order</th>
              {showVendor && <th>Vendor</th>}
              <th className="text-right">Gross</th>
              <th className="text-right">Rate</th>
              <th className="text-right">Commission</th>
              <th className="text-right">Net</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i}><td colSpan={colSpan}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((e) => (
                <tr key={e.id}>
                  <td className="font-mono text-xs">{e.orderNumber}</td>
                  {showVendor && <td className="font-mono text-xs text-base-content/50">{e.vendorId.slice(0, 8)}</td>}
                  <td className="text-right">{money.format(e.grossAmount)}</td>
                  <td className="text-right text-base-content/60">{e.commissionRate}%</td>
                  <td className="text-right text-error">−{money.format(e.commissionAmount)}</td>
                  <td className="text-right font-medium">{money.format(e.netAmount)}</td>
                  <td><span className={`badge badge-sm ${STATUS_BADGE[e.status]}`}>{e.status}</span></td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={colSpan}>
                <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                  <Percent className="h-8 w-8" /><p className="text-sm">No commission entries yet.</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-base-content/60">
          <span>
            Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} entr{data.totalElements === 1 ? 'y' : 'ies'}
            {isFetching && <span className="loading loading-spinner loading-xs ml-2 align-middle text-primary" />}
          </span>
          <div className="join">
            <button className="btn btn-sm join-item" onClick={() => setPage((p) => Math.max(p - 1, 0))} disabled={data.first}>
              <ChevronLeft className="h-4 w-4" /> Prev
            </button>
            <button className="btn btn-sm join-item" onClick={() => setPage((p) => p + 1)} disabled={data.last}>
              Next <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
