'use client'

import { useCallback, useEffect, useRef, useState } from 'react'
import { posApi } from '@/lib/api/pos'
import { allSales, putSale, removeSale, type QueuedSale } from './offlineQueue'
import { classifySyncError, syncMessage } from './sync'

export interface OfflineSales {
  queued: QueuedSale[]
  online: boolean
  syncing: boolean
  pendingCount: number
  failedCount: number
  /** Persist a sale that couldn't reach the server, to sync later. */
  enqueue: (sale: QueuedSale) => Promise<void>
  /** Flush all pending sales to the server (idempotent; no-op while offline/syncing). */
  sync: () => Promise<void>
  /** Re-queue a failed sale and try again. */
  retry: (clientSaleId: string) => Promise<void>
  /** Drop a sale from the queue without syncing (cashier decision). */
  discard: (clientSaleId: string) => Promise<void>
}

/**
 * Owns the offline sale queue: persistence, the online/offline signal, and the sync
 * engine that drains the queue on reconnect. Each sale carries a stable clientSaleId,
 * so the server dedups replays — a sale syncs exactly once.
 */
export function useOfflineSales(): OfflineSales {
  const [queued, setQueued] = useState<QueuedSale[]>([])
  const [online, setOnline] = useState(true)
  const [syncing, setSyncing] = useState(false)
  const syncingRef = useRef(false)

  const refresh = useCallback(async () => {
    setQueued(await allSales())
  }, [])

  const sync = useCallback(async () => {
    if (syncingRef.current) return
    if (typeof navigator !== 'undefined' && !navigator.onLine) return
    syncingRef.current = true
    setSyncing(true)
    try {
      const pending = (await allSales()).filter((s) => s.status === 'pending')
      for (const sale of pending) {
        try {
          await posApi.completeSale({
            clientSaleId: sale.clientSaleId,
            items: sale.items,
            amountTendered: sale.amountTendered,
            cardAmount: sale.cardAmount,
          })
          await removeSale(sale.clientSaleId)
        } catch (err) {
          const outcome = classifySyncError(err)
          if (outcome === 'synced') {
            await removeSale(sale.clientSaleId) // server already had it (exactly-once)
          } else if (outcome === 'failed') {
            await putSale({ ...sale, status: 'failed', attempts: sale.attempts + 1, error: syncMessage(err) })
          } else {
            break // transient/offline — stop the run, keep order, try again later
          }
        }
      }
    } finally {
      await refresh()
      syncingRef.current = false
      setSyncing(false)
    }
  }, [refresh])

  const enqueue = useCallback(
    async (sale: QueuedSale) => {
      await putSale(sale)
      await refresh()
    },
    [refresh],
  )

  const retry = useCallback(
    async (clientSaleId: string) => {
      const sale = (await allSales()).find((s) => s.clientSaleId === clientSaleId)
      if (sale) await putSale({ ...sale, status: 'pending' })
      await refresh()
      await sync()
    },
    [refresh, sync],
  )

  const discard = useCallback(
    async (clientSaleId: string) => {
      await removeSale(clientSaleId)
      await refresh()
    },
    [refresh],
  )

  useEffect(() => {
    setOnline(typeof navigator === 'undefined' ? true : navigator.onLine)
    void refresh().then(() => sync()) // drain anything left from a previous session
    const goOnline = () => {
      setOnline(true)
      void sync()
    }
    const goOffline = () => setOnline(false)
    window.addEventListener('online', goOnline)
    window.addEventListener('offline', goOffline)
    return () => {
      window.removeEventListener('online', goOnline)
      window.removeEventListener('offline', goOffline)
    }
  }, [refresh, sync])

  const pendingCount = queued.filter((s) => s.status === 'pending').length
  const failedCount = queued.filter((s) => s.status === 'failed').length

  return { queued, online, syncing, pendingCount, failedCount, enqueue, sync, retry, discard }
}
