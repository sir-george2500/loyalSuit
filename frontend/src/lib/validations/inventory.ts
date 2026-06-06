import * as yup from 'yup'

export const warehouseSchema = yup.object({
  name: yup.string().trim().required('Name is required').max(255),
  address: yup.string().trim().max(1000).optional(),
  isDefault: yup.boolean().default(false),
})

export type WarehouseFormData = yup.InferType<typeof warehouseSchema>
