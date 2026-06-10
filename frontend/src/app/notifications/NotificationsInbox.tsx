'use client'

import Link from 'next/link'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { useState } from 'react'
import { Bell, CheckCheck, Package, Truck, Wallet, Info, ChevronLeft, ChevronRight } from 'lucide-react'
import { notificationApi } from '@/lib/api/notifications'
import type { AppNotification, NotificationType } from '@/types'

const ICON: Record<NotificationType, typeof Package> = {
  ORDER_PLACED: Package,
  ORDER_DELIVERED: Truck,
  PAYOUT_PAID: Wallet,
  GENERAL: Info,
}

function timeAgo(iso: string): string {
  return new Date(iso).toLocaleString()
}

export default function NotificationsInbox() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['notifications', page],
    queryFn: async () => (await notificationApi.list(page)).data.data,
    placeholderData: keepPreviousData,
    retry: false,
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['notifications'] })

  const markRead = useMutation({
    mutationFn: (id: string) => notificationApi.markRead(id),
    onSuccess: invalidate,
  })
  const markAll = useMutation({
    mutationFn: () => notificationApi.markAllRead(),
    onSuccess: invalidate,
  })

  if (isError && (error as AxiosError)?.response?.status === 401) {
    return <div className="mx-auto max-w-md px-4 py-20 text-center text-gray-600">Please sign in to view your notifications.</div>
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-10 sm:px-6">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-2xl font-bold text-gray-900">
          <Bell className="h-6 w-6" /> Notifications
        </h1>
        <button
          className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-800 disabled:opacity-50"
          onClick={() => markAll.mutate()}
          disabled={markAll.isPending || !data || data.content.every((n) => n.read)}
        >
          <CheckCheck className="h-4 w-4" /> Mark all read
        </button>
      </div>

      <div className="mt-6 space-y-2">
        {isLoading ? (
          Array.from({ length: 4 }).map((_, i) => <div key={i} className="h-16 animate-pulse rounded-lg bg-gray-100" />)
        ) : data && data.content.length > 0 ? (
          data.content.map((n) => <Row key={n.id} n={n} onRead={() => markRead.mutate(n.id)} />)
        ) : (
          <div className="flex flex-col items-center gap-2 py-16 text-center text-gray-400">
            <Bell className="h-10 w-10" /><p className="text-sm">No notifications yet.</p>
          </div>
        )}
      </div>

      {data && data.totalElements > 0 && (
        <div className="mt-4 flex items-center justify-between text-sm text-gray-500">
          <span>Page {data.page + 1} of {Math.max(data.totalPages, 1)}</span>
          <div className="flex gap-2">
            <button className="rounded border px-2 py-1 disabled:opacity-50" onClick={() => setPage((p) => Math.max(p - 1, 0))} disabled={data.first}>
              <ChevronLeft className="h-4 w-4" />
            </button>
            <button className="rounded border px-2 py-1 disabled:opacity-50" onClick={() => setPage((p) => p + 1)} disabled={data.last}>
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

function Row({ n, onRead }: { n: AppNotification; onRead: () => void }) {
  const Icon = ICON[n.type] ?? Info
  const content = (
    <div className={`flex items-start gap-3 rounded-lg border p-3 ${n.read ? 'border-gray-200 bg-white' : 'border-gray-900/10 bg-gray-50'}`}>
      <span className={`mt-0.5 ${n.read ? 'text-gray-400' : 'text-gray-700'}`}><Icon className="h-5 w-5" /></span>
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <span className={`font-medium ${n.read ? 'text-gray-700' : 'text-gray-900'}`}>{n.title}</span>
          {!n.read && <span className="h-2 w-2 rounded-full bg-blue-500" />}
        </div>
        {n.body && <p className="text-sm text-gray-600">{n.body}</p>}
        <p className="mt-0.5 text-xs text-gray-400">{timeAgo(n.createdAt)}</p>
      </div>
    </div>
  )
  const handle = () => { if (!n.read) onRead() }
  return n.link ? (
    <Link href={n.link} onClick={handle} className="block">{content}</Link>
  ) : (
    <button onClick={handle} className="block w-full text-left">{content}</button>
  )
}
