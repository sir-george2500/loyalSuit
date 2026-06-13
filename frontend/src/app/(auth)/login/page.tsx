'use client'

import { Suspense, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import Link from 'next/link'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import { Eye, EyeOff, Loader2 } from 'lucide-react'
import Logo from '@/components/Logo'
import type { AxiosError } from 'axios'
import { authApi } from '@/lib/api/auth'
import { useAuthStore } from '@/stores/authStore'
import { homeForRole } from '@/lib/auth/roles'
import { loginSchema, type LoginFormData } from '@/lib/validations/auth'
import type { UserProfile } from '@/types'

// Only allow internal redirect targets — prevents open-redirect via ?next=https://evil.com
function safeNext(raw: string | null): string | null {
  if (raw && raw.startsWith('/') && !raw.startsWith('//')) return raw
  return null
}

function errorText(err: unknown): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ??
    'Unable to sign in. Please check your connection and try again.'
}

function LoginForm() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const next = safeNext(searchParams.get('next'))
  const signIn = useAuthStore((s) => s.signIn)

  const [serverError, setServerError] = useState<string | null>(null)
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  // When the account has 2FA, login returns a challenge instead of a token.
  const [mfaToken, setMfaToken] = useState<string | null>(null)
  const [mfaCode, setMfaCode] = useState('')

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: yupResolver(loginSchema),
    mode: 'onChange',
    reValidateMode: 'onChange',
  })

  const onSubmit = async (data: LoginFormData) => {
    setLoading(true)
    setServerError(null)
    try {
      const res = await authApi.login(data.email, data.password)
      const result = res.data.data
      if (result.mfaRequired && result.mfaToken) {
        // Hold here for the second factor rather than completing the session.
        setMfaToken(result.mfaToken)
        setLoading(false)
        return
      }
      land(result)
    } catch (err) {
      setServerError(errorText(err))
      setLoading(false)
    }
  }

  const land = (result: { token?: string; expiresIn: number; user?: UserProfile }) => {
    if (!result.token || !result.user) return
    signIn(result.token, result.expiresIn, result.user)
    // Honor an explicit, safe return target; otherwise land where the role belongs.
    router.push(next ?? homeForRole(result.user.role))
    router.refresh()
  }

  const submitMfa = async () => {
    if (!mfaToken) return
    setLoading(true)
    setServerError(null)
    try {
      const res = await authApi.completeMfa(mfaToken, mfaCode.trim())
      land(res.data.data)
    } catch (err) {
      setServerError(errorText(err))
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-base-200 p-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="mb-6 text-center">
          <Logo href="/" className="mx-auto h-14" priority />
        </div>

        <div className="card bg-base-100 shadow-xl">
          <div className="card-body">
            {mfaToken ? (
              <>
                <h1 className="text-2xl font-bold">Two-factor verification</h1>
                <p className="mb-2 text-sm text-base-content/60">
                  Enter the 6-digit code from your authenticator app, or a recovery code.
                </p>
                {serverError && (
                  <div role="alert" className="alert alert-error text-sm"><span>{serverError}</span></div>
                )}
                <div className="form-control">
                  <label className="label" htmlFor="mfaCode">
                    <span className="label-text font-medium">Verification code</span>
                  </label>
                  <input
                    id="mfaCode"
                    value={mfaCode}
                    onChange={(e) => setMfaCode(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && mfaCode.trim() && submitMfa()}
                    inputMode="text"
                    autoComplete="one-time-code"
                    autoFocus
                    placeholder="123456"
                    className="input input-bordered w-full tracking-widest"
                  />
                </div>
                <button
                  onClick={submitMfa}
                  disabled={loading || mfaCode.trim() === ''}
                  className="btn btn-primary mt-3 w-full"
                >
                  {loading && <Loader2 className="h-4 w-4 animate-spin" />}
                  {loading ? 'Verifying…' : 'Verify'}
                </button>
                <button
                  onClick={() => { setMfaToken(null); setMfaCode(''); setServerError(null) }}
                  className="btn btn-ghost btn-sm mt-1"
                >
                  Back to sign in
                </button>
              </>
            ) : (
            <>
            <h1 className="text-2xl font-bold">Welcome back</h1>
            <p className="mb-2 text-sm text-base-content/60">Sign in to your account</p>

            {serverError && (
              <div role="alert" className="alert alert-error text-sm">
                <span>{serverError}</span>
              </div>
            )}

            <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-3">
              {/* Email */}
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

              {/* Password */}
              <div className="form-control">
                <label className="label" htmlFor="password">
                  <span className="label-text font-medium">Password</span>
                  <Link href="/forgot-password" className="label-text-alt link link-primary">
                    Forgot password?
                  </Link>
                </label>
                <div className="relative">
                  <input
                    id="password"
                    {...register('password')}
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="current-password"
                    placeholder="••••••••"
                    className={`input input-bordered w-full pr-10 ${errors.password ? 'input-error' : ''}`}
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
                {errors.password && (
                  <span className="mt-1 text-xs text-error">{errors.password.message}</span>
                )}
              </div>

              <button type="submit" disabled={loading} className="btn btn-primary w-full">
                {loading && <Loader2 className="h-4 w-4 animate-spin" />}
                {loading ? 'Signing in…' : 'Sign in'}
              </button>
            </form>

            <p className="mt-2 text-center text-sm text-base-content/60">
              Don&apos;t have an account?{' '}
              <Link href="/register" className="link link-primary font-medium">
                Sign up free
              </Link>
            </p>
            </>
            )}
          </div>
        </div>

        {/* Test credentials hint */}
        <div role="alert" className="alert mt-4 block bg-warning/10 text-xs">
          <p className="mb-1 font-semibold text-warning">
            Test accounts (password: Admin@Test123)
          </p>
          <ul className="font-mono text-base-content/70">
            <li>superadmin@loyalsuit.dev</li>
            <li>tenantadmin@loyalsuit.dev</li>
            <li>vendor@loyalsuit.dev · customer@loyalsuit.dev · staff@loyalsuit.dev</li>
          </ul>
        </div>
      </div>
    </div>
  )
}

export default function LoginPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center bg-base-200">
          <Loader2 className="h-6 w-6 animate-spin text-primary" />
        </div>
      }
    >
      <LoginForm />
    </Suspense>
  )
}
