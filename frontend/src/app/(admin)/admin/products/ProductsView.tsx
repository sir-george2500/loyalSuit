'use client'

import { useMemo, useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import type { AxiosError } from 'axios'
import {
  Plus, Pencil, Trash2, Package, AlertCircle, Loader2,
  ChevronLeft, ChevronRight, MoreVertical, Layers, ImageIcon, Boxes,
} from 'lucide-react'
import { categoryApi, productApi } from '@/lib/api/catalog'
import { productSchema, slugify, type ProductFormData } from '@/lib/validations/catalog'
import type { Category, Product, ProductStatus } from '@/types'
import VariantsModal from './VariantsModal'
import ImagesModal from './ImagesModal'
import StockModal from './StockModal'

const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })

const STATUS_BADGE: Record<ProductStatus, string> = {
  DRAFT: 'badge-ghost',
  ACTIVE: 'badge-success',
  INACTIVE: 'badge-warning',
  ARCHIVED: 'badge-neutral',
}

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function ProductsView() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<Product | 'new' | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<Product | null>(null)
  const [variantsTarget, setVariantsTarget] = useState<Product | null>(null)
  const [imagesTarget, setImagesTarget] = useState<Product | null>(null)
  const [stockTarget, setStockTarget] = useState<Product | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['products', page],
    queryFn: async () => (await productApi.list(page)).data.data,
    placeholderData: keepPreviousData,
  })

  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: async () => (await categoryApi.list()).data.data,
  })

  const categoryName = useMemo(() => {
    const map = new Map((categories ?? []).map((c) => [c.id, c.name]))
    return (id?: string | null) => (id ? (map.get(id) ?? '—') : '—')
  }, [categories])

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['products'] })

  const statusMutation = useMutation({
    mutationFn: ({ id, action }: { id: string; action: 'publish' | 'unpublish' | 'archive' }) =>
      productApi[action](id),
    onSuccess: () => {
      invalidate()
      setActionError(null)
    },
    onError: (err) => setActionError(errorMessage(err, 'Could not update the product status.')),
  })

  const removeMutation = useMutation({
    mutationFn: (id: string) => productApi.remove(id),
    onSuccess: () => {
      invalidate()
      setDeleteTarget(null)
      setActionError(null)
    },
    onError: (err) => setActionError(errorMessage(err, 'Could not delete the product.')),
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load products. Try again.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <button className="btn btn-primary btn-sm gap-2" onClick={() => setEditing('new')}>
          <Plus className="h-4 w-4" /> New product
        </button>
      </div>

      {actionError && (
        <div role="alert" className="alert alert-error text-sm">
          <span>{actionError}</span>
        </div>
      )}

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr>
              <th>Name</th>
              <th>Category</th>
              <th className="text-right">Price</th>
              <th>Status</th>
              <th className="text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 6 }).map((_, i) => (
                <tr key={i}>
                  <td colSpan={5}>
                    <div className="h-4 w-full animate-pulse rounded bg-base-200" />
                  </td>
                </tr>
              ))
            ) : data && data.content.length > 0 ? (
              data.content.map((p) => (
                <tr key={p.id}>
                  <td>
                    <div className="font-medium">{p.name}</div>
                    <div className="font-mono text-xs text-base-content/50">{p.slug}</div>
                  </td>
                  <td className="text-base-content/70">{categoryName(p.categoryId)}</td>
                  <td className="text-right">{money.format(p.price)}</td>
                  <td>
                    <span className={`badge badge-sm ${STATUS_BADGE[p.status]}`}>{p.status}</span>
                  </td>
                  <td>
                    <div className="flex items-center justify-end gap-1">
                      <button
                        className="btn btn-ghost btn-xs"
                        onClick={() => {
                          setActionError(null)
                          setEditing(p)
                        }}
                        aria-label={`Edit ${p.name}`}
                      >
                        <Pencil className="h-3.5 w-3.5" />
                      </button>

                      <button
                        className="btn btn-ghost btn-xs gap-1"
                        onClick={() => setImagesTarget(p)}
                        aria-label={`Images of ${p.name}`}
                      >
                        <ImageIcon className="h-3.5 w-3.5" /> Images
                      </button>

                      <button
                        className="btn btn-ghost btn-xs gap-1"
                        onClick={() => setVariantsTarget(p)}
                        aria-label={`Variants of ${p.name}`}
                      >
                        <Layers className="h-3.5 w-3.5" /> Variants
                      </button>

                      <button
                        className="btn btn-ghost btn-xs gap-1"
                        onClick={() => setStockTarget(p)}
                        aria-label={`Stock of ${p.name}`}
                      >
                        <Boxes className="h-3.5 w-3.5" /> Stock
                      </button>

                      <div className="dropdown dropdown-end">
                        <button tabIndex={0} className="btn btn-ghost btn-xs" aria-label="Status actions">
                          <MoreVertical className="h-3.5 w-3.5" />
                        </button>
                        <ul tabIndex={0} className="menu dropdown-content z-10 w-40 rounded-box bg-base-100 p-2 shadow">
                          {p.status !== 'ACTIVE' && p.status !== 'ARCHIVED' && (
                            <li>
                              <button onClick={() => statusMutation.mutate({ id: p.id, action: 'publish' })}>
                                Publish
                              </button>
                            </li>
                          )}
                          {p.status === 'ACTIVE' && (
                            <li>
                              <button onClick={() => statusMutation.mutate({ id: p.id, action: 'unpublish' })}>
                                Unpublish
                              </button>
                            </li>
                          )}
                          {p.status !== 'ARCHIVED' && (
                            <li>
                              <button onClick={() => statusMutation.mutate({ id: p.id, action: 'archive' })}>
                                Archive
                              </button>
                            </li>
                          )}
                        </ul>
                      </div>

                      <button
                        className="btn btn-ghost btn-xs text-error"
                        onClick={() => {
                          setActionError(null)
                          setDeleteTarget(p)
                        }}
                        aria-label={`Delete ${p.name}`}
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={5}>
                  <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                    <Package className="h-8 w-8" />
                    <p className="text-sm">No products yet. Add your first one.</p>
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-base-content/60">
          <span>
            Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} product
            {data.totalElements === 1 ? '' : 's'}
            {isFetching && <span className="loading loading-spinner loading-xs ml-2 align-middle text-primary" />}
          </span>
          <div className="join">
            <button className="btn btn-sm join-item" onClick={() => setPage((p) => Math.max(p - 1, 0))} disabled={data.first}>
              <ChevronLeft className="h-4 w-4" /> Prev
            </button>
            <button className="btn btn-sm join-item" onClick={() => setPage((p) => p + 1)} disabled={data.last}>
              Next <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}

      {imagesTarget && (
        <ImagesModal product={imagesTarget} onClose={() => setImagesTarget(null)} />
      )}

      {stockTarget && (
        <StockModal product={stockTarget} onClose={() => setStockTarget(null)} />
      )}

      {variantsTarget && (
        <VariantsModal product={variantsTarget} onClose={() => setVariantsTarget(null)} />
      )}

      {editing && (
        <ProductFormModal
          target={editing}
          categories={categories ?? []}
          onClose={() => setEditing(null)}
          onSaved={() => {
            invalidate()
            setEditing(null)
          }}
        />
      )}

      {deleteTarget && (
        <dialog className="modal modal-open">
          <div className="modal-box">
            <h3 className="text-lg font-bold">Delete “{deleteTarget.name}”?</h3>
            <p className="py-2 text-sm text-base-content/60">
              This permanently removes the product. Consider archiving instead if you may need it later.
            </p>
            <div className="modal-action">
              <button className="btn btn-ghost" onClick={() => setDeleteTarget(null)}>
                Cancel
              </button>
              <button
                className="btn btn-error"
                disabled={removeMutation.isPending}
                onClick={() => removeMutation.mutate(deleteTarget.id)}
              >
                {removeMutation.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
                Delete
              </button>
            </div>
          </div>
          <div className="modal-backdrop" onClick={() => setDeleteTarget(null)} />
        </dialog>
      )}
    </div>
  )
}

function ProductFormModal({
  target,
  categories,
  onClose,
  onSaved,
}: {
  target: Product | 'new'
  categories: Category[]
  onClose: () => void
  onSaved: () => void
}) {
  const isNew = target === 'new'
  const current = isNew ? null : target
  const [serverError, setServerError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    setValue,
    getValues,
    formState: { errors },
  } = useForm<ProductFormData>({
    resolver: yupResolver(productSchema),
    defaultValues: {
      name: current?.name ?? '',
      slug: current?.slug ?? '',
      price: current?.price ?? undefined,
      compareAtPrice: current?.compareAtPrice ?? undefined,
      sku: current?.sku ?? '',
      barcode: current?.barcode ?? '',
      categoryId: current?.categoryId ?? '',
      digital: current?.digital ?? false,
      description: current?.description ?? '',
    },
  })

  const save = useMutation({
    mutationFn: (form: ProductFormData) => {
      const payload = {
        name: form.name,
        slug: form.slug,
        price: form.price,
        compareAtPrice: form.compareAtPrice ?? undefined,
        sku: form.sku || undefined,
        barcode: form.barcode || undefined,
        categoryId: form.categoryId || undefined,
        digital: form.digital,
        description: form.description || undefined,
      }
      return isNew ? productApi.create(payload) : productApi.update(current!.id, payload)
    },
    onSuccess: onSaved,
    onError: (err) => setServerError(errorMessage(err, 'Could not save the product.')),
  })

  const nameField = register('name')

  return (
    <dialog className="modal modal-open">
      <div className="modal-box max-w-2xl">
        <h3 className="mb-3 text-lg font-bold">{isNew ? 'New product' : `Edit ${current?.name}`}</h3>

        {serverError && (
          <div role="alert" className="alert alert-error mb-3 text-sm">
            <span>{serverError}</span>
          </div>
        )}

        <form
          onSubmit={handleSubmit((form) => {
            setServerError(null)
            save.mutate(form)
          })}
          noValidate
          className="space-y-3"
        >
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <Field label="Name" error={errors.name?.message}>
              <input
                {...nameField}
                onChange={(e) => {
                  nameField.onChange(e)
                  if (isNew && !getValues('slug')) setValue('slug', slugify(e.target.value))
                }}
                className={`input input-bordered w-full ${errors.name ? 'input-error' : ''}`}
                placeholder="Wireless mouse"
              />
            </Field>
            <Field label="Slug" error={errors.slug?.message}>
              <input
                {...register('slug')}
                className={`input input-bordered w-full font-mono ${errors.slug ? 'input-error' : ''}`}
                placeholder="wireless-mouse"
              />
            </Field>
          </div>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
            <Field label="Price" error={errors.price?.message}>
              <input
                type="number"
                step="0.01"
                {...register('price')}
                className={`input input-bordered w-full ${errors.price ? 'input-error' : ''}`}
              />
            </Field>
            <Field label="Compare-at" hint="optional" error={errors.compareAtPrice?.message}>
              <input
                type="number"
                step="0.01"
                {...register('compareAtPrice')}
                className={`input input-bordered w-full ${errors.compareAtPrice ? 'input-error' : ''}`}
              />
            </Field>
            <Field label="Category" hint="optional">
              <select {...register('categoryId')} className="select select-bordered w-full">
                <option value="">— None —</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </Field>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Field label="SKU" hint="optional" error={errors.sku?.message}>
              <input {...register('sku')} className="input input-bordered w-full" />
            </Field>
            <Field label="Barcode" hint="optional" error={errors.barcode?.message}>
              <input {...register('barcode')} className="input input-bordered w-full" />
            </Field>
          </div>

          <Field label="Description" hint="optional">
            <textarea {...register('description')} className="textarea textarea-bordered w-full" rows={2} />
          </Field>

          <label className="label cursor-pointer justify-start gap-3">
            <input type="checkbox" {...register('digital')} className="checkbox checkbox-sm" />
            <span className="label-text">Digital product (no shipping)</span>
          </label>

          <div className="modal-action">
            <button type="button" className="btn btn-ghost" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={save.isPending}>
              {save.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              {isNew ? 'Create' : 'Save'}
            </button>
          </div>
        </form>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
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
