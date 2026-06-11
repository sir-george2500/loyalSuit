'use client'

import { useState } from 'react'
import { Loader2, ShieldCheck, ShieldOff, Copy, Check } from 'lucide-react'
import type { AxiosError } from 'axios'
import { authApi } from '@/lib/api/auth'
import { useAuthStore } from '@/stores/authStore'

function errorText(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

type Mode = 'idle' | 'enrolling' | 'recovery' | 'disabling'

export default function TwoFactorCard() {
  const user = useAuthStore((s) => s.user)
  const setUser = useAuthStore((s) => s.setUser)
  const enabled = user?.twoFactorEnabled ?? false

  const [mode, setMode] = useState<Mode>('idle')
  const [secret, setSecret] = useState('')
  const [otpauthUri, setOtpauthUri] = useState('')
  const [code, setCode] = useState('')
  const [password, setPassword] = useState('')
  const [recoveryCodes, setRecoveryCodes] = useState<string[]>([])
  const [copied, setCopied] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const refreshUser = async () => {
    const me = await authApi.me()
    setUser(me.data.data)
  }

  const reset = () => {
    setMode('idle'); setSecret(''); setOtpauthUri(''); setCode(''); setPassword('')
    setRecoveryCodes([]); setError(null); setCopied(false)
  }

  const startEnrol = async () => {
    setBusy(true); setError(null)
    try {
      const res = await authApi.twoFactorSetup()
      setSecret(res.data.data.secret)
      setOtpauthUri(res.data.data.otpauthUri)
      setMode('enrolling')
    } catch (err) {
      setError(errorText(err, 'Could not start setup. Try again.'))
    } finally {
      setBusy(false)
    }
  }

  const confirmEnrol = async () => {
    setBusy(true); setError(null)
    try {
      const res = await authApi.twoFactorEnable(code.trim())
      setRecoveryCodes(res.data.data.recoveryCodes)
      setMode('recovery')
      await refreshUser()
    } catch (err) {
      setError(errorText(err, 'That code was not accepted. Try again.'))
    } finally {
      setBusy(false)
    }
  }

  const disable = async () => {
    setBusy(true); setError(null)
    try {
      await authApi.twoFactorDisable(password)
      await refreshUser()
      reset()
    } catch (err) {
      setError(errorText(err, 'Could not disable two-factor. Check your password.'))
    } finally {
      setBusy(false)
    }
  }

  const copySecret = () => {
    navigator.clipboard?.writeText(secret)
    setCopied(true)
    setTimeout(() => setCopied(false), 1500)
  }

  return (
    <div className="card bg-base-100 shadow-sm">
      <div className="card-body">
        <div className="flex items-center justify-between">
          <h2 className="card-title text-base">
            <ShieldCheck className="h-4 w-4" /> Two-factor authentication
          </h2>
          <span className={`badge ${enabled ? 'badge-success' : 'badge-ghost'}`}>
            {enabled ? 'Enabled' : 'Disabled'}
          </span>
        </div>

        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}

        {/* Enabled, idle → offer disable */}
        {enabled && mode === 'idle' && (
          <>
            <p className="text-sm text-base-content/60">
              Your account is protected with an authenticator app. You’ll enter a code each time you sign in.
            </p>
            <button className="btn btn-outline btn-error btn-sm mt-2 w-fit gap-2" onClick={() => { setError(null); setMode('disabling') }}>
              <ShieldOff className="h-4 w-4" /> Disable 2FA
            </button>
          </>
        )}

        {/* Disabled, idle → offer enable */}
        {!enabled && mode === 'idle' && (
          <>
            <p className="text-sm text-base-content/60">
              Add a second step at sign-in using an authenticator app (Google Authenticator, Authy, 1Password…).
            </p>
            <button className="btn btn-primary btn-sm mt-2 w-fit" disabled={busy} onClick={startEnrol}>
              {busy && <Loader2 className="h-4 w-4 animate-spin" />} Enable 2FA
            </button>
          </>
        )}

        {/* Enrolling → show secret + confirm with a code */}
        {mode === 'enrolling' && (
          <div className="space-y-3">
            <p className="text-sm text-base-content/70">
              Add this secret to your authenticator app (scan the URI as a QR, or enter the key manually), then enter the 6-digit code it shows.
            </p>
            <div className="join w-full max-w-sm">
              <input readOnly value={secret} className="input input-bordered input-sm join-item w-full font-mono tracking-widest" />
              <button className="btn btn-sm join-item" onClick={copySecret} aria-label="Copy secret">
                {copied ? <Check className="h-4 w-4 text-success" /> : <Copy className="h-4 w-4" />}
              </button>
            </div>
            <p className="break-all text-xs text-base-content/40">{otpauthUri}</p>
            <div className="form-control max-w-xs">
              <label className="label"><span className="label-text">Code from your app</span></label>
              <input value={code} onChange={(e) => setCode(e.target.value)} placeholder="123456"
                className="input input-bordered input-sm tracking-widest" autoComplete="one-time-code" />
            </div>
            <div className="flex gap-2">
              <button className="btn btn-primary btn-sm" disabled={busy || code.trim() === ''} onClick={confirmEnrol}>
                {busy && <Loader2 className="h-4 w-4 animate-spin" />} Confirm & enable
              </button>
              <button className="btn btn-ghost btn-sm" onClick={reset}>Cancel</button>
            </div>
          </div>
        )}

        {/* Recovery codes shown once */}
        {mode === 'recovery' && (
          <div className="space-y-3">
            <div role="alert" className="alert alert-success text-sm">
              <ShieldCheck className="h-4 w-4" />
              <span>Two-factor is on. Save these recovery codes somewhere safe — each works once if you lose your device.</span>
            </div>
            <ul className="grid grid-cols-2 gap-2 rounded-box border border-base-300 bg-base-200 p-3 font-mono text-sm">
              {recoveryCodes.map((rc) => <li key={rc}>{rc}</li>)}
            </ul>
            <button className="btn btn-primary btn-sm" onClick={reset}>I’ve saved my codes</button>
          </div>
        )}

        {/* Disabling → password confirm */}
        {mode === 'disabling' && (
          <div className="space-y-3">
            <p className="text-sm text-base-content/70">Enter your password to turn off two-factor authentication.</p>
            <div className="form-control max-w-xs">
              <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                placeholder="Your password" className="input input-bordered input-sm" autoComplete="current-password" />
            </div>
            <div className="flex gap-2">
              <button className="btn btn-error btn-sm" disabled={busy || password === ''} onClick={disable}>
                {busy && <Loader2 className="h-4 w-4 animate-spin" />} Disable
              </button>
              <button className="btn btn-ghost btn-sm" onClick={reset}>Cancel</button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
