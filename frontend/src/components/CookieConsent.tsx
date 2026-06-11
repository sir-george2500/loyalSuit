'use client'

import { useEffect, useState } from 'react'
import { Cookie } from 'lucide-react'

const STORAGE_KEY = 'ls_cookie_consent'

/**
 * A lightweight cookie-consent banner. Records the choice in localStorage so it shows once;
 * essential cookies (auth/cart) always function — this governs the optional/analytics layer.
 */
export default function CookieConsent() {
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    if (!localStorage.getItem(STORAGE_KEY)) {
      setVisible(true)
    }
  }, [])

  const choose = (choice: 'accepted' | 'declined') => {
    localStorage.setItem(STORAGE_KEY, choice)
    setVisible(false)
  }

  if (!visible) return null

  return (
    <div className="fixed inset-x-0 bottom-0 z-50 border-t border-gray-200 bg-white p-4 shadow-lg">
      <div className="mx-auto flex max-w-7xl flex-col items-start gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-start gap-2 text-sm text-gray-600">
          <Cookie className="mt-0.5 h-5 w-5 shrink-0 text-gray-400" />
          <p>
            We use cookies to keep you signed in and your cart working, and optional ones to improve the store.
            See our <a href="/privacy" className="link link-primary">privacy notice</a>.
          </p>
        </div>
        <div className="flex shrink-0 gap-2">
          <button onClick={() => choose('declined')} className="btn btn-ghost btn-sm">Decline optional</button>
          <button onClick={() => choose('accepted')} className="btn btn-primary btn-sm">Accept all</button>
        </div>
      </div>
    </div>
  )
}
