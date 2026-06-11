import apiClient from './client'
import type { ApiResponse, Employee, EmployeeStatus, EmploymentType, PageResponse } from '@/types'

export interface EmployeePayload {
  fullName: string
  email?: string
  phone?: string
  jobTitle: string
  department?: string
  employmentType: EmploymentType
  hireDate: string
  baseSalary: number
  status?: EmployeeStatus
}

const ADMIN = '/api/v1/admin/employees'

/** Admin HRM — the employee roster. Plan-gated (Professional / Enterprise) on the backend. */
export const adminEmployeeApi = {
  list: (page = 0, status?: EmployeeStatus, size?: number) =>
    apiClient.get<ApiResponse<PageResponse<Employee>>>(ADMIN, { params: { page, status, size } }),
  create: (payload: EmployeePayload) => apiClient.post<ApiResponse<Employee>>(ADMIN, payload),
  update: (id: string, payload: EmployeePayload) =>
    apiClient.put<ApiResponse<Employee>>(`${ADMIN}/${id}`, payload),
}
