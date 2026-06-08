'use client'

import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Loader2 } from 'lucide-react'
import { posShiftApi } from '@/lib/api/pos'
import OpenShiftScreen from './OpenShiftScreen'
import PosTerminal from './PosTerminal'

/**
 * Gates the register behind an open cash drawer: no shift → the open-shift screen;
 * an open shift → the terminal. Both transitions just refetch the current shift.
 */
export default function PosWorkspace() {
  const queryClient = useQueryClient()
  const { data: shift, isLoading } = useQuery({
    queryKey: ['pos-current-shift'],
    queryFn: async () => (await posShiftApi.current()).data.data,
  })

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['pos-current-shift'] })

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <Loader2 className="h-6 w-6 animate-spin text-gray-500" />
      </div>
    )
  }

  if (!shift) return <OpenShiftScreen onOpened={refresh} />
  return <PosTerminal shift={shift} onShiftClosed={refresh} />
}
