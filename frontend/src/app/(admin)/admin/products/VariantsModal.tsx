'use client'

import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import type { AxiosError } from 'axios'
import { Loader2, Trash2, Pencil, Plus, X, Layers } from 'lucide-react'
import { variantApi } from '@/lib/api/catalog'
import { variantSchema, type VariantFormData } from '@/lib/validations/catalog'
import type { Product, ProductVariant } from '@/types'

const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function VariantsModal({ product, onClose }: { product: Product; onClose: () => void }) {
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState<ProductVariant | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [listError, setListError] = useState<string | null>(null)

  const variantsKey = ['variants', product.id]
  const { data: variants, isLoading } = useQuery({
    queryKey: variantsKey,
    queryFn: async () => (await variantApi.list(product.id)).data.data,
  })

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<VariantFormData>({
    resolver: yupResolver(variantSchema),
    defaultValues: { name: '', sku: '', price: undefined },
  })

  const startEdit = (v: ProductVariant) => {
    setEditing(v)
    setFormError(null)
    reset({ name: v.name, sku: v.sku ?? '', price: v.price })
  }

  const startNew = () => {
    setEditing(null)
    setFormError(null)
    reset({ name: '', sku: '', price: undefined })
  }

  const invalidate = () => queryClient.invalidateQueries({ queryKey: variantsKey })

  const save = useMutation({
    mutationFn: (form: VariantFormData) => {
      const payload = { name: form.name, sku: form.sku || undefined, price: form.price }
      return editing
        ? variantApi.update(product.id, editing.id, payload)
        : variantApi.create(product.id, payload)
    },
    onSuccess: () => {
      invalidate()
      startNew()
    },
    onError: (err) => setFormError(errorMessage(err, 'Could not save the variant.')),
  })

  const remove = useMutation({
    mutationFn: (variantId: string) => variantApi.remove(product.id, variantId),
    onSuccess: () => {
      invalidate()
      setListError(null)
      if (editing) startNew()
    },
    onError: (err) => setListError(errorMessage(err, 'Could not delete the variant.')),
  })

  return (
    <dialog className="modal modal-open">
      <div className="modal-box max-w-2xl">
        <div className="mb-3 flex items-start justify-between">
          <div>
            <h3 className="text-lg font-bold">Variants</h3>
            <p className="text-sm text-base-content/60">{product.name}</p>
          </div>
          <button className="btn btn-ghost btn-sm btn-circle" onClick={onClose} aria-label="Close">
            <X className="h-4 w-4" />
          </button>
        </div>

        {listError && (
          <div role="alert" className="alert alert-error mb-3 text-sm">
            <span>{listError}</span>
          </div>
        )}

        {/* Existing variants */}
        <div className="mb-4 overflow-x-auto rounded-box border border-base-300">
          <table className="table table-sm">
            <thead>
              <tr>
                <th>Name</th>
                <th>SKU</th>
                <th className="text-right">Price</th>
                <th className="text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={4} className="text-center text-base-content/50">
                    <span className="loading loading-spinner loading-sm" />
                  </td>
                </tr>
              ) : variants && variants.length > 0 ? (
                variants.map((v) => (
                  <tr key={v.id} className={editing?.id === v.id ? 'bg-base-200' : ''}>
                    <td>{v.name}</td>
                    <td className="font-mono text-xs text-base-content/60">{v.sku ?? '—'}</td>
                    <td className="text-right">{money.format(v.price)}</td>
                    <td>
                      <div className="flex justify-end gap-1">
                        <button className="btn btn-ghost btn-xs" onClick={() => startEdit(v)} aria-label={`Edit ${v.name}`}>
                          <Pencil className="h-3.5 w-3.5" />
                        </button>
                        <button
                          className="btn btn-ghost btn-xs text-error"
                          disabled={remove.isPending}
                          onClick={() => remove.mutate(v.id)}
                          aria-label={`Delete ${v.name}`}
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={4}>
                    <div className="flex flex-col items-center gap-1 py-6 text-center text-base-content/50">
                      <Layers className="h-6 w-6" />
                      <p className="text-sm">No variants yet.</p>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Add / edit form */}
        <div className="rounded-box border border-base-300 p-3">
          <p className="mb-2 text-sm font-medium">{editing ? `Edit “${editing.name}”` : 'Add a variant'}</p>
          {formError && (
            <div role="alert" className="alert alert-error mb-2 text-sm">
              <span>{formError}</span>
            </div>
          )}
          <form
            onSubmit={handleSubmit((form) => {
              setFormError(null)
              save.mutate(form)
            })}
            noValidate
            className="flex flex-wrap items-end gap-2"
          >
            <div className="form-control flex-1">
              <label className="label py-1" htmlFor="v-name">
                <span className="label-text text-xs">Name</span>
              </label>
              <input
                id="v-name"
                {...register('name')}
                className={`input input-bordered input-sm w-full ${errors.name ? 'input-error' : ''}`}
                placeholder="Size M"
              />
            </div>
            <div className="form-control">
              <label className="label py-1" htmlFor="v-sku">
                <span className="label-text text-xs">SKU</span>
              </label>
              <input id="v-sku" {...register('sku')} className="input input-bordered input-sm w-28" />
            </div>
            <div className="form-control">
              <label className="label py-1" htmlFor="v-price">
                <span className="label-text text-xs">Price</span>
              </label>
              <input
                id="v-price"
                type="number"
                step="0.01"
                {...register('price')}
                className={`input input-bordered input-sm w-24 ${errors.price ? 'input-error' : ''}`}
              />
            </div>
            <div className="flex gap-1">
              {editing && (
                <button type="button" className="btn btn-ghost btn-sm" onClick={startNew}>
                  Cancel
                </button>
              )}
              <button type="submit" className="btn btn-primary btn-sm gap-1" disabled={save.isPending}>
                {save.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Plus className="h-4 w-4" />}
                {editing ? 'Save' : 'Add'}
              </button>
            </div>
          </form>
          {(errors.name || errors.price) && (
            <p className="mt-1 text-xs text-error">{errors.name?.message ?? errors.price?.message}</p>
          )}
        </div>

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
