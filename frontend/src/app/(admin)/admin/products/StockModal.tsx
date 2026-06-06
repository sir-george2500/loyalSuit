'use client'

import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import type { AxiosError } from 'axios'
import { Loader2, X, Boxes, AlertTriangle, Minus, Plus } from 'lucide-react'
import { stockApi, warehouseApi } from '@/lib/api/inventory'
import { variantApi } from '@/lib/api/catalog'
import type { Product, Stock } from '@/types'

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

interface SetForm {
  warehouseId: string
  variantId: string
  quantity: number
  lowStockThreshold: number
}

export default function StockModal({ product, onClose }: { product: Product; onClose: () => void }) {
  const queryClient = useQueryClient()
  const [error, setError] = useState<string | null>(null)

  const stockKey = ['stock', product.id]
  const { data: stock, isLoading } = useQuery({
    queryKey: stockKey,
    queryFn: async () => (await stockApi.listForProduct(product.id)).data.data,
  })
  const { data: warehouses } = useQuery({
    queryKey: ['warehouses'],
    queryFn: async () => (await warehouseApi.list()).data.data,
  })
  const { data: variants } = useQuery({
    queryKey: ['variants', product.id],
    queryFn: async () => (await variantApi.list(product.id)).data.data,
  })

  const warehouseName = useMemo(() => {
    const map = new Map((warehouses ?? []).map((w) => [w.id, w.name]))
    return (id: string) => map.get(id) ?? '—'
  }, [warehouses])

  const variantName = useMemo(() => {
    const map = new Map((variants ?? []).map((v) => [v.id, v.name]))
    return (id?: string | null) => (id ? (map.get(id) ?? 'Variant') : 'Whole product')
  }, [variants])

  const invalidate = () => queryClient.invalidateQueries({ queryKey: stockKey })

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<SetForm>({
    defaultValues: { warehouseId: '', variantId: '', quantity: 0, lowStockThreshold: 5 },
  })

  const setLevel = useMutation({
    mutationFn: (form: SetForm) =>
      stockApi.setLevel({
        productId: product.id,
        warehouseId: form.warehouseId,
        variantId: form.variantId || undefined,
        quantity: Number(form.quantity),
        lowStockThreshold: Number(form.lowStockThreshold),
      }),
    onSuccess: () => {
      invalidate()
      setError(null)
      reset({ warehouseId: '', variantId: '', quantity: 0, lowStockThreshold: 5 })
    },
    onError: (err) => setError(errorMessage(err, 'Could not set the stock level.')),
  })

  return (
    <dialog className="modal modal-open">
      <div className="modal-box max-w-3xl">
        <div className="mb-3 flex items-start justify-between">
          <div>
            <h3 className="text-lg font-bold">Stock</h3>
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

        {/* Current stock per warehouse/variant */}
        <div className="mb-4 overflow-x-auto rounded-box border border-base-300">
          <table className="table table-sm">
            <thead>
              <tr>
                <th>Warehouse</th>
                <th>Variant</th>
                <th className="text-right">On hand</th>
                <th className="text-right">Low at</th>
                <th className="text-center">Adjust</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={5} className="text-center">
                    <span className="loading loading-spinner loading-sm" />
                  </td>
                </tr>
              ) : stock && stock.length > 0 ? (
                stock.map((row) => (
                  <StockRow
                    key={row.id}
                    row={row}
                    warehouse={warehouseName(row.warehouseId)}
                    variant={variantName(row.variantId)}
                    onError={setError}
                    onAdjusted={invalidate}
                  />
                ))
              ) : (
                <tr>
                  <td colSpan={5}>
                    <div className="flex flex-col items-center gap-1 py-6 text-center text-base-content/50">
                      <Boxes className="h-6 w-6" />
                      <p className="text-sm">No stock recorded yet.</p>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Set / add a stock level */}
        <div className="rounded-box border border-base-300 p-3">
          <p className="mb-2 text-sm font-medium">Set a stock level</p>
          <form
            onSubmit={handleSubmit((form) => {
              setError(null)
              setLevel.mutate(form)
            })}
            className="grid grid-cols-2 gap-2 sm:grid-cols-5 sm:items-end"
          >
            <label className="form-control col-span-2 sm:col-span-1">
              <span className="label-text text-xs">Warehouse</span>
              <select
                {...register('warehouseId', { required: true })}
                className={`select select-bordered select-sm ${errors.warehouseId ? 'select-error' : ''}`}
              >
                <option value="">Select…</option>
                {(warehouses ?? []).map((w) => (
                  <option key={w.id} value={w.id}>
                    {w.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="form-control col-span-2 sm:col-span-1">
              <span className="label-text text-xs">Variant</span>
              <select {...register('variantId')} className="select select-bordered select-sm">
                <option value="">Whole product</option>
                {(variants ?? []).map((v) => (
                  <option key={v.id} value={v.id}>
                    {v.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="form-control">
              <span className="label-text text-xs">Quantity</span>
              <input
                type="number"
                min={0}
                {...register('quantity', { required: true, min: 0 })}
                className={`input input-bordered input-sm ${errors.quantity ? 'input-error' : ''}`}
              />
            </label>
            <label className="form-control">
              <span className="label-text text-xs">Low at</span>
              <input
                type="number"
                min={0}
                {...register('lowStockThreshold', { min: 0 })}
                className="input input-bordered input-sm"
              />
            </label>
            <button type="submit" className="btn btn-primary btn-sm" disabled={setLevel.isPending}>
              {setLevel.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              Save
            </button>
          </form>
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

function StockRow({
  row,
  warehouse,
  variant,
  onError,
  onAdjusted,
}: {
  row: Stock
  warehouse: string
  variant: string
  onError: (msg: string | null) => void
  onAdjusted: () => void
}) {
  const [amount, setAmount] = useState(1)

  const adjust = useMutation({
    mutationFn: (delta: number) => stockApi.adjust(row.id, delta),
    onSuccess: () => {
      onError(null)
      onAdjusted()
    },
    onError: (err) => onError(errorMessage(err, 'Adjustment failed.')),
  })

  return (
    <tr>
      <td>{warehouse}</td>
      <td className="text-base-content/70">{variant}</td>
      <td className="text-right">
        <span className="inline-flex items-center gap-1">
          {row.lowStock && <AlertTriangle className="h-3.5 w-3.5 text-warning" />}
          <span className={row.lowStock ? 'font-medium text-warning' : ''}>{row.quantity}</span>
        </span>
      </td>
      <td className="text-right text-base-content/50">{row.lowStockThreshold}</td>
      <td>
        <div className="flex items-center justify-center gap-1">
          <button
            className="btn btn-ghost btn-xs"
            disabled={adjust.isPending}
            onClick={() => adjust.mutate(-Math.abs(amount))}
            aria-label="Remove stock"
          >
            <Minus className="h-3.5 w-3.5" />
          </button>
          <input
            type="number"
            min={1}
            value={amount}
            onChange={(e) => setAmount(Math.max(1, Number(e.target.value) || 1))}
            className="input input-bordered input-xs w-14 text-center"
          />
          <button
            className="btn btn-ghost btn-xs"
            disabled={adjust.isPending}
            onClick={() => adjust.mutate(Math.abs(amount))}
            aria-label="Add stock"
          >
            <Plus className="h-3.5 w-3.5" />
          </button>
        </div>
      </td>
    </tr>
  )
}
