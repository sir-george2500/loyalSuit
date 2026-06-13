'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import * as yup from 'yup'
import { Building2, Globe, Warehouse, Check, Loader2, ArrowLeft, ArrowRight } from 'lucide-react'
import type { AxiosError } from 'axios'
import { onboardingApi } from '@/lib/api/onboarding'

const CURRENCIES = ['RWF', 'USD', 'EUR', 'GBP', 'NGN', 'GHS', 'KES', 'ZAR', 'INR', 'CAD', 'AUD', 'JPY', 'CNY']
const COUNTRIES = [
  { code: '', name: '— Select —' },
  { code: 'US', name: 'United States' },
  { code: 'GB', name: 'United Kingdom' },
  { code: 'NG', name: 'Nigeria' },
  { code: 'GH', name: 'Ghana' },
  { code: 'KE', name: 'Kenya' },
  { code: 'ZA', name: 'South Africa' },
  { code: 'IN', name: 'India' },
  { code: 'CA', name: 'Canada' },
  { code: 'AU', name: 'Australia' },
]

const schema = yup.object({
  businessName: yup.string().trim().required('Business name is required').max(255),
  phone: yup.string().trim().max(40).optional(),
  country: yup.string().matches(/^([A-Za-z]{2})?$/, 'Select a valid country').optional(),
  currency: yup.string().required('Currency is required').length(3),
  timezone: yup.string().trim().required('Timezone is required'),
  warehouseName: yup.string().trim().required('Warehouse name is required').max(255),
  warehouseAddress: yup.string().trim().max(1000).optional(),
})

type FormData = yup.InferType<typeof schema>

const STEPS = [
  { title: 'Company', icon: Building2, fields: ['businessName', 'phone', 'country'] as const },
  { title: 'Localization', icon: Globe, fields: ['currency', 'timezone'] as const },
  { title: 'First warehouse', icon: Warehouse, fields: ['warehouseName', 'warehouseAddress'] as const },
]

function detectTimezone(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
  } catch {
    return 'UTC'
  }
}

export default function OnboardingWizard() {
  const router = useRouter()
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)
  const [step, setStep] = useState(0)

  const {
    register,
    handleSubmit,
    trigger,
    reset,
    formState: { errors },
  } = useForm<FormData>({
    resolver: yupResolver(schema),
    mode: 'onChange',
    defaultValues: { currency: 'RWF', timezone: 'UTC' },
  })

  // Pre-fill from any existing tenant profile; bail out if already onboarded.
  useEffect(() => {
    let active = true
    onboardingApi
      .getStatus()
      .then(({ data }) => {
        if (!active) return
        const s = data.data
        if (s.onboarded) {
          router.replace('/admin/dashboard')
          return
        }
        reset({
          businessName: s.businessName ?? '',
          currency: s.currency || 'RWF',
          country: s.country ?? '',
          phone: s.phone ?? '',
          timezone: s.timezone && s.timezone !== 'UTC' ? s.timezone : detectTimezone(),
          warehouseName: '',
          warehouseAddress: '',
        })
        setLoading(false)
      })
      .catch(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [reset, router])

  const next = async () => {
    const valid = await trigger([...STEPS[step].fields])
    if (valid) setStep((s) => Math.min(s + 1, STEPS.length - 1))
  }

  const onSubmit = async (data: FormData) => {
    setSubmitting(true)
    setServerError(null)
    try {
      await onboardingApi.complete({
        businessName: data.businessName,
        phone: data.phone || undefined,
        country: data.country || undefined,
        currency: data.currency,
        timezone: data.timezone,
        warehouseName: data.warehouseName,
        warehouseAddress: data.warehouseAddress || undefined,
      })
      router.replace('/admin/dashboard')
      router.refresh()
    } catch (err) {
      const axiosErr = err as AxiosError<{ message?: string }>
      setServerError(
        axiosErr.response?.data?.message ?? 'Could not complete setup. Please try again.'
      )
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loader2 className="h-6 w-6 animate-spin text-primary" />
      </div>
    )
  }

  const isLast = step === STEPS.length - 1

  return (
    <div className="mx-auto flex min-h-screen max-w-2xl flex-col justify-center p-4">
      <div className="mb-6 text-center">
        <h1 className="text-2xl font-bold">Set up your store</h1>
        <p className="text-sm text-base-content/60">A few details to get you selling</p>
      </div>

      {/* Step indicator */}
      <ul className="steps mb-6 w-full">
        {STEPS.map((s, i) => (
          <li key={s.title} className={`step ${i <= step ? 'step-primary' : ''}`}>
            {s.title}
          </li>
        ))}
      </ul>

      <div className="card bg-base-100 shadow-xl">
        <div className="card-body">
          {serverError && (
            <div role="alert" className="alert alert-error text-sm">
              <span>{serverError}</span>
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
            {/* Step 1 — Company */}
            {step === 0 && (
              <>
                <Field label="Business name" error={errors.businessName?.message}>
                  <input
                    {...register('businessName')}
                    className={`input input-bordered w-full ${errors.businessName ? 'input-error' : ''}`}
                    placeholder="Acme Stores"
                  />
                </Field>
                <Field label="Contact phone" hint="optional" error={errors.phone?.message}>
                  <input
                    {...register('phone')}
                    className={`input input-bordered w-full ${errors.phone ? 'input-error' : ''}`}
                    placeholder="+1 555 0100"
                  />
                </Field>
                <Field label="Country" hint="optional" error={errors.country?.message}>
                  <select {...register('country')} className="select select-bordered w-full">
                    {COUNTRIES.map((c) => (
                      <option key={c.code} value={c.code}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </Field>
              </>
            )}

            {/* Step 2 — Localization */}
            {step === 1 && (
              <>
                <Field label="Default currency" error={errors.currency?.message}>
                  <select
                    {...register('currency')}
                    className={`select select-bordered w-full ${errors.currency ? 'select-error' : ''}`}
                  >
                    {CURRENCIES.map((c) => (
                      <option key={c} value={c}>
                        {c}
                      </option>
                    ))}
                  </select>
                </Field>
                <Field label="Timezone" error={errors.timezone?.message}>
                  <input
                    {...register('timezone')}
                    className={`input input-bordered w-full ${errors.timezone ? 'input-error' : ''}`}
                    placeholder="America/New_York"
                  />
                </Field>
              </>
            )}

            {/* Step 3 — Warehouse */}
            {step === 2 && (
              <>
                <Field label="Warehouse name" error={errors.warehouseName?.message}>
                  <input
                    {...register('warehouseName')}
                    className={`input input-bordered w-full ${errors.warehouseName ? 'input-error' : ''}`}
                    placeholder="Main Warehouse"
                  />
                </Field>
                <Field label="Address" hint="optional" error={errors.warehouseAddress?.message}>
                  <textarea
                    {...register('warehouseAddress')}
                    className="textarea textarea-bordered w-full"
                    rows={2}
                    placeholder="123 Market St, Springfield"
                  />
                </Field>
              </>
            )}

            {/* Navigation */}
            <div className="flex items-center justify-between pt-2">
              <button
                type="button"
                onClick={() => setStep((s) => Math.max(s - 1, 0))}
                disabled={step === 0 || submitting}
                className="btn btn-ghost btn-sm gap-2"
              >
                <ArrowLeft className="h-4 w-4" /> Back
              </button>

              {isLast ? (
                <button type="submit" disabled={submitting} className="btn btn-primary gap-2">
                  {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Check className="h-4 w-4" />}
                  {submitting ? 'Finishing…' : 'Finish setup'}
                </button>
              ) : (
                <button type="button" onClick={next} className="btn btn-primary gap-2">
                  Continue <ArrowRight className="h-4 w-4" />
                </button>
              )}
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}

function Field({
  label,
  hint,
  error,
  children,
}: {
  label: string
  hint?: string
  error?: string
  children: React.ReactNode
}) {
  return (
    <div className="form-control">
      <label className="label">
        <span className="label-text font-medium">{label}</span>
        {hint && <span className="label-text-alt text-base-content/40">{hint}</span>}
      </label>
      {children}
      {error && <span className="mt-1 text-xs text-error">{error}</span>}
    </div>
  )
}
