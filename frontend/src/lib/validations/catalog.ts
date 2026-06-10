import * as yup from 'yup'

export const categorySchema = yup.object({
  name: yup.string().trim().required('Name is required').max(255),
  slug: yup
    .string()
    .trim()
    .required('Slug is required')
    .matches(/^[a-z0-9]+(?:-[a-z0-9]+)*$/, 'Use lowercase letters, numbers, and hyphens'),
  parentId: yup.string().optional(),
  sortOrder: yup
    .number()
    .typeError('Must be a number')
    .integer('Must be a whole number')
    .min(0, 'Cannot be negative')
    .default(0),
  description: yup.string().trim().max(2000).optional(),
})

export const productSchema = yup.object({
  name: yup.string().trim().required('Name is required').max(255),
  slug: yup
    .string()
    .trim()
    .required('Slug is required')
    .matches(/^[a-z0-9]+(?:-[a-z0-9]+)*$/, 'Use lowercase letters, numbers, and hyphens'),
  price: yup
    .number()
    .typeError('Price is required')
    .positive('Price must be greater than 0')
    .required('Price is required'),
  compareAtPrice: yup
    .number()
    .typeError('Must be a number')
    .min(0, 'Cannot be negative')
    .nullable()
    .transform((v, o) => (o === '' ? null : v))
    .optional(),
  sku: yup.string().trim().max(100).optional(),
  barcode: yup.string().trim().max(100).optional(),
  categoryId: yup.string().optional(),
  digital: yup.boolean().default(false),
  description: yup.string().trim().max(2000).optional(),
  // Flash deal (owner-controlled promotion). Sale price must be below the list price.
  salePrice: yup
    .number()
    .typeError('Must be a number')
    .positive('Must be greater than 0')
    .nullable()
    .transform((v, o) => (o === '' ? null : v))
    .optional(),
  saleStartsAt: yup.string().optional(),
  saleEndsAt: yup.string().optional(),
})

export type ProductFormData = yup.InferType<typeof productSchema>

export const variantSchema = yup.object({
  name: yup.string().trim().required('Name is required').max(255),
  sku: yup.string().trim().max(100).optional(),
  price: yup
    .number()
    .typeError('Price is required')
    .positive('Price must be greater than 0')
    .required('Price is required'),
})

export type VariantFormData = yup.InferType<typeof variantSchema>

export type CategoryFormData = yup.InferType<typeof categorySchema>

/** Turn a free-text name into a URL-safe slug (lowercase, hyphenated). */
export function slugify(value: string): string {
  return value
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '')
}
