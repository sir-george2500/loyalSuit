'use client'

import { useQuery } from '@tanstack/react-query'
import { Wallet, Undo2 } from 'lucide-react'
import { vendorCommissionApi } from '@/lib/api/commissions'
import CommissionLedger from '@/components/commerce/CommissionLedger'

const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'RWF' })

export default function SellerEarningsView() {
  const { data: earnings, isLoading } = useQuery({
    queryKey: ['seller-earnings'],
    queryFn: async () => (await vendorCommissionApi.earnings()).data.data,
  })

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2">
        <SummaryCard
          icon={<Wallet className="h-5 w-5 text-success" />}
          label="Earned balance"
          hint="Net you’re owed, after commission"
          value={isLoading ? null : money.format(earnings?.earnedBalance ?? 0)}
          tone="text-success"
        />
        <SummaryCard
          icon={<Undo2 className="h-5 w-5 text-base-content/50" />}
          label="Reversed by refunds"
          hint="Clawed back when orders were refunded"
          value={isLoading ? null : money.format(earnings?.reversedTotal ?? 0)}
          tone="text-base-content/70"
        />
      </div>

      <div>
        <h2 className="mb-2 text-lg font-semibold">Commission ledger</h2>
        <CommissionLedger queryKey="seller-commissions" fetchPage={vendorCommissionApi.ledger} />
      </div>
    </div>
  )
}

function SummaryCard({
  icon, label, hint, value, tone,
}: {
  icon: React.ReactNode
  label: string
  hint: string
  value: string | null
  tone: string
}) {
  return (
    <div className="card bg-base-100 shadow-sm">
      <div className="card-body p-5">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-base-200">{icon}</div>
          <div>
            <p className="text-sm text-base-content/60">{label}</p>
            {value === null
              ? <span className="loading loading-spinner loading-sm text-base-content/40" />
              : <p className={`text-2xl font-bold ${tone}`}>{value}</p>}
          </div>
        </div>
        <p className="mt-1 text-xs text-base-content/50">{hint}</p>
      </div>
    </div>
  )
}
