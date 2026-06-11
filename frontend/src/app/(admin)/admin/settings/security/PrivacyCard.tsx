'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Loader2, Download, ShieldAlert } from 'lucide-react'
import type { AxiosError } from 'axios'
import { privacyApi } from '@/lib/api/privacy'
import { useAuthStore } from '@/stores/authStore'

function errorText(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function PrivacyCard() {
  const router = useRouter()
  const signOut = useAuthStore((s) => s.signOut)
  const [downloading, setDownloading] = useState(false)
  const [confirming, setConfirming] = useState(false)
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const download = async () => {
    setDownloading(true); setError(null)
    try {
      const res = await privacyApi.exportData()
      const blob = new Blob([JSON.stringify(res.data.data, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'my-loyalsuit-data.json'
      a.click()
      URL.revokeObjectURL(url)
    } catch (err) {
      setError(errorText(err, 'Could not export your data. Try again.'))
    } finally {
      setDownloading(false)
    }
  }

  const deleteAccount = async () => {
    setBusy(true); setError(null)
    try {
      await privacyApi.deleteAccount(password)
      signOut()
      router.push('/login')
      router.refresh()
    } catch (err) {
      setError(errorText(err, 'Could not delete your account. Check your password.'))
      setBusy(false)
    }
  }

  return (
    <div className="card bg-base-100 shadow-sm">
      <div className="card-body">
        <h2 className="card-title text-base">Your data &amp; privacy</h2>
        <p className="text-sm text-base-content/60">
          Download a copy of your personal data, or permanently delete your account.
        </p>

        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}

        <div className="mt-2 flex flex-wrap gap-2">
          <button className="btn btn-outline btn-sm gap-2" disabled={downloading} onClick={download}>
            {downloading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Download className="h-4 w-4" />}
            Download my data
          </button>
          {!confirming && (
            <button className="btn btn-outline btn-error btn-sm gap-2" onClick={() => { setError(null); setConfirming(true) }}>
              <ShieldAlert className="h-4 w-4" /> Delete my account
            </button>
          )}
        </div>

        {confirming && (
          <div className="mt-3 space-y-3 rounded-box border border-error/40 bg-error/5 p-3">
            <p className="text-sm font-medium text-error">
              This permanently deletes your account and removes your personal data. This can’t be undone.
            </p>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Confirm your password"
              autoComplete="current-password"
              className="input input-bordered input-sm w-full max-w-xs"
            />
            <div className="flex gap-2">
              <button className="btn btn-error btn-sm" disabled={busy || password === ''} onClick={deleteAccount}>
                {busy && <Loader2 className="h-4 w-4 animate-spin" />} Permanently delete
              </button>
              <button className="btn btn-ghost btn-sm" onClick={() => { setConfirming(false); setPassword(''); setError(null) }}>
                Cancel
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
