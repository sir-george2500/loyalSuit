'use client'

import { adminCommissionApi } from '@/lib/api/commissions'
import CommissionLedger from '@/components/commerce/CommissionLedger'

export default function CommissionsView() {
  return (
    <CommissionLedger queryKey="admin-commissions" fetchPage={adminCommissionApi.list} showVendor />
  )
}
