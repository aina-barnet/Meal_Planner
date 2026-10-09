import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useShoppingList, useWeek } from '../api/hooks'
import { Button } from '../ui'
import { MenuSheet } from './MenuSheet'
import { ShoppingSheet } from './ShoppingSheet'

type Mode = 'menu' | 'list'

/**
 * Paper formats. Sizes in CSS pixels at 96 dpi, for A4 with 10 mm margins. Each sheet is scaled
 * down before printing if needed, so it always fits on a single page.
 */
const PAGE: Record<Mode, { size: string; widthMm: number; heightPx: number }> = {
  menu: { size: 'A4 landscape', widthMm: 277, heightPx: (190 * 96) / 25.4 },
  list: { size: 'A4 portrait', widthMm: 190, heightPx: (277 * 96) / 25.4 },
}

/** "Imprimeix ▾": the weekly menu, or the shopping list once the week is confirmed. */
export function PrintButton({ weekId, variant = 'secondary' }: { weekId: number; variant?: 'primary' | 'secondary' }) {
  const [open, setOpen] = useState(false)
  const [mode, setMode] = useState<Mode | null>(null)
  const week = useWeek(weekId)
  const confirmed = week.data?.status === 'CONFIRMED'
  const list = useShoppingList(weekId, confirmed)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const close = (e: MouseEvent) => {
      if (!menuRef.current?.contains(e.target as Node)) setOpen(false)
    }
    window.addEventListener('mousedown', close)
    return () => window.removeEventListener('mousedown', close)
  }, [open])

  const choose = (m: Mode) => {
    setOpen(false)
    setMode(m)
  }

  return (
    <div ref={menuRef} className="relative print:hidden">
      <Button variant={variant} onClick={() => setOpen((o) => !o)} aria-haspopup="menu" aria-expanded={open}>
        Imprimeix ▾
      </Button>
      {open && (
        <div role="menu" className="absolute right-0 z-30 mt-1 w-64 overflow-hidden rounded-xl bg-white text-sm shadow-xl ring-1 ring-stone-200">
          <MenuItem onClick={() => choose('menu')} title="Menú setmanal" hint="Graella dels 7 dies, apaïsat" />
          <MenuItem
            onClick={() => choose('list')}
            disabled={!confirmed || !list.data}
            title="Llista de la compra"
            hint={confirmed ? 'Per passadissos, en una pàgina' : 'Primer cal confirmar el menú'}
          />
        </div>
      )}
      {mode && week.data && (mode === 'menu' || list.data) && (
        <PrintJob mode={mode} onDone={() => setMode(null)}>
          {mode === 'menu' ? <MenuSheet week={week.data} /> : <ShoppingSheet week={week.data} list={list.data!} />}
        </PrintJob>
      )}
    </div>
  )
}

function MenuItem({ onClick, title, hint, disabled = false }: { onClick: () => void; title: string; hint: string; disabled?: boolean }) {
  return (
    <button type="button" role="menuitem" disabled={disabled} onClick={onClick}
      className="block w-full px-4 py-2.5 text-left hover:bg-brand-50 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:bg-transparent">
      <span className="block font-medium text-stone-900">{title}</span>
      <span className="block text-xs text-stone-500">{hint}</span>
    </button>
  )
}

/**
 * Renders the sheet outside the app root (which print CSS hides), fits it to one page, prints,
 * and cleans up.
 */
function PrintJob({ mode, onDone, children }: { mode: Mode; onDone: () => void; children: React.ReactNode }) {
  const sheet = useRef<HTMLDivElement>(null)

  useLayoutEffect(() => {
    const page = PAGE[mode]
    const style = document.createElement('style')
    style.textContent = `@page { size: ${page.size}; margin: 10mm; }`
    document.head.appendChild(style)
    document.body.classList.add('printing')

    const el = sheet.current!
    el.style.zoom = '1'
    const height = el.scrollHeight
    if (height > page.heightPx) el.style.zoom = String(Math.floor((page.heightPx / height) * 1000) / 1000)

    const finish = () => {
      document.body.classList.remove('printing')
      style.remove()
      onDone()
    }
    // Let the browser lay the sheet out before opening the print dialog.
    const timer = window.setTimeout(() => {
      window.addEventListener('afterprint', finish, { once: true })
      window.print()
    }, 50)
    return () => {
      window.clearTimeout(timer)
      window.removeEventListener('afterprint', finish)
      document.body.classList.remove('printing')
      style.remove()
    }
  }, [mode, onDone])

  return createPortal(
    <div ref={sheet} className="print-sheet" style={{ width: `${PAGE[mode].widthMm}mm` }}>
      {children}
    </div>,
    document.body,
  )
}
