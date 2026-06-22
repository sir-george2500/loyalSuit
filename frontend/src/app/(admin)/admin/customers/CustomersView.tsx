'use client'

import { useEffect, useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Users, AlertCircle, ChevronLeft, ChevronRight, Search } from 'lucide-react'
import { customerApi } from '@/lib/api/customers'

function money(amount: number): string {
  return new Intl.NumberFormat('en-RW', { style: 'currency', currency: 'RWF', maximumFractionDigits: 0 }).format(amount)
}

function date(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

export default function CustomersView() {
  const [page, setPage] = useState(0)
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')

  // Debounce the search box so we don't fire a request per keystroke.
  useEffect(() => {
    const t = setTimeout(() => { setSearch(searchInput.trim()); setPage(0) }, 300)
    return () => clearTimeout(t)
  }, [searchInput])

  const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['customers', page, search],
    queryFn: async () => (await customerApi.list({ search, page })).data.data,
    placeholderData: keepPreviousData,
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load customers. Try again.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <label className="input input-bordered input-sm flex items-center gap-2">
          <Search className="h-4 w-4 text-base-content/50" />
          <input
            type="search"
            className="grow"
            placeholder="Search by name or email"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
          />
        </label>
        {isFetching && <span className="loading loading-spinner loading-xs text-primary" />}
      </div>

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr>
              <th>Customer</th>
              <th>Phone</th>
              <th className="text-center">Orders</th>
              <th className="text-right">Lifetime spend</th>
              <th>Joined</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i}><td colSpan={5}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((c) => (
                <tr key={c.id}>
                  <td>
                    <div className="font-medium">{c.fullName}</div>
                    <div className="text-xs text-base-content/50">{c.email}</div>
                  </td>
                  <td className="text-sm">{c.phone ?? '—'}</td>
                  <td className="text-center">{c.orderCount}</td>
                  <td className="text-right font-medium">{money(c.totalSpent)}</td>
                  <td className="text-sm text-base-content/60">{date(c.joinedAt)}</td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={5}>
                <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                  <Users className="h-8 w-8" /><p className="text-sm">{search ? 'No customers match your search.' : 'No customers yet.'}</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-base-content/60">
          <span>Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} customer{data.totalElements === 1 ? '' : 's'}</span>
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
