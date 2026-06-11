'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import { Loader2, ShieldCheck, KeyRound, LogOut, Clock } from 'lucide-react'
import type { AxiosError } from 'axios'
import { authApi } from '@/lib/api/auth'
import { useAuthStore } from '@/stores/authStore'
import { getToken, decodeJwt } from '@/lib/auth/session'
import { changePasswordSchema, type ChangePasswordFormData } from '@/lib/validations/auth'
import TwoFactorCard from './TwoFactorCard'

function SessionCard() {
  const token = typeof window !== 'undefined' ? getToken() : null
  const payload = token ? decodeJwt(token) : null
  const issued = payload?.iat ? new Date((payload.iat as number) * 1000) : null
  const expires = payload?.exp ? new Date((payload.exp as number) * 1000) : null

  return (
    <div className="card bg-base-100 shadow-sm">
      <div className="card-body">
        <h2 className="card-title text-base">
          <Clock className="h-4 w-4" /> Current session
        </h2>
        <dl className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
          <div>
            <dt className="text-xs text-base-content/50">Signed in</dt>
            <dd className="font-medium">{issued ? issued.toLocaleString() : '—'}</dd>
          </div>
          <div>
            <dt className="text-xs text-base-content/50">Expires</dt>
            <dd className="font-medium">{expires ? expires.toLocaleString() : '—'}</dd>
          </div>
        </dl>
        <p className="text-xs text-base-content/40">
          Multi-device session management and revocation arrive with the sessions module.
        </p>
      </div>
    </div>
  )
}

export default function SecuritySettingsPage() {
  const router = useRouter()
  const signOutStore = useAuthStore((s) => s.signOut)
  const [serverError, setServerError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ChangePasswordFormData>({
    resolver: yupResolver(changePasswordSchema),
    mode: 'onChange',
  })

  const onSubmit = async (data: ChangePasswordFormData) => {
    setLoading(true)
    setServerError(null)
    setSuccess(false)
    try {
      await authApi.changePassword({
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      })
      setSuccess(true)
      reset()
    } catch (err) {
      const axiosErr = err as AxiosError<{ message?: string }>
      setServerError(axiosErr.response?.data?.message ?? 'Could not update password. Try again.')
    } finally {
      setLoading(false)
    }
  }

  const signOut = () => {
    signOutStore()
    router.push('/login')
    router.refresh()
  }

  const fieldClass = (name: keyof ChangePasswordFormData) =>
    `input input-bordered w-full ${errors[name] ? 'input-error' : ''}`

  return (
    <div className="space-y-6">
      {/* Change password */}
      <div className="card bg-base-100 shadow-sm">
        <div className="card-body">
          <h2 className="card-title text-base">
            <KeyRound className="h-4 w-4" /> Change password
          </h2>

          {success && (
            <div role="alert" className="alert alert-success text-sm">
              <ShieldCheck className="h-4 w-4" />
              <span>Password updated successfully.</span>
            </div>
          )}
          {serverError && (
            <div role="alert" className="alert alert-error text-sm">
              <span>{serverError}</span>
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} noValidate className="max-w-md space-y-3">
            <div className="form-control">
              <label className="label" htmlFor="currentPassword">
                <span className="label-text font-medium">Current password</span>
              </label>
              <input
                id="currentPassword"
                type="password"
                autoComplete="current-password"
                className={fieldClass('currentPassword')}
                {...register('currentPassword')}
              />
              {errors.currentPassword && (
                <span className="mt-1 text-xs text-error">{errors.currentPassword.message}</span>
              )}
            </div>

            <div className="form-control">
              <label className="label" htmlFor="newPassword">
                <span className="label-text font-medium">New password</span>
              </label>
              <input
                id="newPassword"
                type="password"
                autoComplete="new-password"
                className={fieldClass('newPassword')}
                {...register('newPassword')}
              />
              {errors.newPassword && (
                <span className="mt-1 text-xs text-error">{errors.newPassword.message}</span>
              )}
            </div>

            <div className="form-control">
              <label className="label" htmlFor="confirmPassword">
                <span className="label-text font-medium">Confirm new password</span>
              </label>
              <input
                id="confirmPassword"
                type="password"
                autoComplete="new-password"
                className={fieldClass('confirmPassword')}
                {...register('confirmPassword')}
              />
              {errors.confirmPassword && (
                <span className="mt-1 text-xs text-error">{errors.confirmPassword.message}</span>
              )}
            </div>

            <button type="submit" disabled={loading} className="btn btn-primary">
              {loading && <Loader2 className="h-4 w-4 animate-spin" />}
              {loading ? 'Updating…' : 'Update password'}
            </button>
          </form>
        </div>
      </div>

      <SessionCard />

      <TwoFactorCard />

      {/* Sign out */}
      <div className="card bg-base-100 shadow-sm">
        <div className="card-body flex-row items-center justify-between">
          <div>
            <h2 className="card-title text-base">Sign out</h2>
            <p className="text-sm text-base-content/50">End your session on this device.</p>
          </div>
          <button onClick={signOut} className="btn btn-outline btn-error gap-2">
            <LogOut className="h-4 w-4" /> Sign out
          </button>
        </div>
      </div>
    </div>
  )
}
