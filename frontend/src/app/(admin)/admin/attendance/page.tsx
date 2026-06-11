import type { Metadata } from 'next'
import AttendanceView from './AttendanceView'

export const metadata: Metadata = { title: 'Attendance — Admin' }

export default function AttendancePage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Attendance</h1>
        <p className="text-sm text-base-content/60">Clock employees in and out, and review timesheets by period</p>
      </div>
      <AttendanceView />
    </div>
  )
}
