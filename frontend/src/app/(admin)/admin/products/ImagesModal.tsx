'use client'

import { useRef, useState } from 'react'
import Image from 'next/image'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { Loader2, Trash2, Upload, X, ImageOff, Star } from 'lucide-react'
import { mediaApi } from '@/lib/api/catalog'
import type { Product } from '@/types'

const MAX_BYTES = 5 * 1024 * 1024
const ACCEPTED = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function ImagesModal({ product, onClose }: { product: Product; onClose: () => void }) {
  const queryClient = useQueryClient()
  const fileInput = useRef<HTMLInputElement>(null)
  const [error, setError] = useState<string | null>(null)

  const mediaKey = ['media', product.id]
  const { data: images, isLoading } = useQuery({
    queryKey: mediaKey,
    queryFn: async () => (await mediaApi.list(product.id)).data.data,
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: mediaKey })

  const upload = useMutation({
    mutationFn: (file: File) => mediaApi.upload(product.id, file),
    onSuccess: () => {
      invalidate()
      setError(null)
      if (fileInput.current) fileInput.current.value = ''
    },
    onError: (err) => setError(errorMessage(err, 'Upload failed. Please try again.')),
  })

  const remove = useMutation({
    mutationFn: (mediaId: string) => mediaApi.remove(product.id, mediaId),
    onSuccess: () => {
      invalidate()
      setError(null)
    },
    onError: (err) => setError(errorMessage(err, 'Could not delete the image.')),
  })

  const onFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    // Mirror the server's checks for instant feedback (the server re-validates).
    if (!ACCEPTED.includes(file.type)) {
      setError('Only JPEG, PNG, GIF, or WEBP images are allowed.')
      e.target.value = ''
      return
    }
    if (file.size > MAX_BYTES) {
      setError('Image is too large (max 5 MB).')
      e.target.value = ''
      return
    }
    setError(null)
    upload.mutate(file)
  }

  return (
    <dialog className="modal modal-open">
      <div className="modal-box max-w-2xl">
        <div className="mb-3 flex items-start justify-between">
          <div>
            <h3 className="text-lg font-bold">Images</h3>
            <p className="text-sm text-base-content/60">{product.name}</p>
          </div>
          <button className="btn btn-ghost btn-sm btn-circle" onClick={onClose} aria-label="Close">
            <X className="h-4 w-4" />
          </button>
        </div>

        {error && (
          <div role="alert" className="alert alert-error mb-3 text-sm">
            <span>{error}</span>
          </div>
        )}

        <div className="mb-4">
          <input
            ref={fileInput}
            type="file"
            accept={ACCEPTED.join(',')}
            className="hidden"
            onChange={onFile}
          />
          <button
            className="btn btn-primary btn-sm gap-2"
            disabled={upload.isPending}
            onClick={() => fileInput.current?.click()}
          >
            {upload.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Upload className="h-4 w-4" />}
            {upload.isPending ? 'Uploading…' : 'Upload image'}
          </button>
          <span className="ml-2 text-xs text-base-content/50">JPEG, PNG, GIF, or WEBP · up to 5 MB</span>
        </div>

        {isLoading ? (
          <div className="flex justify-center py-10">
            <span className="loading loading-spinner text-primary" />
          </div>
        ) : images && images.length > 0 ? (
          <div className="grid grid-cols-3 gap-3 sm:grid-cols-4">
            {images.map((m) => (
              <div key={m.id} className="group relative aspect-square overflow-hidden rounded-box border border-base-300">
                <Image src={m.url} alt="" fill sizes="120px" className="object-cover" />
                {m.primary && (
                  <span className="absolute left-1 top-1 flex items-center gap-0.5 rounded bg-primary px-1.5 py-0.5 text-[0.6rem] font-medium text-primary-content">
                    <Star className="h-2.5 w-2.5" /> Primary
                  </span>
                )}
                <button
                  className="btn btn-error btn-xs btn-circle absolute right-1 top-1 opacity-0 transition group-hover:opacity-100"
                  disabled={remove.isPending}
                  onClick={() => remove.mutate(m.id)}
                  aria-label="Delete image"
                >
                  <Trash2 className="h-3 w-3" />
                </button>
              </div>
            ))}
          </div>
        ) : (
          <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
            <ImageOff className="h-8 w-8" />
            <p className="text-sm">No images yet. Upload your first one.</p>
          </div>
        )}

        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>
            Done
          </button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}
