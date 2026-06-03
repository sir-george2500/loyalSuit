import type { Config } from 'tailwindcss'
import daisyui from 'daisyui'

const config: Config & { daisyui?: Record<string, unknown> } = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        // Numbered brand scale (used for marketing gradients on the landing page).
        // DaisyUI owns the semantic `primary`/`secondary`/etc. names.
        brand: {
          50: '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          300: '#93c5fd',
          400: '#60a5fa',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
          800: '#1e40af',
          900: '#1e3a8a',
          950: '#172554',
        },
      },
      fontFamily: {
        sans: ['var(--font-sans)', 'system-ui', 'sans-serif'],
        display: ['var(--font-display)', 'var(--font-sans)', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [daisyui],
  daisyui: {
    themes: [
      {
        loyalsuit: {
          primary: '#2563eb',
          'primary-content': '#ffffff',
          secondary: '#7c3aed',
          'secondary-content': '#ffffff',
          accent: '#0891b2',
          'accent-content': '#ffffff',
          neutral: '#1f2937',
          'neutral-content': '#f9fafb',
          'base-100': '#ffffff',
          'base-200': '#f7f8fa',
          'base-300': '#eceef1',
          'base-content': '#1f2937',
          info: '#0ea5e9',
          success: '#16a34a',
          warning: '#d97706',
          error: '#dc2626',
          '--rounded-box': '0.75rem',
          '--rounded-btn': '0.5rem',
        },
      },
      'dark',
    ],
    darkTheme: 'dark',
    logs: false,
  },
}

export default config
