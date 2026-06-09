'use client'

import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { Loader2, Lock } from 'lucide-react'
import { posShiftApi } from '@/lib/api/pos'

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

/** Start-of-shift screen: the register is locked until a drawer is opened with a float. */
export default function OpenShiftScreen({ onOpened }: { onOpened: () => void }) {
  const [float, setFloat] = useState('')
  const floatNum = Number.parseFloat(float) || 0

  const open = useMutation({
    mutationFn: () => posShiftApi.open(floatNum),
    onSuccess: onOpened,
  })

  return (
    <div className="flex h-full items-center justify-center p-4">
      <div className="w-full max-w-sm rounded-xl bg-gray-800 p-6 text-center">
        <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-primary/20">
          <Lock className="h-6 w-6 text-primary" />
        </div>
        <h2 className="mt-3 text-lg font-semibold">Open your shift</h2>
        <p className="text-sm text-gray-400">Count the cash already in the drawer to start.</p>

        <label className="mt-5 block text-left text-sm">
          <span className="text-gray-400">Opening float</span>
          <input
            type="number"
            inputMode="decimal"
            min={0}
            step="0.01"
            autoFocus
            value={float}
            onChange={(e) => setFloat(e.target.value)}
            placeholder="0.00"
            className="input input-bordered mt-1 w-full bg-gray-900 text-white"
          />
        </label>

        {open.isError && (
          <p className="mt-3 text-sm text-error">{errorMessage(open.error, 'Could not open the shift.')}</p>
        )}

        <button
          onClick={() => open.mutate()}
          disabled={float === '' || floatNum < 0 || open.isPending}
          className="btn btn-primary btn-block mt-6"
        >
          {open.isPending ? <Loader2 className="h-5 w-5 animate-spin" /> : 'Open shift'}
        </button>
      </div>
    </div>
  )
}
