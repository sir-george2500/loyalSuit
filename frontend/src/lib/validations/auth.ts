import * as yup from 'yup'

export const loginSchema = yup.object({
  email: yup
    .string()
    .required('Email is required')
    .email('Enter a valid email address'),
  password: yup
    .string()
    .required('Password is required')
    .min(8, 'Password must be at least 8 characters'),
})

export const registerSchema = yup.object({
  fullName: yup
    .string()
    .required('Full name is required')
    .min(2, 'Name must be at least 2 characters')
    .max(80, 'Name is too long'),
  email: yup
    .string()
    .required('Email is required')
    .email('Enter a valid email address'),
  password: yup
    .string()
    .required('Password is required')
    .min(8, 'Must be at least 8 characters')
    .matches(/[A-Z]/, 'Must include at least one uppercase letter')
    .matches(/[0-9]/, 'Must include at least one number')
    .matches(/[@$!%*?&#]/, 'Must include at least one special character (@$!%*?&#)'),
  confirmPassword: yup
    .string()
    .required('Please confirm your password')
    .oneOf([yup.ref('password')], 'Passwords do not match'),
})

export const changePasswordSchema = yup.object({
  currentPassword: yup.string().required('Current password is required'),
  newPassword: yup
    .string()
    .required('New password is required')
    .min(8, 'Must be at least 8 characters')
    .matches(/[A-Z]/, 'Must include at least one uppercase letter')
    .matches(/[0-9]/, 'Must include at least one number')
    .matches(/[@$!%*?&#]/, 'Must include at least one special character (@$!%*?&#)')
    .notOneOf([yup.ref('currentPassword')], 'New password must be different'),
  confirmPassword: yup
    .string()
    .required('Please confirm your new password')
    .oneOf([yup.ref('newPassword')], 'Passwords do not match'),
})

export const forgotPasswordSchema = yup.object({
  email: yup
    .string()
    .required('Email is required')
    .email('Enter a valid email address'),
})

export const resetPasswordSchema = yup.object({
  newPassword: yup
    .string()
    .required('New password is required')
    .min(8, 'Must be at least 8 characters')
    .matches(/[A-Z]/, 'Must include at least one uppercase letter')
    .matches(/[0-9]/, 'Must include at least one number')
    .matches(/[@$!%*?&#]/, 'Must include at least one special character (@$!%*?&#)'),
  confirmPassword: yup
    .string()
    .required('Please confirm your new password')
    .oneOf([yup.ref('newPassword')], 'Passwords do not match'),
})

export type LoginFormData = yup.InferType<typeof loginSchema>
export type RegisterFormData = yup.InferType<typeof registerSchema>
export type ChangePasswordFormData = yup.InferType<typeof changePasswordSchema>
export type ForgotPasswordFormData = yup.InferType<typeof forgotPasswordSchema>
export type ResetPasswordFormData = yup.InferType<typeof resetPasswordSchema>
