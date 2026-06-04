'use client'

import { Suspense, useState } from 'react'
import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import { Eye, EyeOff, Loader2, ShoppingCart, CheckCircle2, XCircle, ShieldAlert } from 'lucide-react'
import type { AxiosError } from 'axios'
import { authApi } from '@/lib/api/auth'
import { resetPasswordSchema, type ResetPasswordFormData } from '@/lib/validations/auth'

function PasswordChecks({ password }: { password: string }) {
  const checks = [
    { label: 'At least 8 characters', met: password.length >= 8 },
    { label: 'Uppercase letter', met: /[A-Z]/.test(password) },
    { label: 'Number', met: /[0-9]/.test(password) },
    { label: 'Special character', met: /[@$!%*?&#]/.test(password) },
  ]
  if (!password) return null
  return (
    <ul className="mt-2 grid grid-cols-2 gap-1">
      {checks.map(({ label, met }) => (
        <li
          key={label}
          className={`flex items-center gap-1.5 text-xs ${met ? 'text-success' : 'text-base-content/40'}`}
        >
          {met ? <CheckCircle2 className="h-3.5 w-3.5 shrink-0" /> : <XCircle className="h-3.5 w-3.5 shrink-0" />}
          {label}
        </li>
      ))}
    </ul>
  )
}

function ResetForm() {
  const router = useRouter()
  const token = useSearchParams().get('token')

  const [serverError, setServerError] = useState<string | null>(null)
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [done, setDone] = useState(false)

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<ResetPasswordFormData>({
    resolver: yupResolver(resetPasswordSchema),
    mode: 'onChange',
  })

  const passwordValue = watch('newPassword', '')

  const onSubmit = async (data: ResetPasswordFormData) => {
    if (!token) return
    setLoading(true)
    setServerError(null)
    try {
      await authApi.resetPassword(token, data.newPassword)
      setDone(true)
      setTimeout(() => router.replace('/login'), 2500)
    } catch (err) {
      const axiosErr = err as AxiosError<{ message?: string }>
      setServerError(
        axiosErr.response?.data?.message ??
          'We couldn’t reset your password. The link may have expired.'
      )
      setLoading(false)
    }
  }

  // No token in the URL → the link is malformed or was stripped.
  if (!token) {
    return (
      <Shell>
        <div className="text-center">
          <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-error/10 text-error">
            <ShieldAlert className="h-6 w-6" />
          </div>
          <h1 className="text-xl font-bold">Invalid reset link</h1>
          <p className="mt-2 text-sm text-base-content/60">
            This link is missing its token. Request a fresh one to continue.
          </p>
          <Link href="/forgot-password" className="btn btn-primary btn-sm mt-4">
            Request a new link
          </Link>
        </div>
      </Shell>
    )
  }

  if (done) {
    return (
      <Shell>
        <div className="text-center">
          <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-success/10 text-success">
            <CheckCircle2 className="h-6 w-6" />
          </div>
          <h1 className="text-xl font-bold">Password reset</h1>
          <p className="mt-2 text-sm text-base-content/60">
            Your password has been updated. Redirecting you to sign in…
          </p>
          <Link href="/login" className="btn btn-primary btn-sm mt-4">
            Sign in now
          </Link>
        </div>
      </Shell>
    )
  }

  return (
    <Shell>
      <h1 className="text-2xl font-bold">Choose a new password</h1>
      <p className="mb-2 text-sm text-base-content/60">Make it strong and unique.</p>

      {serverError && (
        <div role="alert" className="alert alert-error text-sm">
          <span>{serverError}</span>
          <Link href="/forgot-password" className="link link-neutral text-xs">
            Get a new link
          </Link>
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-3">
        <div className="form-control">
          <label className="label" htmlFor="newPassword">
            <span className="label-text font-medium">New password</span>
          </label>
          <div className="relative">
            <input
              id="newPassword"
              {...register('newPassword')}
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
              placeholder="••••••••"
              className={`input input-bordered w-full pr-10 ${errors.newPassword ? 'input-error' : ''}`}
            />
            <button
              type="button"
              onClick={() => setShowPassword((p) => !p)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-base-content/40 hover:text-base-content"
              tabIndex={-1}
              aria-label={showPassword ? 'Hide password' : 'Show password'}
            >
              {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          </div>
          <PasswordChecks password={passwordValue} />
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
            {...register('confirmPassword')}
            type={showPassword ? 'text' : 'password'}
            autoComplete="new-password"
            placeholder="••••••••"
            className={`input input-bordered w-full ${errors.confirmPassword ? 'input-error' : ''}`}
          />
          {errors.confirmPassword && (
            <span className="mt-1 text-xs text-error">{errors.confirmPassword.message}</span>
          )}
        </div>

        <button type="submit" disabled={loading} className="btn btn-primary w-full">
          {loading && <Loader2 className="h-4 w-4 animate-spin" />}
          {loading ? 'Resetting…' : 'Reset password'}
        </button>
      </form>

      <p className="mt-2 text-center text-sm text-base-content/60">
        <Link href="/login" className="link link-primary font-medium">
          Back to sign in
        </Link>
      </p>
    </Shell>
  )
}

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-base-200 p-4">
      <div className="w-full max-w-md">
        <div className="mb-6 text-center">
          <Link href="/" className="inline-flex items-center gap-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary text-primary-content">
              <ShoppingCart className="h-6 w-6" />
            </div>
            <span className="text-2xl font-bold">LoyalSuit</span>
          </Link>
        </div>
        <div className="card bg-base-100 shadow-xl">
          <div className="card-body">{children}</div>
        </div>
      </div>
    </div>
  )
}

export default function ResetPasswordPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center bg-base-200">
          <Loader2 className="h-6 w-6 animate-spin text-primary" />
        </div>
      }
    >
      <ResetForm />
    </Suspense>
  )
}
