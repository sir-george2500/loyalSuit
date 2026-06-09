import type { SaleLineInput } from '@/lib/api/pos'

export type QueuedStatus = 'pending' | 'failed'

/**
 * A sale rung up while offline (or whose network call never completed), persisted in
 * IndexedDB so it survives a reload and reconnect. {@link clientSaleId} is the key and
 * the idempotency token — re-sending it can never ring the sale up twice on the server.
 * The snapshot fields (total, itemCount, currency) let the queue render without re-pricing.
 */
export interface QueuedSale {
  clientSaleId: string
  items: SaleLineInput[]
  amountTendered: number
  /** Amount charged on the card terminal; 0 for a cash-only sale. */
  cardAmount: number
  total: number
  itemCount: number
  currency: string
  createdAt: string
  status: QueuedStatus
  /** Last sync error, when status is 'failed'. */
  error?: string
  attempts: number
}

const DB_NAME = 'loyalsuit-pos'
const STORE = 'pending-sales'
const VERSION = 1

function available(): boolean {
  return typeof indexedDB !== 'undefined'
}

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, VERSION)
    req.onupgradeneeded = () => {
      const db = req.result
      if (!db.objectStoreNames.contains(STORE)) {
        db.createObjectStore(STORE, { keyPath: 'clientSaleId' })
      }
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

function run<T>(mode: IDBTransactionMode, op: (store: IDBObjectStore) => IDBRequest): Promise<T> {
  return openDb().then(
    (db) =>
      new Promise<T>((resolve, reject) => {
        const transaction = db.transaction(STORE, mode)
        const request = op(transaction.objectStore(STORE))
        request.onsuccess = () => resolve(request.result as T)
        request.onerror = () => reject(request.error)
        transaction.oncomplete = () => db.close()
      }),
  )
}

/** Insert or replace a queued sale (keyed by clientSaleId). */
export async function putSale(sale: QueuedSale): Promise<void> {
  if (!available()) return
  await run('readwrite', (s) => s.put(sale))
}

export async function removeSale(clientSaleId: string): Promise<void> {
  if (!available()) return
  await run('readwrite', (s) => s.delete(clientSaleId))
}

/** All queued sales, oldest first (sync and display order). */
export async function allSales(): Promise<QueuedSale[]> {
  if (!available()) return []
  const sales = await run<QueuedSale[]>('readonly', (s) => s.getAll())
  return sales.sort((a, b) => a.createdAt.localeCompare(b.createdAt))
}
