'use client'

import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import type { AxiosError } from 'axios'
import { Plus, Pencil, Trash2, Warehouse as WarehouseIcon, AlertCircle, Loader2, Star } from 'lucide-react'
import { warehouseApi } from '@/lib/api/inventory'
import { warehouseSchema, type WarehouseFormData } from '@/lib/validations/inventory'
import type { Warehouse } from '@/types'

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function WarehousesView() {
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState<Warehouse | 'new' | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<Warehouse | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const { data: warehouses, isLoading, isError } = useQuery({
    queryKey: ['warehouses'],
    queryFn: async () => (await warehouseApi.list()).data.data,
  })

  const removeMutation = useMutation({
    mutationFn: (id: string) => warehouseApi.remove(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['warehouses'] })
      setDeleteTarget(null)
      setActionError(null)
    },
    onError: (err) => setActionError(errorMessage(err, 'Could not delete the warehouse.')),
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load warehouses. Try again.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <button className="btn btn-primary btn-sm gap-2" onClick={() => setEditing('new')}>
          <Plus className="h-4 w-4" /> New warehouse
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
              <th>Address</th>
              <th className="text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 3 }).map((_, i) => (
                <tr key={i}>
                  <td colSpan={3}>
                    <div className="h-4 w-full animate-pulse rounded bg-base-200" />
                  </td>
                </tr>
              ))
            ) : warehouses && warehouses.length > 0 ? (
              warehouses.map((w) => (
                <tr key={w.id}>
                  <td>
                    <span className="flex items-center gap-2 font-medium">
                      {w.name}
                      {w.isDefault && (
                        <span className="badge badge-primary badge-sm gap-1">
                          <Star className="h-2.5 w-2.5" /> Default
                        </span>
                      )}
                    </span>
                  </td>
                  <td className="text-base-content/60">{w.address ?? '—'}</td>
                  <td>
                    <div className="flex justify-end gap-1">
                      <button
                        className="btn btn-ghost btn-xs"
                        onClick={() => {
                          setActionError(null)
                          setEditing(w)
                        }}
                        aria-label={`Edit ${w.name}`}
                      >
                        <Pencil className="h-3.5 w-3.5" />
                      </button>
                      <button
                        className="btn btn-ghost btn-xs text-error"
                        onClick={() => {
                          setActionError(null)
                          setDeleteTarget(w)
                        }}
                        aria-label={`Delete ${w.name}`}
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={3}>
                  <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                    <WarehouseIcon className="h-8 w-8" />
                    <p className="text-sm">No warehouses yet.</p>
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {editing && (
        <WarehouseFormModal
          target={editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            queryClient.invalidateQueries({ queryKey: ['warehouses'] })
            setEditing(null)
          }}
        />
      )}

      {deleteTarget && (
        <dialog className="modal modal-open">
          <div className="modal-box">
            <h3 className="text-lg font-bold">Delete “{deleteTarget.name}”?</h3>
            <p className="py-2 text-sm text-base-content/60">
              The default warehouse and a store’s last warehouse can’t be deleted.
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

function WarehouseFormModal({
  target,
  onClose,
  onSaved,
}: {
  target: Warehouse | 'new'
  onClose: () => void
  onSaved: () => void
}) {
  const isNew = target === 'new'
  const current = isNew ? null : target
  const [serverError, setServerError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<WarehouseFormData>({
    resolver: yupResolver(warehouseSchema),
    defaultValues: {
      name: current?.name ?? '',
      address: current?.address ?? '',
      isDefault: current?.isDefault ?? false,
    },
  })

  const save = useMutation({
    mutationFn: (form: WarehouseFormData) => {
      const payload = { name: form.name, address: form.address || undefined, isDefault: form.isDefault }
      return isNew ? warehouseApi.create(payload) : warehouseApi.update(current!.id, payload)
    },
    onSuccess: onSaved,
    onError: (err) => setServerError(errorMessage(err, 'Could not save the warehouse.')),
  })

  // The current default can't un-default itself by unchecking — promoting another
  // warehouse is how you move the default. Lock the toggle when already default.
  const lockDefault = !isNew && current!.isDefault

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="mb-3 text-lg font-bold">{isNew ? 'New warehouse' : `Edit ${current?.name}`}</h3>

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
          <div className="form-control">
            <label className="label" htmlFor="name">
              <span className="label-text font-medium">Name</span>
            </label>
            <input
              id="name"
              {...register('name')}
              className={`input input-bordered w-full ${errors.name ? 'input-error' : ''}`}
              placeholder="Main Warehouse"
            />
            {errors.name && <span className="mt-1 text-xs text-error">{errors.name.message}</span>}
          </div>

          <div className="form-control">
            <label className="label" htmlFor="address">
              <span className="label-text font-medium">Address</span>
              <span className="label-text-alt text-base-content/40">optional</span>
            </label>
            <textarea
              id="address"
              {...register('address')}
              className="textarea textarea-bordered w-full"
              rows={2}
              placeholder="123 Market St, Springfield"
            />
          </div>

          <label className="label cursor-pointer justify-start gap-3">
            <input
              type="checkbox"
              {...register('isDefault')}
              disabled={lockDefault}
              className="checkbox checkbox-sm"
            />
            <span className="label-text">
              {lockDefault ? 'This is the default warehouse' : 'Make this the default warehouse'}
            </span>
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
