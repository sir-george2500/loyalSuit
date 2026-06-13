'use client'

import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { Loader2, X } from 'lucide-react'
import { posShiftApi } from '@/lib/api/pos'
import type { PosShift } from '@/types'

const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'RWF' })

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

/**
 * Close a drawer: count the cash, submit, then show the reconciliation the server
 * computed. Only cash reaches the drawer, so it reconciles against the cash portion of
 * sales (expected = float + cash sales; variance = counted − expected).
 */
export default function CloseShiftDialog({
  shift,
  onClose,
  onClosed,
}: {
  shift: PosShift
  onClose: () => void
  onClosed: () => void
}) {
  const [counted, setCounted] = useState('')
  const [result, setResult] = useState<PosShift | null>(null)
  const countedNum = Number.parseFloat(counted) || 0

  const close = useMutation({
    mutationFn: () => posShiftApi.close(shift.id, countedNum),
    onSuccess: (res) => setResult(res.data.data),
  })

  const variance = result?.variance ?? 0
  const varianceTone = variance === 0 ? 'text-success' : variance > 0 ? 'text-warning' : 'text-error'
  const varianceLabel = variance === 0 ? 'Balanced' : variance > 0 ? 'Over' : 'Short'

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <div className="w-full max-w-sm rounded-xl bg-gray-800 p-6">
        {result ? (
          <div className="text-center">
            <h3 className="text-lg font-semibold">Shift closed</h3>
            <p className="text-sm text-gray-400">{result.saleCount} sales rung up</p>

            <dl className="mt-5 space-y-2 text-left text-sm">
              <Row label="Opening float" value={money.format(result.openingFloat)} />
              <Row label="Sales (total)" value={money.format(result.salesTotal ?? 0)} />
              <Row label="Cash sales" value={money.format(result.cashSales ?? 0)} />
              <Row label="Expected in drawer" value={money.format(result.expectedCash ?? 0)} />
              <Row label="Counted" value={money.format(result.countedCash ?? 0)} />
              <div className="flex justify-between border-t border-gray-700 pt-2 text-base font-semibold">
                <span>{varianceLabel}</span>
                <span className={varianceTone}>{money.format(Math.abs(variance))}</span>
              </div>
            </dl>

            <button onClick={onClosed} className="btn btn-primary btn-block mt-6">
              Done
            </button>
          </div>
        ) : (
          <>
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-semibold">Close shift</h3>
              <button onClick={onClose} className="btn btn-circle btn-ghost btn-sm">
                <X className="h-4 w-4" />
              </button>
            </div>
            <p className="mt-1 text-sm text-gray-400">Count all the cash in the drawer.</p>

            <label className="mt-4 block text-left text-sm">
              <span className="text-gray-400">Counted cash</span>
              <input
                type="number"
                inputMode="decimal"
                min={0}
                step="0.01"
                autoFocus
                value={counted}
                onChange={(e) => setCounted(e.target.value)}
                placeholder="0.00"
                className="input input-bordered mt-1 w-full bg-gray-900 text-white"
              />
            </label>

            {close.isError && (
              <p className="mt-3 text-sm text-error">{errorMessage(close.error, 'Could not close the shift.')}</p>
            )}

            <button
              onClick={() => close.mutate()}
              disabled={counted === '' || countedNum < 0 || close.isPending}
              className="btn btn-primary btn-block mt-6"
            >
              {close.isPending ? <Loader2 className="h-5 w-5 animate-spin" /> : 'Close & reconcile'}
            </button>
          </>
        )}
      </div>
    </div>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between">
      <dt className="text-gray-400">{label}</dt>
      <dd>{value}</dd>
    </div>
  )
}
