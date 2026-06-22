import type { Metadata } from 'next'
import ApiKeysView from './ApiKeysView'

export const metadata: Metadata = { title: 'API Keys — Admin' }

export default function ApiKeysPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">API Keys</h1>
        <p className="text-sm text-base-content/60">Issue keys for integrations to access your store’s API</p>
      </div>
      <ApiKeysView />
    </div>
  )
}
