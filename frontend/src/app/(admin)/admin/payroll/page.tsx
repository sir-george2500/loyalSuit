import type { Metadata } from 'next'
import PayrollView from './PayrollView'

export const metadata: Metadata = { title: 'Payroll — Admin' }

export default function PayrollPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Payroll</h1>
        <p className="text-sm text-base-content/60">Run payroll over a period and review payslips</p>
      </div>
      <PayrollView />
    </div>
  )
}
