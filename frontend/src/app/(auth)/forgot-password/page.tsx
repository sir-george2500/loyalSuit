'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import { Loader2, ShoppingCart, MailCheck, ArrowLeft } from 'lucide-react'
import { authApi } from '@/lib/api/auth'
import { forgotPasswordSchema, type ForgotPasswordFormData } from '@/lib/validations/auth'

export default function ForgotPasswordPage() {
  const [submitted, setSubmitted] = useState(false)
  const [loading, setLoading] = useState(false)

  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors },
  } = useForm<ForgotPasswordFormData>({
    resolver: yupResolver(forgotPasswordSchema),
    mode: 'onChange',
  })

  const onSubmit = async (data: ForgotPasswordFormData) => {
    setLoading(true)
    try {
      await authApi.forgotPassword(data.email)
    } catch {
      // Intentionally ignored: we show the same confirmation either way so the
      // page never reveals whether an account exists for that email.
    } finally {
      setSubmitted(true)
      setLoading(false)
    }
  }

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
          <div className="card-body">
            {submitted ? (
              <div className="text-center">
                <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-success/10 text-success">
                  <MailCheck className="h-6 w-6" />
                </div>
                <h1 className="text-xl font-bold">Check your email</h1>
                <p className="mt-2 text-sm text-base-content/60">
                  If an account exists for{' '}
                  <span className="font-medium">{getValues('email')}</span>, we&apos;ve sent a link
                  to reset your password. It expires shortly, so use it soon.
                </p>
                <Link href="/login" className="btn btn-ghost btn-sm mt-4 gap-2">
                  <ArrowLeft className="h-4 w-4" /> Back to sign in
                </Link>
              </div>
            ) : (
              <>
                <h1 className="text-2xl font-bold">Forgot your password?</h1>
                <p className="mb-2 text-sm text-base-content/60">
                  Enter your email and we&apos;ll send you a reset link.
                </p>

                <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-3">
                  <div className="form-control">
                    <label className="label" htmlFor="email">
                      <span className="label-text font-medium">Email address</span>
                    </label>
                    <input
                      id="email"
                      {...register('email')}
                      type="email"
                      autoComplete="email"
                      placeholder="you@example.com"
                      className={`input input-bordered w-full ${errors.email ? 'input-error' : ''}`}
                    />
                    {errors.email && (
                      <span className="mt-1 text-xs text-error">{errors.email.message}</span>
                    )}
                  </div>

                  <button type="submit" disabled={loading} className="btn btn-primary w-full">
                    {loading && <Loader2 className="h-4 w-4 animate-spin" />}
                    {loading ? 'Sending…' : 'Send reset link'}
                  </button>
                </form>

                <p className="mt-2 text-center text-sm text-base-content/60">
                  Remembered it?{' '}
                  <Link href="/login" className="link link-primary font-medium">
                    Sign in
                  </Link>
                </p>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
