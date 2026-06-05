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

export type CategoryFormData = yup.InferType<typeof categorySchema>

/** Turn a free-text name into a URL-safe slug (lowercase, hyphenated). */
export function slugify(value: string): string {
  return value
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '')
}
