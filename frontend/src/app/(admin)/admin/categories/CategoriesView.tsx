'use client'

import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import type { AxiosError } from 'axios'
import { Plus, Pencil, Trash2, FolderTree, AlertCircle, Loader2, CornerDownRight } from 'lucide-react'
import { categoryApi } from '@/lib/api/catalog'
import { categorySchema, slugify, type CategoryFormData } from '@/lib/validations/catalog'
import type { Category } from '@/types'

interface TreeRow {
  cat: Category
  depth: number
}

/** Flatten the categories into depth-ordered rows, sorted by sortOrder then name. */
function orderedTree(cats: Category[]): TreeRow[] {
  const ids = new Set(cats.map((c) => c.id))
  const byParent = new Map<string, Category[]>()
  for (const c of cats) {
    // Treat a parent that isn't in the set (shouldn't happen per-tenant) as a root.
    const key = c.parentId && ids.has(c.parentId) ? c.parentId : '__root__'
    byParent.set(key, [...(byParent.get(key) ?? []), c])
  }
  const sortSiblings = (a: Category, b: Category) =>
    a.sortOrder - b.sortOrder || a.name.localeCompare(b.name)

  const rows: TreeRow[] = []
  const walk = (key: string, depth: number) => {
    for (const cat of (byParent.get(key) ?? []).sort(sortSiblings)) {
      rows.push({ cat, depth })
      walk(cat.id, depth + 1)
    }
  }
  walk('__root__', 0)
  return rows
}

/** Ids of a category and all its descendants — invalid parent choices (would cycle). */
function descendantIds(rootId: string, cats: Category[]): Set<string> {
  const out = new Set<string>([rootId])
  let added = true
  while (added) {
    added = false
    for (const c of cats) {
      if (c.parentId && out.has(c.parentId) && !out.has(c.id)) {
        out.add(c.id)
        added = true
      }
    }
  }
  return out
}

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function CategoriesView() {
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState<Category | 'new' | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<Category | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const { data: categories, isLoading, isError } = useQuery({
    queryKey: ['categories'],
    queryFn: async () => (await categoryApi.list()).data.data,
  })

  const rows = useMemo(() => orderedTree(categories ?? []), [categories])

  const removeMutation = useMutation({
    mutationFn: (id: string) => categoryApi.remove(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categories'] })
      setDeleteTarget(null)
      setActionError(null)
    },
    onError: (err) => setActionError(errorMessage(err, 'Could not delete the category.')),
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load categories. Try again.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <button className="btn btn-primary btn-sm gap-2" onClick={() => setEditing('new')}>
          <Plus className="h-4 w-4" /> New category
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
              <th>Slug</th>
              <th className="text-center">Order</th>
              <th className="text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i}>
                  <td colSpan={4}>
                    <div className="h-4 w-full animate-pulse rounded bg-base-200" />
                  </td>
                </tr>
              ))
            ) : rows.length > 0 ? (
              rows.map(({ cat, depth }) => (
                <tr key={cat.id}>
                  <td>
                    <span className="flex items-center" style={{ paddingLeft: `${depth * 1.25}rem` }}>
                      {depth > 0 && <CornerDownRight className="mr-1 h-3.5 w-3.5 text-base-content/30" />}
                      {cat.name}
                    </span>
                  </td>
                  <td className="font-mono text-xs text-base-content/60">{cat.slug}</td>
                  <td className="text-center text-base-content/60">{cat.sortOrder}</td>
                  <td>
                    <div className="flex justify-end gap-1">
                      <button
                        className="btn btn-ghost btn-xs"
                        onClick={() => {
                          setActionError(null)
                          setEditing(cat)
                        }}
                        aria-label={`Edit ${cat.name}`}
                      >
                        <Pencil className="h-3.5 w-3.5" />
                      </button>
                      <button
                        className="btn btn-ghost btn-xs text-error"
                        onClick={() => {
                          setActionError(null)
                          setDeleteTarget(cat)
                        }}
                        aria-label={`Delete ${cat.name}`}
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
                  <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                    <FolderTree className="h-8 w-8" />
                    <p className="text-sm">No categories yet. Create your first one.</p>
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {editing && (
        <CategoryFormModal
          target={editing}
          categories={categories ?? []}
          onClose={() => setEditing(null)}
          onSaved={() => {
            queryClient.invalidateQueries({ queryKey: ['categories'] })
            setEditing(null)
          }}
        />
      )}

      {deleteTarget && (
        <dialog className="modal modal-open">
          <div className="modal-box">
            <h3 className="text-lg font-bold">Delete “{deleteTarget.name}”?</h3>
            <p className="py-2 text-sm text-base-content/60">
              This can’t be undone. Categories with subcategories or products can’t be deleted
              until those are reassigned.
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

function CategoryFormModal({
  target,
  categories,
  onClose,
  onSaved,
}: {
  target: Category | 'new'
  categories: Category[]
  onClose: () => void
  onSaved: () => void
}) {
  const isNew = target === 'new'
  const current = isNew ? null : target
  const [serverError, setServerError] = useState<string | null>(null)

  // When editing, a category may not become a child of itself or its descendants.
  const blocked = current ? descendantIds(current.id, categories) : new Set<string>()
  const parentOptions = categories.filter((c) => !blocked.has(c.id))

  const {
    register,
    handleSubmit,
    setValue,
    getValues,
    formState: { errors },
  } = useForm<CategoryFormData>({
    resolver: yupResolver(categorySchema),
    defaultValues: {
      name: current?.name ?? '',
      slug: current?.slug ?? '',
      parentId: current?.parentId ?? '',
      sortOrder: current?.sortOrder ?? 0,
      description: current?.description ?? '',
    },
  })

  const save = useMutation({
    mutationFn: (data: CategoryFormData) => {
      const payload = {
        name: data.name,
        slug: data.slug,
        parentId: data.parentId || undefined,
        sortOrder: data.sortOrder,
        description: data.description || undefined,
      }
      return isNew ? categoryApi.create(payload) : categoryApi.update(current!.id, payload)
    },
    onSuccess: onSaved,
    onError: (err) => setServerError(errorMessage(err, 'Could not save the category.')),
  })

  // Captured so the name field can chain RHF's onChange with slug auto-fill.
  const nameField = register('name')

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="mb-3 text-lg font-bold">{isNew ? 'New category' : `Edit ${current?.name}`}</h3>

        {serverError && (
          <div role="alert" className="alert alert-error mb-3 text-sm">
            <span>{serverError}</span>
          </div>
        )}

        <form
          onSubmit={handleSubmit((data) => {
            setServerError(null)
            save.mutate(data)
          })}
          noValidate
          className="space-y-3"
        >
          <div className="form-control">
            <label className="label" htmlFor="name">
              <span className="label-text font-medium">Name</span>
            </label>
            <input
              id="name"
              {...nameField}
              onChange={(e) => {
                nameField.onChange(e) // keep react-hook-form's tracking intact
                // Auto-fill slug from name only while creating and slug is untouched.
                if (isNew && !getValues('slug')) setValue('slug', slugify(e.target.value))
              }}
              className={`input input-bordered w-full ${errors.name ? 'input-error' : ''}`}
              placeholder="Electronics"
            />
            {errors.name && <span className="mt-1 text-xs text-error">{errors.name.message}</span>}
          </div>

          <div className="form-control">
            <label className="label" htmlFor="slug">
              <span className="label-text font-medium">Slug</span>
            </label>
            <input
              id="slug"
              {...register('slug')}
              className={`input input-bordered w-full font-mono ${errors.slug ? 'input-error' : ''}`}
              placeholder="electronics"
            />
            {errors.slug && <span className="mt-1 text-xs text-error">{errors.slug.message}</span>}
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="form-control">
              <label className="label" htmlFor="parentId">
                <span className="label-text font-medium">Parent</span>
              </label>
              <select id="parentId" {...register('parentId')} className="select select-bordered w-full">
                <option value="">— None (top level) —</option>
                {parentOptions.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-control">
              <label className="label" htmlFor="sortOrder">
                <span className="label-text font-medium">Sort order</span>
              </label>
              <input
                id="sortOrder"
                type="number"
                {...register('sortOrder')}
                className={`input input-bordered w-full ${errors.sortOrder ? 'input-error' : ''}`}
              />
              {errors.sortOrder && (
                <span className="mt-1 text-xs text-error">{errors.sortOrder.message}</span>
              )}
            </div>
          </div>

          <div className="form-control">
            <label className="label" htmlFor="description">
              <span className="label-text font-medium">Description</span>
              <span className="label-text-alt text-base-content/40">optional</span>
            </label>
            <textarea
              id="description"
              {...register('description')}
              className="textarea textarea-bordered w-full"
              rows={2}
            />
          </div>

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
