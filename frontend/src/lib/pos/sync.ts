import type { AxiosError } from 'axios'

/**
 * What to do with a queued sale after a sync attempt fails.
 * - `synced`  — the server already has this sale (a 409 on the unique clientSaleId),
 *               so it's done; drop it from the queue. This is what guarantees a sale
 *               syncs *exactly once* even if it's replayed.
 * - `retry`   — transient: no response (still offline) or a 5xx. Keep it; try later.
 * - `failed`  — a 4xx the server won't accept on replay (e.g. insufficient stock).
 *               Keep it but flag it; a human must resolve it (never silently dropped).
 */
export type SyncOutcome = 'synced' | 'retry' | 'failed'

export function classifySyncError(error: unknown): SyncOutcome {
  const status = (error as AxiosError)?.response?.status
  if (status === undefined) return 'retry' // network error — no response at all
  if (status === 409) return 'synced' // duplicate clientSaleId = already recorded
  if (status >= 500) return 'retry' // transient server problem
  return 'failed' // other 4xx — needs attention
}

export function syncMessage(error: unknown, fallback = 'Could not sync this sale'): string {
  return (error as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}
