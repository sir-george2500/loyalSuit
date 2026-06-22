'use client'

import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { KeyRound, AlertCircle, Loader2, Plus, Trash2, Copy, Check } from 'lucide-react'
import { apiKeyApi } from '@/lib/api/apiKeys'
import type { CreatedApiKey } from '@/types'

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

function date(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

export default function ApiKeysView() {
  const queryClient = useQueryClient()
  const [creating, setCreating] = useState(false)
  const [created, setCreated] = useState<CreatedApiKey | null>(null)
  const [error, setError] = useState<string | null>(null)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['api-keys'],
    queryFn: async () => (await apiKeyApi.list()).data.data,
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['api-keys'] })

  const revoke = useMutation({
    mutationFn: (id: string) => apiKeyApi.revoke(id),
    onSuccess: () => { invalidate(); setError(null) },
    onError: (err) => setError(errorMessage(err, 'Could not revoke the key.')),
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load API keys. Try again.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <button className="btn btn-primary btn-sm gap-1" onClick={() => { setError(null); setCreating(true) }}>
          <Plus className="h-4 w-4" /> New key
        </button>
      </div>

      {error && <div role="alert" className="alert alert-error text-sm"><span>{error}</span></div>}

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr><th>Name</th><th>Key</th><th>Status</th><th>Created</th><th>Last used</th><th className="text-right">Actions</th></tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 3 }).map((_, i) => (
                <tr key={i}><td colSpan={6}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
              ))
            ) : data && data.length > 0 ? (
              data.map((k) => (
                <tr key={k.id}>
                  <td className="font-medium">{k.name}</td>
                  <td className="font-mono text-xs">{k.keyPrefix}…{k.lastFour}</td>
                  <td>
                    <span className={`badge badge-sm ${k.active ? 'badge-success' : 'badge-ghost'}`}>
                      {k.active ? 'Active' : 'Revoked'}
                    </span>
                  </td>
                  <td className="text-sm text-base-content/60">{date(k.createdAt)}</td>
                  <td className="text-sm text-base-content/60">{k.lastUsedAt ? date(k.lastUsedAt) : 'Never'}</td>
                  <td>
                    <div className="flex justify-end">
                      {k.active && (
                        <button className="btn btn-ghost btn-xs gap-1 text-error" disabled={revoke.isPending}
                          onClick={() => { if (confirm(`Revoke “${k.name}”? This cannot be undone.`)) { setError(null); revoke.mutate(k.id) } }}>
                          <Trash2 className="h-3.5 w-3.5" /> Revoke
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={6}>
                <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                  <KeyRound className="h-8 w-8" /><p className="text-sm">No API keys yet.</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {creating && (
        <CreateKeyModal
          onClose={() => setCreating(false)}
          onCreated={(c) => { invalidate(); setCreating(false); setCreated(c) }}
        />
      )}

      {created && <RevealKeyModal created={created} onClose={() => setCreated(null)} />}
    </div>
  )
}

function CreateKeyModal({ onClose, onCreated }: { onClose: () => void; onCreated: (c: CreatedApiKey) => void }) {
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)

  const create = useMutation({
    mutationFn: () => apiKeyApi.create(name.trim()),
    onSuccess: (res) => onCreated(res.data.data),
    onError: (err) => setError(errorMessage(err, 'Could not create the key.')),
  })

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">New API key</h3>
        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}
        <label className="form-control pt-2">
          <span className="label-text">Name</span>
          <input className="input input-bordered" value={name} placeholder="e.g. Mobile app"
            onChange={(e) => setName(e.target.value)} />
        </label>
        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={create.isPending || !name.trim()} onClick={() => { setError(null); create.mutate() }}>
            {create.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
            Create
          </button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}

function RevealKeyModal({ created, onClose }: { created: CreatedApiKey; onClose: () => void }) {
  const [copied, setCopied] = useState(false)

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(created.plaintext)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      // Clipboard may be unavailable (insecure context); the key is shown for manual copy.
    }
  }

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">{created.key.name}</h3>
        <div role="alert" className="alert alert-warning mt-2 text-sm">
          <AlertCircle className="h-4 w-4" />
          <span>Copy this key now — you won’t be able to see it again.</span>
        </div>
        <div className="mt-3 flex items-center gap-2">
          <input readOnly className="input input-bordered input-sm grow font-mono text-xs" value={created.plaintext} />
          <button className="btn btn-sm gap-1" onClick={copy}>
            {copied ? <Check className="h-4 w-4 text-success" /> : <Copy className="h-4 w-4" />}
            {copied ? 'Copied' : 'Copy'}
          </button>
        </div>
        <div className="modal-action">
          <button className="btn btn-primary" onClick={onClose}>Done</button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}
