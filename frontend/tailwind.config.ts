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
        // Loyal Spare Parts — Deep Blue ("Trust"). Numbered scale for gradients/accents; anchored
        // on the brand primary #0d375b (=600). DaisyUI owns the flat `primary`/`secondary` names.
        brand: {
          50: '#eef3f8',
          100: '#d7e3ef',
          200: '#aec6dd',
          300: '#7aa0c2',
          400: '#3f6d99',
          500: '#145892', // brand secondary blue
          600: '#0d375b', // brand PRIMARY — Deep Blue
          700: '#0a2c49',
          800: '#082238',
          900: '#06192a',
          950: '#030f1a',
        },
        // Loyal Spare Parts — Vibrant Orange ("Innovation"). Named `flame` so it never shadows
        // DaisyUI's flat `accent` utility. Anchored on #ff6600 (=500).
        flame: {
          50: '#fff3eb',
          100: '#ffe0cc',
          200: '#ffc199',
          300: '#ff9751', // brand secondary orange (light)
          400: '#ff7e2e',
          500: '#ff6600', // brand PRIMARY — Vibrant Orange
          600: '#ee6103', // brand secondary orange (dark)
          700: '#c64f02',
          800: '#9c3f04',
          900: '#7e350a',
          950: '#441903',
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
        // Loyal Spare Parts brand theme — Deep Blue (trust) + Vibrant Orange (innovation).
        loyalsuit: {
          primary: '#0d375b', // Deep Blue
          'primary-content': '#ffffff',
          secondary: '#ff6600', // Vibrant Orange
          'secondary-content': '#ffffff',
          accent: '#145892', // secondary blue
          'accent-content': '#ffffff',
          neutral: '#0d375b', // deep-blue chrome (sidebar, dark surfaces)
          'neutral-content': '#ffffff',
          'base-100': '#ffffff',
          'base-200': '#f5f5f5', // brand neutral
          'base-300': '#e0e0e0', // brand neutral
          'base-content': '#0d375b',
          info: '#145892',
          success: '#16a34a',
          warning: '#ee6103',
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
