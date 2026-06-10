'use client'

import { useQuery } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { Share2, Loader2, Copy } from 'lucide-react'
import { affiliateApi } from '@/lib/api/affiliate'

const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })

export default function AffiliateDashboard() {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['affiliate-me'],
    queryFn: async () => (await affiliateApi.me()).data.data,
    retry: false,
  })

  if (isLoading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <Loader2 className="h-6 w-6 animate-spin text-gray-400" />
      </div>
    )
  }

  if (isError) {
    const status = (error as AxiosError)?.response?.status
    return (
      <div className="mx-auto max-w-md px-4 py-20 text-center text-gray-600">
        <Share2 className="mx-auto h-10 w-10 text-gray-300" />
        <p className="mt-4">
          {status === 404
            ? 'You’re not an affiliate yet. Ask the store to enrol you.'
            : status === 401
              ? 'Please sign in to view your affiliate dashboard.'
              : 'Couldn’t load your affiliate dashboard.'}
        </p>
      </div>
    )
  }

  if (!data) return null

  return (
    <div className="mx-auto max-w-2xl px-4 py-10 sm:px-6">
      <h1 className="flex items-center gap-2 text-2xl font-bold text-gray-900">
        <Share2 className="h-6 w-6" /> Affiliate dashboard
      </h1>
      <p className="mt-1 text-sm text-gray-500">Share your code; earn {data.rewardRate}% on orders it brings in.</p>

      <div className="mt-6 grid gap-4 sm:grid-cols-3">
        <Card label="Your code">
          <button
            className="inline-flex items-center gap-2 font-mono text-lg font-bold text-gray-900"
            onClick={() => navigator.clipboard?.writeText(data.code)}
            title="Copy code"
          >
            {data.code} <Copy className="h-4 w-4 text-gray-400" />
          </button>
        </Card>
        <Card label="Earned"><span className="text-2xl font-bold text-green-600">{money.format(data.earned)}</span></Card>
        <Card label="Reversed"><span className="text-2xl font-bold text-gray-500">{money.format(data.reversed)}</span></Card>
      </div>

      {!data.active && (
        <p className="mt-4 rounded-lg bg-amber-50 px-4 py-2 text-sm text-amber-800">
          Your affiliate account is currently inactive — new referrals won’t be attributed.
        </p>
      )}
    </div>
  )
}

function Card({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="rounded-lg border border-gray-200 p-4">
      <p className="text-sm text-gray-500">{label}</p>
      <div className="mt-1">{children}</div>
    </div>
  )
}
