import Image from 'next/image'
import Link from 'next/link'
import { BRAND } from '@/lib/brand'

/**
 * The official Loyal Spare Parts logo. Always renders the supplied asset unmodified (per the brand
 * guideline). On dark surfaces pass {@code chip} to seat it on a white plate so it stays legible.
 */
export default function Logo({
  href = '/',
  className = 'h-10',
  chip = false,
  priority = false,
}: {
  href?: string | null
  className?: string
  chip?: boolean
  priority?: boolean
}) {
  const img = (
    <Image
      src="/logo.png"
      alt={BRAND.name}
      width={512}
      height={512}
      priority={priority}
      className={`${className} w-auto`}
    />
  )
  const content = chip ? (
    <span className="inline-flex items-center rounded-lg bg-white px-2 py-1 shadow-sm">{img}</span>
  ) : (
    img
  )

  return href ? (
    <Link href={href} aria-label={BRAND.name} className="inline-flex items-center">
      {content}
    </Link>
  ) : (
    content
  )
}
