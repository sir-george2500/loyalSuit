'use client'

import { useEffect } from 'react'
import { captureReferralFromUrl } from '@/lib/referral'

/** Mounts on the storefront to remember a ?ref=CODE referral for later checkout. Renders nothing. */
export default function ReferralCapture() {
  useEffect(() => { captureReferralFromUrl() }, [])
  return null
}
