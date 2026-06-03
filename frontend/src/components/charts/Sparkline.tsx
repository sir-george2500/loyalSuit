'use client'

interface SparklineProps {
  data: number[]
  width?: number
  height?: number
  className?: string
  strokeClass?: string
  fillClass?: string
}

/**
 * Lightweight, dependency-free area sparkline. Renders an SVG polyline + filled
 * area scaled to the data range. Flat/empty series render a baseline.
 */
export default function Sparkline({
  data,
  width = 320,
  height = 64,
  className,
  strokeClass = 'stroke-primary',
  fillClass = 'fill-primary/10',
}: SparklineProps) {
  const pad = 2
  const n = data.length
  const max = Math.max(...data, 0)
  const min = Math.min(...data, 0)
  const range = max - min || 1

  const x = (i: number) => (n <= 1 ? pad : pad + (i * (width - pad * 2)) / (n - 1))
  const y = (v: number) => height - pad - ((v - min) / range) * (height - pad * 2)

  const line = data.map((v, i) => `${x(i).toFixed(1)},${y(v).toFixed(1)}`).join(' ')
  const area =
    n > 0
      ? `${x(0).toFixed(1)},${(height - pad).toFixed(1)} ${line} ${x(n - 1).toFixed(1)},${(height - pad).toFixed(1)}`
      : ''

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      preserveAspectRatio="none"
      className={className}
      role="img"
      aria-label="Revenue trend"
    >
      {n > 1 && <polygon points={area} className={fillClass} stroke="none" />}
      {n > 1 && (
        <polyline
          points={line}
          fill="none"
          className={strokeClass}
          strokeWidth={2}
          strokeLinejoin="round"
          strokeLinecap="round"
          vectorEffect="non-scaling-stroke"
        />
      )}
    </svg>
  )
}
