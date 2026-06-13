'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import { Eye, EyeOff, Loader2, CheckCircle2, XCircle } from 'lucide-react'
import Logo from '@/components/Logo'
import type { AxiosError } from 'axios'
import { authApi } from '@/lib/api/auth'
import { useAuthStore } from '@/stores/authStore'
import { homeForRole } from '@/lib/auth/roles'
import { registerSchema, type RegisterFormData } from '@/lib/validations/auth'

function PasswordStrengthIndicator({ password }: { password: string }) {
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

export default function RegisterPage() {
  const router = useRouter()
  const signIn = useAuthStore((s) => s.signIn)
  const [serverError, setServerError] = useState<string | null>(null)
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, touchedFields },
  } = useForm<RegisterFormData>({
    resolver: yupResolver(registerSchema),
    mode: 'onChange',
    reValidateMode: 'onChange',
  })

  const passwordValue = watch('password', '')

  const onSubmit = async (data: RegisterFormData) => {
    setLoading(true)
    setServerError(null)
    try {
      const res = await authApi.register({
        fullName: data.fullName,
        email: data.email,
        password: data.password,
      })
      const { token, expiresIn, user } = res.data.data
      // Registration always returns a full session (2FA can only be added later).
      if (!token || !user) throw new Error('Unexpected response')
      signIn(token, expiresIn, user)
      // Registration creates a shopper (CUSTOMER) on the Loyal Spare Parts marketplace, so this
      // lands in /store. Routing by role keeps it correct for any role.
      router.push(homeForRole(user.role))
      router.refresh()
    } catch (err) {
      const axiosErr = err as AxiosError<{ message?: string }>
      setServerError(
        axiosErr.response?.data?.message ?? 'Unable to create your account. Please try again.'
      )
      setLoading(false)
    }
  }

  const inputClass = (name: keyof RegisterFormData) =>
    `input input-bordered w-full ${errors[name] ? 'input-error' : ''}`

  return (
    <div className="flex min-h-screen items-center justify-center bg-base-200 p-4">
      <div className="w-full max-w-md">
        <div className="mb-6 text-center">
          <Logo href="/" className="mx-auto h-14" priority />
        </div>

        <div className="card bg-base-100 shadow-xl">
          <div className="card-body">
            <h1 className="text-2xl font-bold">Create your account</h1>
            <p className="mb-2 text-sm text-base-content/60">
              Shop Loyal Spare Parts and our sellers — want to sell? You can apply after signing up.
            </p>

            {serverError && (
              <div role="alert" className="alert alert-error text-sm">
                <span>{serverError}</span>
              </div>
            )}

            <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-3">
              <div className="form-control">
                <label className="label" htmlFor="fullName">
                  <span className="label-text font-medium">Full name</span>
                </label>
                <input
                  id="fullName"
                  {...register('fullName')}
                  type="text"
                  autoComplete="name"
                  placeholder="John Doe"
                  className={inputClass('fullName')}
                />
                {errors.fullName && (
                  <span className="mt-1 text-xs text-error">{errors.fullName.message}</span>
                )}
              </div>

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
                  className={inputClass('email')}
                />
                {errors.email && (
                  <span className="mt-1 text-xs text-error">{errors.email.message}</span>
                )}
              </div>

              <div className="form-control">
                <label className="label" htmlFor="password">
                  <span className="label-text font-medium">Password</span>
                </label>
                <div className="relative">
                  <input
                    id="password"
                    {...register('password')}
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="new-password"
                    placeholder="••••••••"
                    className={`${inputClass('password')} pr-10`}
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
                <PasswordStrengthIndicator password={passwordValue} />
              </div>

              <div className="form-control">
                <label className="label" htmlFor="confirmPassword">
                  <span className="label-text font-medium">Confirm password</span>
                </label>
                <input
                  id="confirmPassword"
                  {...register('confirmPassword')}
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="new-password"
                  placeholder="••••••••"
                  className={inputClass('confirmPassword')}
                />
                {errors.confirmPassword && (
                  <span className="mt-1 text-xs text-error">{errors.confirmPassword.message}</span>
                )}
                {touchedFields.confirmPassword && !errors.confirmPassword && (
                  <span className="mt-1 flex items-center gap-1 text-xs text-success">
                    <CheckCircle2 className="h-3.5 w-3.5" /> Passwords match
                  </span>
                )}
              </div>

              <button type="submit" disabled={loading} className="btn btn-primary w-full">
                {loading && <Loader2 className="h-4 w-4 animate-spin" />}
                {loading ? 'Creating account…' : 'Create account'}
              </button>
            </form>

            <p className="mt-2 text-center text-sm text-base-content/60">
              Already have an account?{' '}
              <Link href="/login" className="link link-primary font-medium">
                Sign in
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
