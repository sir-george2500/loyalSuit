'use client'

import { useRef, useState } from 'react'
import Link from 'next/link'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import type { AxiosError } from 'axios'
import { Store, Loader2, ImagePlus, CheckCircle2, AlertTriangle } from 'lucide-react'
import { vendorApi, type UpdateStorefrontPayload } from '@/lib/api/vendors'
import type { Vendor } from '@/types'

const MAX_LOGO_BYTES = 5 * 1024 * 1024 // mirrors the server-side cap
const ACCEPTED = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']

function apiMessage(err: unknown, fallback: string) {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function SellerSettingsView() {
  const queryClient = useQueryClient()

  // 404 from /vendor/me means "not a vendor yet" — surface a call-to-action, not an error.
  const { data: vendor, isLoading } = useQuery<Vendor | null>({
    queryKey: ['vendor-me'],
    queryFn: async () => {
      try {
        return (await vendorApi.me()).data.data
      } catch {
        return null
      }
    },
    retry: false,
  })

  if (isLoading) {
    return (
      <div className="flex justify-center py-16">
        <Loader2 className="h-6 w-6 animate-spin text-primary" />
      </div>
    )
  }

  if (!vendor) {
    return (
      <div className="card bg-base-100 shadow">
        <div className="card-body items-center text-center">
          <Store className="h-10 w-10 text-primary" />
          <h2 className="text-lg font-bold">You don’t have a store yet</h2>
          <p className="text-sm text-base-content/60">Apply to sell on Loyal Spare Parts to set up your storefront.</p>
          <Link href="/become-seller" className="btn btn-primary btn-sm mt-2">Become a seller</Link>
        </div>
      </div>
    )
  }

  return <SettingsForm vendor={vendor} onSaved={() => queryClient.invalidateQueries({ queryKey: ['vendor-me'] })} />
}

function SettingsForm({ vendor, onSaved }: { vendor: Vendor; onSaved: () => void }) {
  const fileInput = useRef<HTMLInputElement>(null)
  const [logoError, setLogoError] = useState<string | null>(null)
  const [profileSaved, setProfileSaved] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors, isDirty },
  } = useForm<UpdateStorefrontPayload>({
    values: { storeName: vendor.storeName, description: vendor.description ?? '' },
  })

  const save = useMutation({
    mutationFn: (payload: UpdateStorefrontPayload) => vendorApi.updateMine(payload),
    onSuccess: () => {
      setProfileSaved(true)
      onSaved()
    },
  })

  const uploadLogo = useMutation({
    mutationFn: (file: File) => vendorApi.uploadLogo(file),
    onSuccess: () => {
      setLogoError(null)
      onSaved()
    },
  })

  function onPickLogo(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    e.target.value = '' // allow re-picking the same file after an error
    if (!file) return
    if (!ACCEPTED.includes(file.type)) {
      setLogoError('Choose a JPEG, PNG, GIF, or WEBP image.')
      return
    }
    if (file.size > MAX_LOGO_BYTES) {
      setLogoError('That image is larger than 5 MB.')
      return
    }
    setLogoError(null)
    uploadLogo.mutate(file)
  }

  return (
    <div className="grid gap-6 lg:grid-cols-3">
      {/* Logo */}
      <div className="card bg-base-100 shadow lg:col-span-1">
        <div className="card-body">
          <h2 className="card-title text-base">Store logo</h2>
          <p className="text-sm text-base-content/60">Shown on your storefront and product listings.</p>

          <div className="my-4 flex justify-center">
            {vendor.logoUrl ? (
              // eslint-disable-next-line @next/next/no-img-element -- remote Cloudinary asset, not a static import
              <img src={vendor.logoUrl} alt={`${vendor.storeName} logo`} className="h-32 w-32 rounded-lg object-cover ring-1 ring-base-300" />
            ) : (
              <div className="flex h-32 w-32 items-center justify-center rounded-lg bg-base-200 text-base-content/40">
                <Store className="h-10 w-10" />
              </div>
            )}
          </div>

          {logoError && (
            <div role="alert" className="alert alert-error mb-2 text-sm">
              <AlertTriangle className="h-4 w-4" />
              <span>{logoError}</span>
            </div>
          )}
          {uploadLogo.isError && (
            <div role="alert" className="alert alert-error mb-2 text-sm">
              <AlertTriangle className="h-4 w-4" />
              <span>{apiMessage(uploadLogo.error, 'Could not upload the logo.')}</span>
            </div>
          )}

          <input
            ref={fileInput}
            type="file"
            accept={ACCEPTED.join(',')}
            onChange={onPickLogo}
            className="hidden"
          />
          <button
            type="button"
            onClick={() => fileInput.current?.click()}
            disabled={uploadLogo.isPending}
            className="btn btn-outline btn-sm w-full"
          >
            {uploadLogo.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <ImagePlus className="h-4 w-4" />}
            {vendor.logoUrl ? 'Replace logo' : 'Upload logo'}
          </button>
          <p className="mt-1 text-center text-xs text-base-content/40">JPEG, PNG, GIF, or WEBP · up to 5 MB</p>
        </div>
      </div>

      {/* Profile */}
      <div className="card bg-base-100 shadow lg:col-span-2">
        <div className="card-body">
          <h2 className="card-title text-base">Storefront profile</h2>

          {save.isError && (
            <div role="alert" className="alert alert-error text-sm">
              <AlertTriangle className="h-4 w-4" />
              <span>{apiMessage(save.error, 'Could not save your changes.')}</span>
            </div>
          )}
          {save.isSuccess && profileSaved && (
            <div role="alert" className="alert alert-success text-sm">
              <CheckCircle2 className="h-4 w-4" />
              <span>Storefront updated.</span>
            </div>
          )}

          <form
            onSubmit={handleSubmit((data) => {
              setProfileSaved(false)
              save.mutate(data)
            })}
            noValidate
            className="space-y-4"
          >
            <div className="form-control">
              <label className="label" htmlFor="storeName"><span className="label-text font-medium">Store name</span></label>
              <input
                id="storeName"
                {...register('storeName', {
                  required: 'Store name is required',
                  maxLength: { value: 255, message: 'Store name is too long' },
                })}
                className={`input input-bordered w-full ${errors.storeName ? 'input-error' : ''}`}
              />
              {errors.storeName && <span className="mt-1 text-xs text-error">{errors.storeName.message}</span>}
            </div>

            <div className="form-control">
              <label className="label" htmlFor="description">
                <span className="label-text font-medium">About your store</span>
                <span className="label-text-alt text-base-content/40">optional</span>
              </label>
              <textarea
                id="description"
                rows={4}
                {...register('description', { maxLength: { value: 2000, message: 'Description is too long' } })}
                className={`textarea textarea-bordered w-full ${errors.description ? 'textarea-error' : ''}`}
              />
              {errors.description && <span className="mt-1 text-xs text-error">{errors.description.message}</span>}
            </div>

            <div className="form-control">
              <label className="label"><span className="label-text font-medium">Storefront address</span></label>
              <div className="join w-full">
                <span className="join-item flex items-center rounded-l-lg bg-base-200 px-3 text-sm text-base-content/60">/store/</span>
                <input value={vendor.slug} readOnly className="input input-bordered join-item w-full bg-base-200 text-base-content/60" />
              </div>
              <span className="mt-1 text-xs text-base-content/40">Your storefront link stays the same so existing links keep working.</span>
            </div>

            <button type="submit" disabled={save.isPending || !isDirty} className="btn btn-primary">
              {save.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              Save changes
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
