'use client'

import { useCallback, useEffect, useState } from 'react'
import { Star, Loader2 } from 'lucide-react'
import type { AxiosError } from 'axios'
import { reviewApi } from '@/lib/api/reviews'
import type { ProductReview, ReviewSummary } from '@/types'

function Stars({ value, className = 'h-4 w-4' }: { value: number; className?: string }) {
  return (
    <span className="inline-flex" aria-label={`${value} out of 5 stars`}>
      {[1, 2, 3, 4, 5].map((n) => (
        <Star
          key={n}
          className={`${className} ${n <= Math.round(value) ? 'fill-flame-500 text-flame-500' : 'text-gray-300'}`}
        />
      ))}
    </span>
  )
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

export default function ProductReviews({ productId }: { productId: string }) {
  const [summary, setSummary] = useState<ReviewSummary | null>(null)
  const [reviews, setReviews] = useState<ProductReview[]>([])
  const [page, setPage] = useState(0)
  const [last, setLast] = useState(true)
  const [loading, setLoading] = useState(true)

  // Eligibility resolves only for a signed-in customer who received this product.
  const [canReview, setCanReview] = useState(false)
  const [alreadyReviewed, setAlreadyReviewed] = useState(false)

  const load = useCallback(
    async (next: number) => {
      setLoading(true)
      try {
        const { data } = await reviewApi.forProduct(productId, next)
        setSummary(data.data.summary)
        setReviews((prev) => (next === 0 ? data.data.reviews.content : [...prev, ...data.data.reviews.content]))
        setLast(data.data.reviews.last)
        setPage(next)
      } finally {
        setLoading(false)
      }
    },
    [productId],
  )

  useEffect(() => {
    load(0)
  }, [load])

  useEffect(() => {
    let active = true
    reviewApi
      .eligibility(productId)
      .then(({ data }) => {
        if (!active) return
        setCanReview(data.data.canReview)
        setAlreadyReviewed(data.data.alreadyReviewed)
      })
      // Anonymous / not-a-customer → no form. Not an error worth surfacing.
      .catch(() => undefined)
    return () => {
      active = false
    }
  }, [productId])

  function onSubmitted(review: ProductReview) {
    setReviews((prev) => [review, ...prev])
    setCanReview(false)
    setAlreadyReviewed(true)
    setSummary((prev) => {
      const count = (prev?.count ?? 0) + 1
      const prevTotal = (prev?.averageRating ?? 0) * (prev?.count ?? 0)
      return { count, averageRating: Math.round(((prevTotal + review.rating) / count) * 10) / 10 }
    })
  }

  return (
    <section className="mt-12 border-t border-gray-100 pt-8">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-gray-900">Reviews</h2>
        {summary && summary.count > 0 && (
          <div className="flex items-center gap-2 text-sm text-gray-600">
            <Stars value={summary.averageRating} />
            <span className="font-medium text-gray-900">{summary.averageRating.toFixed(1)}</span>
            <span>({summary.count})</span>
          </div>
        )}
      </div>

      {canReview && <WriteReview productId={productId} onSubmitted={onSubmitted} />}
      {alreadyReviewed && !canReview && (
        <p className="mt-3 text-sm text-gray-500">Thanks — you’ve reviewed this product.</p>
      )}

      <div className="mt-6 space-y-5">
        {reviews.length === 0 && !loading && (
          <p className="text-sm text-gray-500">No reviews yet. Reviews come from customers who bought this part.</p>
        )}
        {reviews.map((r) => (
          <div key={r.id} className="border-b border-gray-50 pb-4 last:border-0">
            <div className="flex items-center gap-2">
              <Stars value={r.rating} />
              <span className="text-sm font-medium text-gray-900">{r.authorName}</span>
              <span className="text-xs text-gray-400">· {formatDate(r.createdAt)}</span>
            </div>
            {r.comment && <p className="mt-1.5 whitespace-pre-line text-sm text-gray-700">{r.comment}</p>}
          </div>
        ))}
      </div>

      {loading && (
        <div className="flex justify-center py-4">
          <Loader2 className="h-5 w-5 animate-spin text-gray-400" />
        </div>
      )}
      {!last && !loading && (
        <button onClick={() => load(page + 1)} className="mt-4 text-sm font-medium text-brand-600 hover:underline">
          Show more reviews
        </button>
      )}
    </section>
  )
}

function WriteReview({ productId, onSubmitted }: { productId: string; onSubmitted: (r: ProductReview) => void }) {
  const [rating, setRating] = useState(0)
  const [hover, setHover] = useState(0)
  const [comment, setComment] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit() {
    if (rating < 1) {
      setError('Please choose a star rating.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      const { data } = await reviewApi.create({ productId, rating, comment: comment.trim() || undefined })
      onSubmitted(data.data)
    } catch (err) {
      setError((err as AxiosError<{ message?: string }>)?.response?.data?.message ?? 'Could not submit your review.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mt-4 rounded-xl border border-gray-200 bg-gray-50 p-4">
      <p className="text-sm font-medium text-gray-900">Write a review</p>
      <div className="mt-2 flex items-center gap-1">
        {[1, 2, 3, 4, 5].map((n) => (
          <button
            key={n}
            type="button"
            aria-label={`${n} star${n > 1 ? 's' : ''}`}
            onClick={() => setRating(n)}
            onMouseEnter={() => setHover(n)}
            onMouseLeave={() => setHover(0)}
          >
            <Star
              className={`h-6 w-6 ${n <= (hover || rating) ? 'fill-flame-500 text-flame-500' : 'text-gray-300'}`}
            />
          </button>
        ))}
      </div>
      <textarea
        value={comment}
        onChange={(e) => setComment(e.target.value)}
        rows={3}
        maxLength={2000}
        placeholder="Share how this part worked for you (optional)"
        className="mt-3 w-full rounded-lg border border-gray-300 p-2 text-sm focus:border-brand-500 focus:outline-none"
      />
      {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
      <button
        onClick={submit}
        disabled={submitting}
        className="mt-2 inline-flex items-center gap-2 rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-60"
      >
        {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
        Submit review
      </button>
    </div>
  )
}
