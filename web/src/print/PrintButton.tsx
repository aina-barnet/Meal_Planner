import { useCallback, useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useRecipe, useWeek } from '../api/hooks'
import { Button } from '../ui'
import { MenuSheet } from './MenuSheet'
import { RecipeSheet } from './RecipeSheet'

type Kind = 'menu' | 'recipe'

/**
 * Paper formats. Sizes in CSS pixels at 96 dpi, for A4 with 10 mm margins. Each sheet is scaled
 * down before printing if needed, so it always fits on a single page.
 */
const PAGE: Record<Kind, { size: string; widthMm: number; heightPx: number }> = {
  menu: { size: 'A4 landscape', widthMm: 277, heightPx: (190 * 96) / 25.4 },
  recipe: { size: 'A4 portrait', widthMm: 190, heightPx: (277 * 96) / 25.4 },
}

/** "Imprimeix": the weekly menu as a Monday-to-Friday grid. Recipes print from each dish on the board. */
export function PrintButton({ weekId, variant = 'secondary' }: { weekId: number; variant?: 'primary' | 'secondary' }) {
  const [printing, setPrinting] = useState(false)
  const week = useWeek(weekId)
  const done = useCallback(() => setPrinting(false), [])

  return (
    <div className="print:hidden">
      <Button variant={variant} disabled={!week.data} onClick={() => setPrinting(true)}
        title="Graella de dilluns a divendres, amb qui menja a casa, qui s'endú carmanyola i qui és fora">
        Imprimeix el menú
      </Button>
      {printing && week.data && (
        <PrintJob kind="menu" onDone={done}>
          <MenuSheet week={week.data} />
        </PrintJob>
      )}
    </div>
  )
}

/** Loads the full recipe (ingredients and steps), then prints it on its own page. */
export function RecipePrint({ recipeId, onDone }: { recipeId: number; onDone: () => void }) {
  const recipe = useRecipe(recipeId)
  if (!recipe.data) return null
  return (
    <PrintJob kind="recipe" onDone={onDone}>
      <RecipeSheet recipe={recipe.data} />
    </PrintJob>
  )
}

/**
 * Renders the sheet outside the app root (which print CSS hides), fits it to one page, prints,
 * and cleans up.
 */
function PrintJob({ kind, onDone, children }: { kind: Kind; onDone: () => void; children: React.ReactNode }) {
  const sheet = useRef<HTMLDivElement>(null)

  useLayoutEffect(() => {
    const page = PAGE[kind]
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
  }, [kind, onDone])

  return createPortal(
    <div ref={sheet} className="print-sheet" style={{ width: `${PAGE[kind].widthMm}mm` }}>
      {children}
    </div>,
    document.body,
  )
}
