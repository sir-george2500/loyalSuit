import type { Metadata } from 'next'
import PosTerminal from './PosTerminal'

export const metadata: Metadata = { title: 'POS Terminal' }

export default function PosPage() {
  return <PosTerminal />
}
