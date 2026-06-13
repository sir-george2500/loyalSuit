import { Search } from 'lucide-react'

/**
 * Marketplace-wide product search. A plain GET form so it works without JS —
 * submitting navigates to /store/search?q=…, which the server component renders.
 */
export default function MarketplaceSearchBar({ defaultValue = '' }: { defaultValue?: string }) {
  return (
    <form action="/store/search" method="get" role="search" className="relative w-full">
      <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
      <input
        type="search"
        name="q"
        defaultValue={defaultValue}
        placeholder="Search parts on Loyal Spare Parts…"
        aria-label="Search parts on Loyal Spare Parts"
        className="w-full rounded-full border border-gray-300 bg-white py-2.5 pl-10 pr-4 text-sm text-gray-900 placeholder:text-gray-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
      />
    </form>
  )
}
