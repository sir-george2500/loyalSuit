'use client'

import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { Flag, AlertCircle, Loader2, Plus, Pencil, Trash2 } from 'lucide-react'
import { featureFlagApi, type UpsertFeatureFlagPayload } from '@/lib/api/featureFlags'
import type { FeatureFlag } from '@/types'

function errorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<{ message?: string }>)?.response?.data?.message ?? fallback
}

export default function FlagsView() {
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState<FeatureFlag | null>(null)
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['feature-flags'],
    queryFn: async () => (await featureFlagApi.list()).data.data,
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['feature-flags'] })

  const toggle = useMutation({
    mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) =>
      enabled ? featureFlagApi.disable(id) : featureFlagApi.enable(id),
    onSuccess: () => { invalidate(); setError(null) },
    onError: (err) => setError(errorMessage(err, 'Could not update the flag.')),
  })

  const remove = useMutation({
    mutationFn: (id: string) => featureFlagApi.remove(id),
    onSuccess: () => { invalidate(); setError(null) },
    onError: (err) => setError(errorMessage(err, 'Could not delete the flag.')),
  })

  if (isError) {
    return (
      <div className="card border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-base-content/60">Couldn’t load feature flags. Try again.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <button className="btn btn-primary btn-sm gap-1" onClick={() => { setError(null); setCreating(true) }}>
          <Plus className="h-4 w-4" /> New flag
        </button>
      </div>

      {error && <div role="alert" className="alert alert-error text-sm"><span>{error}</span></div>}

      <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
        <table className="table table-sm">
          <thead>
            <tr><th>Flag</th><th>Description</th><th className="text-center">Enabled</th><th className="text-right">Actions</th></tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 4 }).map((_, i) => (
                <tr key={i}><td colSpan={4}><div className="h-4 w-full animate-pulse rounded bg-base-200" /></td></tr>
              ))
            ) : data && data.length > 0 ? (
              data.map((f) => (
                <tr key={f.id}>
                  <td className="font-mono text-xs font-medium">{f.flagKey}</td>
                  <td className="text-sm text-base-content/70">{f.description ?? '—'}</td>
                  <td className="text-center">
                    <input type="checkbox" className="toggle toggle-primary toggle-sm" checked={f.enabled} disabled={toggle.isPending}
                      onChange={() => { setError(null); toggle.mutate({ id: f.id, enabled: f.enabled }) }} />
                  </td>
                  <td>
                    <div className="flex justify-end gap-1">
                      <button className="btn btn-ghost btn-xs gap-1" onClick={() => { setError(null); setEditing(f) }}>
                        <Pencil className="h-3.5 w-3.5" /> Edit
                      </button>
                      <button className="btn btn-ghost btn-xs gap-1 text-error" disabled={remove.isPending}
                        onClick={() => { if (confirm(`Delete flag “${f.flagKey}”?`)) { setError(null); remove.mutate(f.id) } }}>
                        <Trash2 className="h-3.5 w-3.5" /> Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={4}>
                <div className="flex flex-col items-center gap-2 py-10 text-center text-base-content/50">
                  <Flag className="h-8 w-8" /><p className="text-sm">No feature flags yet.</p>
                </div>
              </td></tr>
            )}
          </tbody>
        </table>
      </div>

      {(creating || editing) && (
        <FlagModal
          flag={editing}
          onClose={() => { setCreating(false); setEditing(null) }}
          onSaved={() => { invalidate(); setCreating(false); setEditing(null) }}
        />
      )}
    </div>
  )
}

function FlagModal({ flag, onClose, onSaved }: { flag: FeatureFlag | null; onClose: () => void; onSaved: () => void }) {
  const [form, setForm] = useState<UpsertFeatureFlagPayload>({
    flagKey: flag?.flagKey ?? '',
    description: flag?.description ?? '',
    enabled: flag?.enabled ?? false,
  })
  const [error, setError] = useState<string | null>(null)

  const save = useMutation({
    mutationFn: () => (flag ? featureFlagApi.update(flag.id, form) : featureFlagApi.create(form)),
    onSuccess: onSaved,
    onError: (err) => setError(errorMessage(err, 'Could not save the flag.')),
  })

  return (
    <dialog className="modal modal-open">
      <div className="modal-box">
        <h3 className="text-lg font-bold">{flag ? 'Edit flag' : 'New flag'}</h3>
        {error && <div role="alert" className="alert alert-error my-2 text-sm"><span>{error}</span></div>}

        <label className="form-control pt-2">
          <span className="label-text">Key</span>
          <input className="input input-bordered input-sm font-mono" value={form.flagKey} disabled={!!flag}
            placeholder="checkout.new-flow"
            onChange={(e) => setForm({ ...form, flagKey: e.target.value })} />
          <span className="label-text-alt mt-1 text-base-content/50">Lowercase letters, digits, dots, dashes, underscores.</span>
        </label>
        <label className="form-control pt-2">
          <span className="label-text">Description</span>
          <input className="input input-bordered input-sm" value={form.description ?? ''}
            onChange={(e) => setForm({ ...form, description: e.target.value })} />
        </label>
        <label className="mt-3 flex cursor-pointer items-center gap-3">
          <input type="checkbox" className="toggle toggle-primary" checked={form.enabled}
            onChange={(e) => setForm({ ...form, enabled: e.target.checked })} />
          <span className="text-sm">Enabled</span>
        </label>

        <div className="modal-action">
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={save.isPending} onClick={() => { setError(null); save.mutate() }}>
            {save.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
            Save
          </button>
        </div>
      </div>
      <div className="modal-backdrop" onClick={onClose} />
    </dialog>
  )
}
