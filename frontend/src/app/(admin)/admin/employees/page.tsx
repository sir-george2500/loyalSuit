import type { Metadata } from 'next'
import EmployeesView from './EmployeesView'

export const metadata: Metadata = { title: 'Employees — Admin' }

export default function EmployeesPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Employees</h1>
        <p className="text-sm text-base-content/60">Your team roster — roles, departments, and employment details</p>
      </div>
      <EmployeesView />
    </div>
  )
}
