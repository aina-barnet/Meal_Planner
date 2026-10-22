import { useMemo, useState } from 'react'
import { ApiError } from '../api/client'
import { useIngredients } from '../api/hooks'
import type { Allergen, AllergenKind, DietType, MemberInput } from '../api/types'
import { ALLERGEN, ALLERGEN_KIND, DIET } from '../labels'
import { Button, ErrorBox, Field, inputClass } from '../ui'

const ALLERGENS = Object.keys(ALLERGEN) as Allergen[]

export function MemberForm({
  initial,
  saving,
  error,
  onSubmit,
  onCancel,
}: {
  initial: MemberInput
  saving: boolean
  error: unknown
  onSubmit: (input: MemberInput) => void
  onCancel?: () => void
}) {
  const [form, setForm] = useState<MemberInput>(initial)
  const set = <K extends keyof MemberInput>(key: K, value: MemberInput[K]) => setForm((f) => ({ ...f, [key]: value }))
  const fieldError = (field: string) =>
    error instanceof ApiError ? error.fieldErrors.find((e) => e.field === field)?.message : undefined

  const cycleAllergen = (allergen: Allergen) => {
    const current = form.allergens.find((a) => a.allergen === allergen)?.kind
    const next: AllergenKind | null = current === undefined ? 'ALLERGY' : current === 'ALLERGY' ? 'INTOLERANCE' : null
    const others = form.allergens.filter((a) => a.allergen !== allergen)
    set('allergens', next ? [...others, { allergen, kind: next }] : others)
  }

  const canSubmit = form.name.trim() !== ''

  return (
    <form
      className="space-y-6"
      onSubmit={(e) => {
        e.preventDefault()
        if (canSubmit) onSubmit({ ...form, name: form.name.trim() })
      }}
    >
      <Field label="Nom" error={fieldError('name')}>
        <input className={inputClass} value={form.name} autoFocus onChange={(e) => set('name', e.target.value)} />
      </Field>

      <div className="grid grid-cols-2 gap-4">
        <Field label="Dieta">
          <div className="flex flex-wrap gap-2">
            {(Object.keys(DIET) as DietType[]).map((diet) => (
              <Choice key={diet} selected={form.dietType === diet} onClick={() => set('dietType', diet)}>
                {DIET[diet]}
              </Choice>
            ))}
          </div>
        </Field>
        <Field label="Altres observacions sobre la dieta" hint="Opcional. Només informatiu.">
          <input
            className={inputClass}
            value={form.dietNotes ?? ''}
            placeholder="p. ex. sense picant"
            onChange={(e) => set('dietNotes', e.target.value || null)}
          />
        </Field>
      </div>

      <Field
        label="Al·lèrgies i intoleràncies"
        hint="Clica per marcar al·lèrgia, torna a clicar per intolerància. Mai es proposaran plats que les continguin."
      >
        <div className="flex flex-wrap gap-2">
          {ALLERGENS.map((allergen) => {
            const kind = form.allergens.find((a) => a.allergen === allergen)?.kind
            return (
              <button
                key={allergen}
                type="button"
                onClick={() => cycleAllergen(allergen)}
                className={`rounded-full px-3 py-1 text-sm ring-1 transition-colors ${
                  kind === 'ALLERGY'
                    ? 'bg-terracotta-600 text-white ring-terracotta-600'
                    : kind === 'INTOLERANCE'
                      ? 'bg-terracotta-100 text-terracotta-800 ring-terracotta-300'
                      : 'bg-white text-stone-700 ring-stone-300 hover:bg-stone-50'
                }`}
              >
                {ALLERGEN[allergen]}
                {kind && <span className="ml-1 text-xs opacity-80">· {ALLERGEN_KIND[kind]}</span>}
              </button>
            )
          })}
        </div>
      </Field>

      <DislikePicker selected={form.dislikedIngredientIds} onChange={(ids) => set('dislikedIngredientIds', ids)} />

      <ErrorBox error={error} />

      <div className="flex justify-end gap-2">
        {onCancel && (
          <Button variant="ghost" onClick={onCancel}>
            Cancel·la
          </Button>
        )}
        <Button type="submit" disabled={!canSubmit || saving}>
          {saving ? 'Desant…' : 'Desa el membre'}
        </Button>
      </div>
    </form>
  )
}

function DislikePicker({ selected, onChange }: { selected: number[]; onChange: (ids: number[]) => void }) {
  const { data: ingredients = [] } = useIngredients()
  const [query, setQuery] = useState('')
  const byId = useMemo(() => new Map(ingredients.map((i) => [i.id, i])), [ingredients])
  const matches = useMemo(() => {
    const q = fold(query)
    if (!q) return []
    return ingredients.filter((i) => !selected.includes(i.id) && fold(i.name).includes(q)).slice(0, 8)
  }, [ingredients, query, selected])

  return (
    <Field label="No li agrada" hint="Opcional. S'evitarà quan sigui possible, però no és una prohibició.">
      <div className="space-y-2">
        {selected.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {selected.map((id) => (
              <span key={id} className="inline-flex items-center gap-1 rounded-full bg-stone-200 px-3 py-1 text-sm">
                {byId.get(id)?.name ?? id}
                <button
                  type="button"
                  aria-label="Treu"
                  className="text-stone-500 hover:text-stone-900"
                  onClick={() => onChange(selected.filter((s) => s !== id))}
                >
                  ×
                </button>
              </span>
            ))}
          </div>
        )}
        <div className="relative">
          <input
            className={inputClass}
            value={query}
            placeholder="Cerca un ingredient…"
            onChange={(e) => setQuery(e.target.value)}
          />
          {matches.length > 0 && (
            <ul className="absolute z-10 mt-1 w-full overflow-hidden rounded-lg bg-white shadow-lg ring-1 ring-stone-200">
              {matches.map((ingredient) => (
                <li key={ingredient.id}>
                  <button
                    type="button"
                    className="w-full px-3 py-2 text-left text-sm hover:bg-brand-50"
                    onClick={() => {
                      onChange([...selected, ingredient.id])
                      setQuery('')
                    }}
                  >
                    {ingredient.name}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </Field>
  )
}

function Choice({
  selected,
  onClick,
  children,
  className = '',
}: {
  selected: boolean
  onClick: () => void
  children: React.ReactNode
  className?: string
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={selected}
      className={`flex rounded-lg px-3 py-2 text-sm ring-1 transition-colors ${
        selected
          ? 'bg-brand-600 text-white ring-brand-600'
          : 'bg-white text-stone-700 ring-stone-300 hover:bg-stone-50'
      } ${className}`}
    >
      {children}
    </button>
  )
}

function fold(text: string): string {
  return text.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim()
}
