'use client'

import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Star, Loader2, MessageSquare } from 'lucide-react'
import { reviewApi } from '@/lib/api/reviews'
import type { VendorReviews } from '@/types'

function Stars({ value }: { value: number }) {
  return (
    <span className="inline-flex" aria-label={`${value} out of 5 stars`}>
      {[1, 2, 3, 4, 5].map((n) => (
        <Star
          key={n}
          className={`h-4 w-4 ${n <= Math.round(value) ? 'fill-flame-500 text-flame-500' : 'text-base-300'}`}
        />
      ))}
    </span>
  )
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

export default function SellerReviewsView() {
  const [page, setPage] = useState(0)

  const { data, isLoading, isError } = useQuery<VendorReviews>({
    queryKey: ['vendor-reviews', page],
    queryFn: async () => (await reviewApi.forVendor(page)).data.data,
    placeholderData: keepPreviousData,
  })

  if (isLoading) {
    return (
      <div className="flex justify-center py-16">
        <Loader2 className="h-6 w-6 animate-spin text-primary" />
      </div>
    )
  }

  if (isError) {
    return <div className="alert alert-error text-sm">Could not load your reviews.</div>
  }

  const summary = data?.summary
  const reviews = data?.reviews

  return (
    <div className="space-y-6">
      {/* Aggregate */}
      <div className="stats bg-base-100 shadow">
        <div className="stat">
          <div className="stat-title">Average rating</div>
          <div className="stat-value flex items-center gap-2 text-2xl">
            {summary && summary.count > 0 ? summary.averageRating.toFixed(1) : '—'}
            {summary && summary.count > 0 && <Stars value={summary.averageRating} />}
          </div>
          <div className="stat-desc">{summary?.count ?? 0} review{(summary?.count ?? 0) === 1 ? '' : 's'}</div>
        </div>
      </div>

      {/* List */}
      {!reviews || reviews.content.length === 0 ? (
        <div className="card bg-base-100 shadow">
          <div className="card-body items-center text-center text-base-content/60">
            <MessageSquare className="h-10 w-10 text-base-300" />
            <p>No reviews yet. They’ll appear here once customers review what they bought from you.</p>
          </div>
        </div>
      ) : (
        <div className="space-y-3">
          {reviews.content.map((r) => (
            <div key={r.id} className="card bg-base-100 shadow">
              <div className="card-body gap-2 py-4">
                <div className="flex items-center justify-between gap-2">
                  <span className="font-medium">{r.productName}</span>
                  <Stars value={r.rating} />
                </div>
                {r.comment && <p className="whitespace-pre-line text-sm text-base-content/80">{r.comment}</p>}
                <p className="text-xs text-base-content/50">{r.authorName} · {formatDate(r.createdAt)}</p>
              </div>
            </div>
          ))}

          {reviews.totalPages > 1 && (
            <div className="flex items-center justify-between pt-2">
              <button
                className="btn btn-sm btn-ghost"
                disabled={reviews.first}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Previous
              </button>
              <span className="text-sm text-base-content/60">Page {reviews.page + 1} of {reviews.totalPages}</span>
              <button
                className="btn btn-sm btn-ghost"
                disabled={reviews.last}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
