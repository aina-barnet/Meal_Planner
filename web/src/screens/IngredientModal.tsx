import { useEffect, useState } from 'react'
import { useSaveIngredient } from '../api/hooks'
import type { Aisle, Allergen, CanonicalUnit, Ingredient, IngredientCategory, IngredientInput, Unit } from '../api/types'
import { AISLE, ALLERGEN, CATEGORY, COUNTABLE_UNITS, UNIT } from '../labels'
import { Button, ErrorBox, Field, inputClass } from '../ui'

/** Creates an ingredient of the family's own, or edits one they added. */
export function IngredientModal({
  initial,
  initialName = '',
  onSaved,
  onClose,
}: {
  initial?: Ingredient
  initialName?: string
  onSaved: (ingredient: Ingredient) => void
  onClose: () => void
}) {
  const save = useSaveIngredient()
  const [form, setForm] = useState<IngredientInput>(() =>
    initial
      ? {
          name: initial.name,
          category: initial.category,
          canonicalUnit: initial.canonicalUnit,
          packageSize: initial.packageSize,
          aisle: initial.aisle,
          kcalPer100: initial.kcalPer100,
          proteinPer100: initial.proteinPer100,
          carbsPer100: initial.carbsPer100,
          fatPer100: initial.fatPer100,
          allergens: initial.allergens,
          conversions: initial.conversions,
          pantryStaple: initial.pantryStaple,
        }
      : {
          name: initialName,
          category: 'VEGETABLE',
          canonicalUnit: 'G',
          packageSize: 500,
          aisle: 'PRODUCE',
          kcalPer100: 0,
          proteinPer100: 0,
          carbsPer100: 0,
          fatPer100: 0,
          allergens: [],
          conversions: {},
          pantryStaple: false,
        },
  )
  const set = <K extends keyof IngredientInput>(key: K, value: IngredientInput[K]) => setForm((f) => ({ ...f, [key]: value }))
  const base = form.canonicalUnit === 'G' ? 'g' : 'ml'
  const conversions = Object.entries(form.conversions) as [Unit, number][]
  const freeUnits = COUNTABLE_UNITS.filter((u) => form.conversions[u] === undefined)

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  const number = (key: 'packageSize' | 'kcalPer100' | 'proteinPer100' | 'carbsPer100' | 'fatPer100', label: string, hint?: string) => (
    <Field label={label} hint={hint}>
      <input
        type="number"
        min={0}
        step="any"
        className={inputClass}
        value={Number.isFinite(form[key]) ? form[key] : ''}
        onChange={(e) => set(key, Number(e.target.value))}
      />
    </Field>
  )

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-stone-900/40 p-8" onClick={onClose}>
      <form
        role="dialog"
        aria-modal="true"
        aria-label={initial ? 'Edita l’ingredient' : 'Nou ingredient'}
        className="flex max-h-[90vh] w-full max-w-2xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl"
        onClick={(e) => e.stopPropagation()}
        onSubmit={(e) => {
          e.preventDefault()
          save.mutate({ id: initial?.id, input: { ...form, name: form.name.trim() } }, { onSuccess: onSaved })
        }}
      >
        <div className="border-b border-stone-200 px-6 py-4">
          <h2 className="text-lg font-semibold">{initial ? `Edita «${initial.name}»` : 'Nou ingredient'}</h2>
          <p className="text-xs text-stone-500">
            Les dades nutricionals són per 100 {base}. Les trobareu a l'etiqueta del paquet.
          </p>
        </div>

        <div className="flex-1 space-y-5 overflow-y-auto px-6 py-5">
          <div className="grid grid-cols-2 gap-4">
            <Field label="Nom">
              <input className={inputClass} autoFocus value={form.name} onChange={(e) => set('name', e.target.value)} />
            </Field>
            <Field label="Tipus" hint="Serveix per saber si és apte per a cada dieta">
              <select className={inputClass} value={form.category} onChange={(e) => set('category', e.target.value as IngredientCategory)}>
                {(Object.keys(CATEGORY) as IngredientCategory[]).map((c) => (
                  <option key={c} value={c}>
                    {CATEGORY[c]}
                  </option>
                ))}
              </select>
            </Field>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <Field label="Es mesura en">
              <div className="flex gap-2">
                {(['G', 'ML'] as CanonicalUnit[]).map((u) => (
                  <button
                    key={u}
                    type="button"
                    disabled={initial !== undefined}
                    onClick={() => set('canonicalUnit', u)}
                    className={`flex-1 rounded-lg px-3 py-2 text-sm ring-1 disabled:opacity-60 ${
                      form.canonicalUnit === u ? 'bg-brand-600 text-white ring-brand-600' : 'bg-white ring-stone-300'
                    }`}
                  >
                    {u === 'G' ? 'grams' : 'mil·lilitres'}
                  </button>
                ))}
              </div>
            </Field>
            {number('packageSize', `Paquet (${base})`, 'El que compreu a la botiga')}
            <Field label="Passadís">
              <select className={inputClass} value={form.aisle} onChange={(e) => set('aisle', e.target.value as Aisle)}>
                {(Object.keys(AISLE) as Aisle[]).map((a) => (
                  <option key={a} value={a}>
                    {AISLE[a]}
                  </option>
                ))}
              </select>
            </Field>
          </div>

          <div className="grid grid-cols-4 gap-4">
            {number('kcalPer100', 'kcal')}
            {number('proteinPer100', 'Proteïnes (g)')}
            {number('carbsPer100', 'Hidrats (g)')}
            {number('fatPer100', 'Greixos (g)')}
          </div>

          <Field label="Al·lèrgens">
            <div className="flex flex-wrap gap-1.5">
              {(Object.keys(ALLERGEN) as Allergen[]).map((a) => {
                const on = form.allergens.includes(a)
                return (
                  <button
                    key={a}
                    type="button"
                    onClick={() => set('allergens', on ? form.allergens.filter((x) => x !== a) : [...form.allergens, a])}
                    className={`rounded-full px-3 py-1 text-xs ring-1 ${
                      on ? 'bg-terracotta-600 text-white ring-terracotta-600' : 'bg-white text-stone-700 ring-stone-300'
                    }`}
                  >
                    {ALLERGEN[a]}
                  </button>
                )
              })}
            </div>
          </Field>

          <Field label="Equivalències" hint={`Per poder posar-lo a les receptes en unitats, cullerades… (p. ex. 1 unitat = 60 ${base})`}>
            <div className="space-y-2">
              {conversions.map(([unit, factor]) => (
                <div key={unit} className="flex items-center gap-2 text-sm">
                  <span className="w-28">1 {UNIT[unit]} =</span>
                  <input
                    type="number"
                    min={0}
                    step="any"
                    className={`${inputClass} w-28! shrink-0`}
                    value={factor}
                    onChange={(e) => set('conversions', { ...form.conversions, [unit]: Number(e.target.value) })}
                  />
                  <span>{base}</span>
                  <Button
                    variant="danger"
                    className="px-2 py-1"
                    onClick={() => {
                      const next = { ...form.conversions }
                      delete next[unit]
                      set('conversions', next)
                    }}
                  >
                    Treu
                  </Button>
                </div>
              ))}
              {freeUnits.length > 0 && (
                <select
                  className={`${inputClass} w-56! shrink-0`}
                  value=""
                  onChange={(e) => e.target.value && set('conversions', { ...form.conversions, [e.target.value]: 0 })}
                >
                  <option value="">+ Afegeix una equivalència…</option>
                  {freeUnits.map((u) => (
                    <option key={u} value={u}>
                      {UNIT[u]}
                    </option>
                  ))}
                </select>
              )}
            </div>
          </Field>

          <label className="flex items-start gap-2 text-sm">
            <input
              type="checkbox"
              className="mt-0.5 accent-brand-700"
              checked={form.pantryStaple}
              onChange={(e) => set('pantryStaple', e.target.checked)}
            />
            <span>
              <strong className="font-medium">Bàsic de rebost</strong>
              <span className="block text-xs text-stone-500">Sempre en teniu (com l'oli o la sal): no surt a la llista de la compra.</span>
            </span>
          </label>

          <ErrorBox error={save.error} />
        </div>

        <div className="flex justify-end gap-2 border-t border-stone-200 px-6 py-4">
          <Button variant="ghost" onClick={onClose}>
            Cancel·la
          </Button>
          <Button
            type="submit"
            disabled={!form.name.trim() || !(form.packageSize > 0) || conversions.some(([, f]) => !(f > 0)) || save.isPending}
          >
            {save.isPending ? 'Desant…' : 'Desa l’ingredient'}
          </Button>
        </div>
      </form>
    </div>
  )
}
