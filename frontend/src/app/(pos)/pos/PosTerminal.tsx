'use client'

import { useEffect, useMemo, useRef, useState } from 'react'
import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { Search, Plus, Minus, Trash2, Loader2, X, Check, ScanLine } from 'lucide-react'
import { posApi, type CompleteSaleInput } from '@/lib/api/pos'
import type { PosProduct, PosSale } from '@/types'

const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })

interface CartLine {
  id: string
  name: string
  price: number
  quantity: number
}

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function PosTerminal() {
  const [query, setQuery] = useState('')
  const [debounced, setDebounced] = useState('')
  const [cart, setCart] = useState<CartLine[]>([])
  const [tendered, setTendered] = useState('')
  const [receipt, setReceipt] = useState<PosSale | null>(null)
  // Held stable across retries so a re-submit after a network blip can't double-charge.
  const saleIdRef = useRef<string | null>(null)

  useEffect(() => {
    const t = setTimeout(() => setDebounced(query.trim()), 250)
    return () => clearTimeout(t)
  }, [query])

  const { data: products, isFetching } = useQuery({
    queryKey: ['pos-products', debounced],
    queryFn: async () => (await posApi.products(debounced)).data.data.content,
    placeholderData: keepPreviousData,
  })

  const total = useMemo(() => cart.reduce((sum, l) => sum + l.price * l.quantity, 0), [cart])
  const tenderedNum = Number.parseFloat(tendered) || 0
  const change = tenderedNum - total
  const canCharge = cart.length > 0 && tenderedNum >= total && total > 0

  const sale = useMutation({
    mutationFn: (payload: CompleteSaleInput) => posApi.completeSale(payload),
    onSuccess: (res) => {
      setReceipt(res.data.data)
      setCart([])
      setTendered('')
      saleIdRef.current = null
    },
  })

  function addToCart(product: PosProduct) {
    setCart((prev) => {
      const existing = prev.find((l) => l.id === product.id)
      if (existing) {
        return prev.map((l) => (l.id === product.id ? { ...l, quantity: l.quantity + 1 } : l))
      }
      return [...prev, { id: product.id, name: product.name, price: product.price, quantity: 1 }]
    })
  }

  function changeQty(id: string, delta: number) {
    setCart((prev) =>
      prev
        .map((l) => (l.id === id ? { ...l, quantity: l.quantity + delta } : l))
        .filter((l) => l.quantity > 0),
    )
  }

  function removeLine(id: string) {
    setCart((prev) => prev.filter((l) => l.id !== id))
  }

  // Barcode scanners type the code then press Enter; one exact match is rung straight in.
  function onSearchEnter() {
    if (products && products.length >= 1) {
      addToCart(products[0])
      setQuery('')
    }
  }

  function charge() {
    if (!canCharge) return
    if (!saleIdRef.current) saleIdRef.current = crypto.randomUUID()
    sale.mutate({
      clientSaleId: saleIdRef.current,
      items: cart.map((l) => ({ productId: l.id, quantity: l.quantity })),
      amountTendered: tenderedNum,
    })
  }

  return (
    <div className="flex h-full">
      {/* Catalog */}
      <div className="flex flex-1 flex-col p-4">
        <label className="flex items-center gap-2 rounded-lg bg-gray-800 px-3 py-2">
          <Search className="h-5 w-5 text-gray-400" />
          <input
            autoFocus
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && onSearchEnter()}
            placeholder="Search or scan a product…"
            className="w-full bg-transparent text-white placeholder:text-gray-500 focus:outline-none"
          />
          {isFetching && <Loader2 className="h-4 w-4 animate-spin text-gray-500" />}
        </label>

        <div className="mt-4 flex-1 overflow-y-auto">
          {products && products.length > 0 ? (
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
              {products.map((p) => (
                <button
                  key={p.id}
                  onClick={() => addToCart(p)}
                  className="flex flex-col rounded-lg border border-gray-700 bg-gray-800 p-3 text-left transition hover:border-primary hover:bg-gray-700"
                >
                  <span className="line-clamp-2 text-sm font-medium">{p.name}</span>
                  <span className="mt-1 text-xs text-gray-400">{p.sku || p.barcode || ''}</span>
                  <span className="mt-auto pt-2 font-semibold text-primary">{money.format(p.price)}</span>
                </button>
              ))}
            </div>
          ) : (
            <div className="flex h-full flex-col items-center justify-center text-gray-500">
              <ScanLine className="h-10 w-10" />
              <p className="mt-2 text-sm">
                {debounced ? 'No matching products' : 'Search or scan to find products'}
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Current sale */}
      <div className="flex w-96 flex-col border-l border-gray-700 bg-gray-800">
        <div className="flex items-center justify-between px-5 py-4">
          <h2 className="text-lg font-semibold">Current Sale</h2>
          {cart.length > 0 && (
            <button onClick={() => setCart([])} className="text-xs text-gray-400 hover:text-error">
              Clear
            </button>
          )}
        </div>

        <div className="flex-1 space-y-2 overflow-y-auto px-5">
          {cart.length === 0 ? (
            <p className="py-8 text-center text-sm text-gray-500">No items yet</p>
          ) : (
            cart.map((l) => (
              <div key={l.id} className="flex items-center gap-2 rounded-lg bg-gray-900 p-2">
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium">{l.name}</p>
                  <p className="text-xs text-gray-400">
                    {money.format(l.price)} × {l.quantity} = {money.format(l.price * l.quantity)}
                  </p>
                </div>
                <div className="flex items-center gap-1">
                  <button onClick={() => changeQty(l.id, -1)} className="btn btn-circle btn-ghost btn-xs">
                    <Minus className="h-3.5 w-3.5" />
                  </button>
                  <span className="w-5 text-center text-sm">{l.quantity}</span>
                  <button onClick={() => changeQty(l.id, 1)} className="btn btn-circle btn-ghost btn-xs">
                    <Plus className="h-3.5 w-3.5" />
                  </button>
                  <button onClick={() => removeLine(l.id)} className="btn btn-circle btn-ghost btn-xs text-error">
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>

        <div className="space-y-3 border-t border-gray-700 p-5">
          <div className="flex justify-between text-lg font-bold">
            <span>Total</span>
            <span>{money.format(total)}</span>
          </div>

          <div>
            <div className="flex items-center gap-2">
              <input
                type="number"
                inputMode="decimal"
                min={0}
                step="0.01"
                value={tendered}
                onChange={(e) => setTendered(e.target.value)}
                placeholder="Cash received"
                className="input input-bordered w-full bg-gray-900 text-white"
              />
              <button
                onClick={() => setTendered(total.toFixed(2))}
                disabled={total <= 0}
                className="btn btn-ghost btn-sm"
              >
                Exact
              </button>
            </div>
            {tenderedNum > 0 && (
              <div className="mt-2 flex justify-between text-sm">
                <span className="text-gray-400">Change due</span>
                <span className={change < 0 ? 'text-error' : 'text-success'}>
                  {money.format(Math.max(change, 0))}
                </span>
              </div>
            )}
          </div>

          {sale.isError && (
            <p className="text-sm text-error">{errorMessage(sale.error, 'Could not complete the sale.')}</p>
          )}

          <button
            onClick={charge}
            disabled={!canCharge || sale.isPending}
            className="btn btn-primary btn-lg btn-block"
          >
            {sale.isPending ? <Loader2 className="h-5 w-5 animate-spin" /> : `Charge ${money.format(total)}`}
          </button>
        </div>
      </div>

      {receipt && <ReceiptOverlay sale={receipt} onClose={() => setReceipt(null)} />}
    </div>
  )
}

function ReceiptOverlay({ sale, onClose }: { sale: PosSale; onClose: () => void }) {
  const fmt = new Intl.NumberFormat('en-US', { style: 'currency', currency: sale.currency || 'USD' })
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <div className="w-full max-w-sm rounded-xl bg-gray-800 p-6 text-center">
        <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-success/20">
          <Check className="h-6 w-6 text-success" />
        </div>
        <h3 className="mt-3 text-lg font-semibold">Sale complete</h3>
        <p className="text-sm text-gray-400">Receipt {sale.orderNumber}</p>

        <dl className="mt-5 space-y-2 text-left text-sm">
          <Row label="Items" value={String(sale.itemCount)} />
          <Row label="Total" value={fmt.format(sale.total)} />
          <Row label="Cash received" value={fmt.format(sale.amountTendered)} />
          <div className="flex justify-between border-t border-gray-700 pt-2 text-base font-semibold">
            <span>Change due</span>
            <span className="text-success">{fmt.format(sale.changeGiven)}</span>
          </div>
        </dl>

        <button onClick={onClose} className="btn btn-primary btn-block mt-6">
          <X className="h-4 w-4" /> New sale
        </button>
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
