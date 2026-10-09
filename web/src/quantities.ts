import type { CanonicalUnit } from './api/types'

const N0 = new Intl.NumberFormat('ca', { maximumFractionDigits: 0 })
const N1 = new Intl.NumberFormat('ca', { maximumFractionDigits: 1 })
const N2 = new Intl.NumberFormat('ca', { maximumFractionDigits: 2 })

/** 1250 g → "1,25 kg"; 300 g → "300 g"; 0.3 g → "0,3 g"; 1500 ml → "1,5 l". */
export function formatQuantity(value: number, unit: CanonicalUnit): string {
  const small = unit === 'G' ? 'g' : 'ml'
  const big = unit === 'G' ? 'kg' : 'l'
  if (value >= 1000) return `${N2.format(value / 1000)} ${big}`
  if (value >= 10) return `${N0.format(value)} ${small}`
  return `${N1.format(value)} ${small}`
}

/** "2 × 500 g" or "1 × 1 kg". */
export function formatPackages(packages: number, packageSize: number, unit: CanonicalUnit): string {
  return `${packages} × ${formatQuantity(packageSize, unit)}`
}
