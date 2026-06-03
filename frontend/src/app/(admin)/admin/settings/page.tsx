'use client'

import { useQuery } from '@tanstack/react-query'
import { Loader2, Mail, Shield, Building2 } from 'lucide-react'
import { authApi } from '@/lib/api/auth'

export default function GeneralSettingsPage() {
  const { data: profile, isLoading, isError } = useQuery({
    queryKey: ['me'],
    queryFn: async () => (await authApi.me()).data.data,
  })

  return (
    <div className="card bg-base-100 shadow-sm">
      <div className="card-body">
        <h2 className="card-title text-base">Profile</h2>

        {isLoading && (
          <div className="flex items-center gap-2 py-6 text-base-content/50">
            <Loader2 className="h-4 w-4 animate-spin" /> Loading profile…
          </div>
        )}

        {isError && (
          <div role="alert" className="alert alert-error text-sm">
            <span>Couldn&apos;t load your profile.</span>
          </div>
        )}

        {profile && (
          <div className="space-y-4">
            <div className="flex items-center gap-4">
              <div className="avatar placeholder">
                <div className="h-16 w-16 rounded-full bg-primary text-primary-content">
                  <span className="text-2xl font-semibold">
                    {(profile.fullName ?? profile.email).charAt(0).toUpperCase()}
                  </span>
                </div>
              </div>
              <div>
                <p className="text-lg font-semibold">{profile.fullName ?? '—'}</p>
                <span className="badge badge-primary badge-sm">{profile.role.replace('_', ' ')}</span>
              </div>
            </div>

            <div className="divider my-1" />

            <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="flex items-start gap-3">
                <Mail className="mt-0.5 h-4 w-4 text-base-content/40" />
                <div>
                  <dt className="text-xs text-base-content/50">Email</dt>
                  <dd className="text-sm font-medium">{profile.email}</dd>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <Shield className="mt-0.5 h-4 w-4 text-base-content/40" />
                <div>
                  <dt className="text-xs text-base-content/50">Role</dt>
                  <dd className="text-sm font-medium">{profile.role.replace('_', ' ')}</dd>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <Building2 className="mt-0.5 h-4 w-4 text-base-content/40" />
                <div>
                  <dt className="text-xs text-base-content/50">Tenant ID</dt>
                  <dd className="font-mono text-xs font-medium">{profile.tenantId}</dd>
                </div>
              </div>
            </dl>

            <p className="text-xs text-base-content/40">
              Profile editing (name, avatar, phone) arrives with the account-management module.
            </p>
          </div>
        )}
      </div>
    </div>
  )
}
