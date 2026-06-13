'use client'

import { useMemo, useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import type { AxiosError } from 'axios'
import { Plus, Pencil, Package, AlertCircle, Loader2, ChevronLeft, ChevronRight, MoreVertical } from 'lucide-react'
import { categoryApi, productApi } from '@/lib/api/catalog'
import { productSchema, slugify, type ProductFormData } from '@/lib/validations/catalog'
import type { Category, Product, ProductStatus } from '@/types'

const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'RWF' })

const STATUS_BADGE: Record<ProductStatus, string> = {
  DRAFT: 'badge-ghost',
  ACTIVE: 'badge-success',
  INACTIVE: 'badge-warning',
  ARCHIVED: 'badge-neutral',
}

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function SellerProductsView() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<Product | 'new' | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['seller-products', page],
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

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['seller-products'] })

  const status = useMutation({
    mutationFn: ({ id, action }: { id: string; action: 'publish' | 'unpublish' | 'archive' }) =>
      productApi[action](id),
    onSuccess: () => { invalidate(); setActionError(null) },
    onError: (err) => setActionError(errorMessage(err, 'Could not update the product.')),
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load your products. Try again.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <button className="btn btn-primary btn-sm gap-2" onClick={() => { setActionError(null); setEditing('new') }}>
          <Plus className="h-4 w-4" /> New product
        </button>
      </div>

      {actionError && <div role="alert" className="alert alert-error text-sm"><span>{actionError}</span></div>}

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr><th>Name</th><th>Category</th><th className="text-right">Price</th><th>Status</th><th className="text-right">Actions</th></tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i}><td colSpan={5}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
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
                  <td><span className={`badge badge-sm ${STATUS_BADGE[p.status]}`}>{p.status}</span></td>
                  <td>
                    <div className="flex items-center justify-end gap-1">
                      <button className="btn btn-ghost btn-xs" onClick={() => { setActionError(null); setEditing(p) }} aria-label={`Edit ${p.name}`}>
                        <Pencil className="h-3.5 w-3.5" />
                      </button>
                      <div className="dropdown dropdown-end">
                        <button tabIndex={0} className="btn btn-ghost btn-xs" aria-label="Status actions">
                          <MoreVertical className="h-3.5 w-3.5" />
                        </button>
                        <ul tabIndex={0} className="menu dropdown-content z-10 w-40 rounded-box bg-base-100 p-2 shadow">
                          {p.status !== 'ACTIVE' && p.status !== 'ARCHIVED' && (
                            <li><button onClick={() => status.mutate({ id: p.id, action: 'publish' })}>Publish</button></li>
                          )}
                          {p.status === 'ACTIVE' && (
                            <li><button onClick={() => status.mutate({ id: p.id, action: 'unpublish' })}>Unpublish</button></li>
                          )}
                          {p.status !== 'ARCHIVED' && (
                            <li><button onClick={() => status.mutate({ id: p.id, action: 'archive' })}>Archive</button></li>
                          )}
                        </ul>
                      </div>
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={5}>
                <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                  <Package className="h-8 w-8" /><p className="text-sm">You haven’t added any products yet.</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalElements > 0 && (
        <div className="flex items-center justify-between text-sm text-base-content/60">
          <span>
            Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} product{data.totalElements === 1 ? '' : 's'}
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

      {editing && (
        <SellerProductFormModal
          target={editing}
          categories={categories ?? []}
          onClose={() => setEditing(null)}
          onSaved={() => { invalidate(); setEditing(null) }}
        />
      )}
    </div>
  )
}

function SellerProductFormModal({
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

        {serverError && <div role="alert" className="alert alert-error mb-3 text-sm"><span>{serverError}</span></div>}

        <form
          onSubmit={handleSubmit((form) => { setServerError(null); save.mutate(form) })}
          noValidate
          className="space-y-3"
        >
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <label className="form-control">
              <span className="label-text font-medium">Name</span>
              <input
                {...nameField}
                onChange={(e) => { nameField.onChange(e); if (isNew && !getValues('slug')) setValue('slug', slugify(e.target.value)) }}
                className={`input input-bordered w-full ${errors.name ? 'input-error' : ''}`}
                placeholder="Wireless mouse"
              />
              {errors.name && <span className="mt-1 text-xs text-error">{errors.name.message}</span>}
            </label>
            <label className="form-control">
              <span className="label-text font-medium">Slug</span>
              <input {...register('slug')} className={`input input-bordered w-full font-mono ${errors.slug ? 'input-error' : ''}`} placeholder="wireless-mouse" />
              {errors.slug && <span className="mt-1 text-xs text-error">{errors.slug.message}</span>}
            </label>
          </div>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
            <label className="form-control">
              <span className="label-text font-medium">Price</span>
              <input type="number" step="0.01" {...register('price')} className={`input input-bordered w-full ${errors.price ? 'input-error' : ''}`} />
              {errors.price && <span className="mt-1 text-xs text-error">{errors.price.message}</span>}
            </label>
            <label className="form-control">
              <span className="label-text font-medium">Compare-at</span>
              <input type="number" step="0.01" {...register('compareAtPrice')} className="input input-bordered w-full" />
            </label>
            <label className="form-control">
              <span className="label-text font-medium">Category</span>
              <select {...register('categoryId')} className="select select-bordered w-full">
                <option value="">— None —</option>
                {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </label>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <label className="form-control">
              <span className="label-text font-medium">SKU</span>
              <input {...register('sku')} className="input input-bordered w-full" />
            </label>
            <label className="form-control">
              <span className="label-text font-medium">Barcode</span>
              <input {...register('barcode')} className="input input-bordered w-full" />
            </label>
          </div>

          <label className="form-control">
            <span className="label-text font-medium">Description</span>
            <textarea {...register('description')} className="textarea textarea-bordered w-full" rows={2} />
          </label>

          <label className="label cursor-pointer justify-start gap-3">
            <input type="checkbox" {...register('digital')} className="checkbox checkbox-sm" />
            <span className="label-text">Digital product (no shipping)</span>
          </label>

          <div className="modal-action">
            <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
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
