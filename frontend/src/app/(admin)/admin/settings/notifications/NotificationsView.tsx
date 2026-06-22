'use client'

import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { AlertCircle, Loader2, Check } from 'lucide-react'
import { notificationSettingsApi } from '@/lib/api/notificationSettings'
import type { NotificationPreferences } from '@/types'

const FIELDS: { key: keyof NotificationPreferences; label: string; description: string }[] = [
  { key: 'orderConfirmationEmail', label: 'Order confirmation email', description: 'Email the customer when an order is placed.' },
  { key: 'orderStatusEmail', label: 'Order status updates', description: 'Email the customer when an order is shipped or delivered.' },
  { key: 'lowStockAlert', label: 'Low-stock alerts', description: 'Notify your team when a product is running low.' },
  { key: 'newReviewAlert', label: 'New review alerts', description: 'Notify your team when a customer leaves a review.' },
  { key: 'payoutAlert', label: 'Payout alerts', description: 'Notify vendors when a payout is sent.' },
  { key: 'marketingEmail', label: 'Marketing emails', description: 'Send promotional campaigns to customers who opted in.' },
]

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function NotificationsView() {
  const queryClient = useQueryClient()
  const [form, setForm] = useState<NotificationPreferences | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['notification-settings'],
    queryFn: async () => (await notificationSettingsApi.get()).data.data,
  })

  useEffect(() => { if (data) setForm(data) }, [data])

  const save = useMutation({
    mutationFn: (payload: NotificationPreferences) => notificationSettingsApi.update(payload),
    onSuccess: (res) => {
      queryClient.setQueryData(['notification-settings'], res.data.data)
      setError(null); setSaved(true)
      setTimeout(() => setSaved(false), 2500)
    },
    onError: (err) => setError(errorMessage(err, 'Could not save your preferences.')),
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load notification settings. Try again.</p>
        </div>
      </div>
    )
  }

  if (isLoading || !form) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body space-y-3">
          {Array.from({ length: 6 }).map((_, i) => <div key={i} className="h-10 w-full animate-pulse rounded bg-base-200" />)}
        </div>
      </div>
    )
  }

  return (
    <div className="card max-w-2xl border border-base-300 bg-base-100">
      <div className="card-body space-y-1">
        {error && <div role="alert" className="alert alert-error text-sm"><span>{error}</span></div>}

        {FIELDS.map((f) => (
          <label key={f.key} className="flex cursor-pointer items-start justify-between gap-4 rounded-lg py-3 hover:bg-base-200/40">
            <div>
              <div className="font-medium">{f.label}</div>
              <div className="text-sm text-base-content/60">{f.description}</div>
            </div>
            <input
              type="checkbox"
              className="toggle toggle-primary mt-1"
              checked={form[f.key]}
              onChange={(e) => setForm({ ...form, [f.key]: e.target.checked })}
            />
          </label>
        ))}

        <div className="flex items-center justify-end gap-3 pt-2">
          {saved && <span className="flex items-center gap-1 text-sm text-success"><Check className="h-4 w-4" /> Saved</span>}
          <button className="btn btn-primary" disabled={save.isPending} onClick={() => { setError(null); save.mutate(form) }}>
            {save.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
            Save changes
          </button>
        </div>
      </div>
    </div>
  )
}
