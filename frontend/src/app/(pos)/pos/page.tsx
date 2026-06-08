import type { Metadata } from 'next'
import PosWorkspace from './PosWorkspace'

export const metadata: Metadata = { title: 'POS Terminal' }

export default function PosPage() {
  return <PosWorkspace />
}
