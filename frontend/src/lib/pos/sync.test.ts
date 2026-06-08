import { describe, expect, it } from 'vitest'
import { classifySyncError, syncMessage } from './sync'

/** Builds an axios-shaped error with the given HTTP status (or none for a network error). */
function axiosError(status?: number, message?: string) {
  if (status === undefined) return new Error('Network Error')
  return { response: { status, data: message ? { message } : {} } }
}

describe('classifySyncError', () => {
  it('treats a network error (no response) as retry — still offline', () => {
    // Arrange / Act / Assert
    expect(classifySyncError(axiosError(undefined))).toBe('retry')
  })

  it('treats a 409 conflict as already synced — exactly-once', () => {
    // A duplicate clientSaleId means the server already recorded the sale.
    expect(classifySyncError(axiosError(409))).toBe('synced')
  })

  it('treats a 5xx as retry — transient server problem', () => {
    expect(classifySyncError(axiosError(503))).toBe('retry')
  })

  it('treats other 4xx as failed — needs a human (e.g. insufficient stock)', () => {
    expect(classifySyncError(axiosError(400))).toBe('failed')
    expect(classifySyncError(axiosError(404))).toBe('failed')
  })
})

describe('syncMessage', () => {
  it('surfaces the server message when present', () => {
    expect(syncMessage(axiosError(400, 'Not enough stock'))).toBe('Not enough stock')
  })

  it('falls back when there is no server message', () => {
    expect(syncMessage(axiosError(undefined), 'offline')).toBe('offline')
  })
})
