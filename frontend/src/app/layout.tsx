import type { Metadata } from 'next'
import { Roboto, Montserrat } from 'next/font/google'
import { BRAND } from '@/lib/brand'
import { QueryProvider } from '@/providers/QueryProvider'
import './globals.css'

// Brand typography: Roboto for body, Montserrat for headings (per the brand guideline).
const roboto = Roboto({
  subsets: ['latin'],
  weight: ['400', '500', '700'],
  variable: '--font-sans',
  display: 'swap',
})

const montserrat = Montserrat({
  subsets: ['latin'],
  weight: ['400', '500', '600', '700', '800'],
  variable: '--font-display',
  display: 'swap',
})

export const metadata: Metadata = {
  title: {
    template: `%s | ${BRAND.name}`,
    default: `${BRAND.name} — ${BRAND.motto}`,
  },
  description: BRAND.description,
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" data-theme="loyalsuit" className={`${roboto.variable} ${montserrat.variable}`}>
      <body>
        <QueryProvider>{children}</QueryProvider>
      </body>
    </html>
  )
}
