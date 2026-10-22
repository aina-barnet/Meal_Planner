import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { ApiError } from './api/client'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-brand-600 text-white shadow-sm shadow-brand-600/20 hover:bg-brand-700 disabled:bg-brand-200 disabled:shadow-none',
  secondary: 'bg-white text-stone-800 ring-1 ring-brand-200 hover:bg-brand-50 disabled:text-stone-400 disabled:ring-stone-200',
  ghost: 'text-stone-700 hover:bg-brand-50 hover:text-brand-800',
  danger: 'text-terracotta-700 hover:bg-terracotta-50 disabled:text-stone-300 disabled:hover:bg-transparent',
}

export function Button({
  variant = 'primary',
  className = '',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant }) {
  return (
    <button
      type="button"
      className={`inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition-colors disabled:cursor-not-allowed ${VARIANTS[variant]} ${className}`}
      {...props}
    />
  )
}

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`rounded-3xl bg-white p-6 shadow-sm ring-1 ring-cream-300 ${className}`}>{children}</div>
}

export function Field({
  label,
  hint,
  error,
  children,
}: {
  label: string
  hint?: string
  error?: string
  children: ReactNode
}) {
  return (
    <label className="block">
      <span className="text-sm font-medium text-stone-700">{label}</span>
      <div className="mt-1">{children}</div>
      {error ? (
        <span className="mt-1 block text-xs text-terracotta-700">{error}</span>
      ) : hint ? (
        <span className="mt-1 block text-xs text-stone-500">{hint}</span>
      ) : null}
    </label>
  )
}

export const inputClass =
  'block w-full rounded-lg border-0 bg-white px-3 py-2 text-sm text-stone-900 ring-1 ring-stone-300 placeholder:text-stone-400 focus:ring-2 focus:ring-brand-600 focus:outline-none'

/** Shows an API problem: its detail and, for rule violations, each reason. */
export function ErrorBox({ error }: { error: unknown }) {
  if (!error) return null
  const message = error instanceof Error ? error.message : 'Alguna cosa no ha anat bé.'
  const reasons = error instanceof ApiError ? error.reasons : []
  return (
    <div role="alert" className="rounded-lg bg-terracotta-50 px-4 py-3 text-sm text-terracotta-800 ring-1 ring-terracotta-200">
      <p className="font-medium">{message}</p>
      {reasons.length > 0 && (
        <ul className="mt-1 list-disc pl-5">
          {reasons.map((reason) => (
            <li key={reason}>{reason}</li>
          ))}
        </ul>
      )}
    </div>
  )
}

export function Spinner({ label = 'Carregant…' }: { label?: string }) {
  return (
    <div className="flex items-center gap-3 py-10 text-sm text-stone-500">
      <span className="size-4 animate-spin rounded-full border-2 border-stone-300 border-t-brand-600" />
      {label}
    </div>
  )
}

export function LunchboxIcon({ className = 'size-4' }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className={className} aria-hidden>
      <rect x="3" y="9" width="18" height="11" rx="2.5" />
      <path d="M3 13h18M9 9V6.5A1.5 1.5 0 0 1 10.5 5h3A1.5 1.5 0 0 1 15 6.5V9" strokeLinecap="round" />
    </svg>
  )
}

export function PrintIcon({ className = 'size-4' }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className={className} aria-hidden>
      <path d="M7 9V4h10v5M7 17H5a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-2" strokeLinejoin="round" />
      <rect x="7" y="14" width="10" height="6" rx="1" />
    </svg>
  )
}

export function HomeIcon({ className = 'size-4' }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className={className} aria-hidden>
      <path d="M4 11.5 12 5l8 6.5V19a1 1 0 0 1-1 1h-4.5v-5h-5v5H5a1 1 0 0 1-1-1z" strokeLinejoin="round" />
    </svg>
  )
}
