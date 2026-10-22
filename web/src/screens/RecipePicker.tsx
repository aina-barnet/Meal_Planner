import { useEffect, useMemo, useState } from 'react'
import { useAllowedRecipes } from '../api/hooks'
import type { Meal, ProteinType, RecipeSummary } from '../api/types'
import { useProteinName } from '../proteins'
import { ALLERGEN, SLOT, formatWeekday } from '../labels'
import { Button, ErrorBox, LunchboxIcon, Spinner, inputClass } from '../ui'

/** Modal listing the recipes that satisfy a meal's hard constraints. */
export function RecipePicker({
  weekId,
  meal,
  onPick,
  onClose,
  busy,
  error,
}: {
  weekId: number
  meal: Meal
  onPick: (recipe: RecipeSummary) => void
  onClose: () => void
  busy: boolean
  error: unknown
}) {
  const recipes = useAllowedRecipes(weekId, meal.id)
  const proteinName = useProteinName()
  const [query, setQuery] = useState('')
  const [protein, setProtein] = useState<ProteinType | null>(null)

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  const proteins = useMemo(
    () => [...new Set((recipes.data ?? []).map((r) => r.proteinType))].sort(),
    [recipes.data],
  )
  const shown = useMemo(() => {
    const q = fold(query)
    return (recipes.data ?? [])
      .filter((r) => !protein || r.proteinType === protein)
      .filter((r) => !q || fold(r.name).includes(q))
  }, [recipes.data, protein, query])

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-stone-900/40 p-8" onClick={onClose}>
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Tria un plat"
        className="flex max-h-[85vh] w-full max-w-3xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="border-b border-stone-200 p-5">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm text-stone-500">
                {SLOT[meal.slot]} de {formatWeekday(meal.date).toLowerCase()}
              </p>
              <h2 className="text-lg font-semibold">Tria un plat</h2>
            </div>
            <Button variant="ghost" onClick={onClose} aria-label="Tanca">
              ✕
            </Button>
          </div>
          <p className="mt-1 text-xs text-stone-500">
            Només es mostren plats compatibles amb qui menja
            {meal.dinersTakeaway > 0 ? ' i que aguanten la carmanyola' : ''}.
          </p>
          <input
            autoFocus
            className={`${inputClass} mt-3`}
            placeholder="Cerca per nom…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <div className="mt-3 flex flex-wrap gap-1.5">
            <FilterChip active={protein === null} onClick={() => setProtein(null)}>
              Tots
            </FilterChip>
            {proteins.map((p) => (
              <FilterChip key={p} active={protein === p} onClick={() => setProtein(p)}>
                {proteinName(p)}
              </FilterChip>
            ))}
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-2">
          {recipes.isPending && <Spinner />}
          <ErrorBox error={recipes.error ?? error} />
          {recipes.isSuccess && shown.length === 0 && (
            <p className="p-6 text-center text-sm text-stone-500">Cap plat no coincideix amb la cerca.</p>
          )}
          <ul>
            {shown.map((recipe) => (
              <li key={recipe.id}>
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => onPick(recipe)}
                  className={`flex w-full items-center gap-4 rounded-xl px-4 py-3 text-left hover:bg-brand-50 disabled:opacity-50 ${
                    meal.recipe?.id === recipe.id ? 'bg-brand-50 ring-1 ring-brand-300' : ''
                  }`}
                >
                  <div className="min-w-0 flex-1">
                    <p className="flex items-center gap-2 font-medium text-stone-900">
                      {recipe.name}
                      {recipe.portable && <LunchboxIcon className="size-4 text-terracotta-600" />}
                    </p>
                    <p className="truncate text-xs text-stone-500">
                      {proteinName(recipe.proteinType)} · {recipe.prepMinutes} min
                      {recipe.allergens.length > 0 &&
                        ` · conté ${recipe.allergens.map((a) => ALLERGEN[a].toLowerCase()).join(', ')}`}
                    </p>
                  </div>
                </button>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}

function FilterChip({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-full px-3 py-1 text-xs ring-1 ${
        active ? 'bg-brand-600 text-white ring-brand-600' : 'bg-white text-stone-700 ring-stone-300 hover:bg-stone-50'
      }`}
    >
      {children}
    </button>
  )
}

function fold(text: string): string {
  return text.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim()
}
