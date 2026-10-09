import { useMemo, useState } from 'react'
import { useHideRecipe, useIngredients, useProteins, useRecipe, useSaveRecipe, useSaveProtein } from '../api/hooks'
import type {
  Cuisine,
  Ingredient,
  RecipeDetail,
  RecipeInput,
  ScalingMode,
  Season,
  Unit,
} from '../api/types'
import { type Line, allergensOf, dietsOf, nutritionPerServing, supportedUnits } from '../catalogue'
import { ALLERGEN, CUISINE, DIET, SCALING, SEASON, UNIT, formatNumber } from '../labels'
import { navigate } from '../router'
import { Button, Card, ErrorBox, Field, LunchboxIcon, Spinner, inputClass } from '../ui'
import { IngredientModal } from './IngredientModal'

interface LineDraft {
  key: number
  ingredientId: number | null
  quantity: string
  unit: Unit
  scalingMode: ScalingMode
}

interface Draft extends Omit<RecipeInput, 'ingredients' | 'description'> {
  description: string
  lines: LineDraft[]
}

let nextKey = 1

function emptyDraft(): Draft {
  return {
    name: '',
    description: '',
    baseServings: 4,
    prepMinutes: 30,
    proteinType: 'VEGETABLES',
    cuisine: 'OTHER',
    seasons: ['SPRING', 'SUMMER', 'AUTUMN', 'WINTER'],
    portable: false,
    lunchboxServing: null,
    lines: [{ key: nextKey++, ingredientId: null, quantity: '', unit: 'G', scalingMode: 'LINEAR' }],
    steps: [''],
  }
}

function fromDetail(detail: RecipeDetail): Draft {
  const s = detail.summary
  return {
    name: s.name,
    description: s.description ?? '',
    baseServings: s.baseServings,
    prepMinutes: s.prepMinutes,
    proteinType: s.proteinType,
    cuisine: s.cuisine,
    seasons: s.seasons,
    portable: s.portable,
    lunchboxServing: s.lunchboxServing,
    lines: detail.ingredients.map((l) => ({
      key: nextKey++,
      ingredientId: l.ingredientId,
      quantity: String(l.quantity),
      unit: l.unit,
      scalingMode: l.scalingMode,
    })),
    steps: detail.steps.length > 0 ? detail.steps : [''],
  }
}

export function RecipeEditor({ recipeId }: { recipeId: number | null }) {
  const recipe = useRecipe(recipeId)
  const ingredients = useIngredients()
  if ((recipeId !== null && recipe.isPending) || ingredients.isPending) return <Spinner />
  if (recipe.error) return <ErrorBox error={recipe.error} />
  return (
    <Editor
      recipeId={recipeId}
      detail={recipe.data}
      ingredients={ingredients.data ?? []}
    />
  )
}

function Editor({ recipeId, detail, ingredients }: { recipeId: number | null; detail?: RecipeDetail; ingredients: Ingredient[] }) {
  const save = useSaveRecipe()
  const hide = useHideRecipe()
  const [draft, setDraft] = useState<Draft>(() => (detail ? fromDetail(detail) : emptyDraft()))
  const [creating, setCreating] = useState<{ lineKey: number; name: string } | null>(null)
  const [editingIngredient, setEditingIngredient] = useState<Ingredient | null>(null)
  const byId = useMemo(() => new Map(ingredients.map((i) => [i.id, i])), [ingredients])
  const set = <K extends keyof Draft>(key: K, value: Draft[K]) => setDraft((d) => ({ ...d, [key]: value }))
  const setLine = (key: number, change: Partial<LineDraft>) =>
    setDraft((d) => ({ ...d, lines: d.lines.map((l) => (l.key === key ? { ...l, ...change } : l)) }))

  const chooseIngredient = (lineKey: number, ingredient: Ingredient) => {
    const line = draft.lines.find((l) => l.key === lineKey)
    const units = supportedUnits(ingredient)
    const unit = line && units.includes(line.unit) ? line.unit : (units.find((u) => u === 'UNIT') ?? units[0])
    setLine(lineKey, { ingredientId: ingredient.id, unit })
  }

  // Lines complete enough for the live preview.
  const previewLines: Line[] = draft.lines.flatMap((l) => {
    const ingredient = l.ingredientId !== null ? byId.get(l.ingredientId) : undefined
    const quantity = Number(l.quantity)
    return ingredient && quantity > 0 ? [{ ingredient, quantity, unit: l.unit }] : []
  })
  const nutrition = nutritionPerServing(previewLines, draft.baseServings)
  const allergens = allergensOf(previewLines)
  const diets = dietsOf(previewLines)

  const steps = draft.steps.map((s) => s.trim()).filter(Boolean)
  const incomplete = draft.lines.some((l) => l.ingredientId === null || !(Number(l.quantity) > 0))
  const problems = [
    !draft.name.trim() && 'Falta el nom.',
    previewLines.length === 0 && 'Afegiu almenys un ingredient.',
    incomplete && 'Hi ha ingredients sense triar o sense quantitat.',
    steps.length === 0 && 'Afegiu almenys un pas.',
    draft.seasons.length === 0 && 'Trieu almenys una temporada.',
    draft.portable && !draft.lunchboxServing && 'Indiqueu si es menja fred o cal escalfar-lo.',
  ].filter(Boolean) as string[]

  const submit = () => {
    const input: RecipeInput = {
      name: draft.name.trim(),
      description: draft.description.trim() || null,
      baseServings: draft.baseServings,
      prepMinutes: draft.prepMinutes,
      proteinType: draft.proteinType,
      cuisine: draft.cuisine,
      seasons: draft.seasons,
      portable: draft.portable,
      lunchboxServing: draft.portable ? draft.lunchboxServing : null,
      ingredients: draft.lines.map((l) => ({
        ingredientId: l.ingredientId!,
        quantity: Number(l.quantity),
        unit: l.unit,
        scalingMode: l.scalingMode,
      })),
      steps,
    }
    save.mutate({ id: recipeId ?? undefined, input }, { onSuccess: () => navigate({ screen: 'recipes' }) })
  }

  const moveStep = (i: number, delta: number) => {
    const next = [...draft.steps]
    const [step] = next.splice(i, 1)
    next.splice(i + delta, 0, step)
    set('steps', next)
  }

  return (
    <div className="mx-auto max-w-6xl space-y-5 pb-10">
      <header className="flex items-end justify-between">
        <div>
          <button type="button" className="text-sm text-stone-500 hover:text-stone-800" onClick={() => navigate({ screen: 'recipes' })}>
            ← Plats
          </button>
          <h1 className="text-2xl font-semibold">{recipeId === null ? 'Nou plat' : `Edita «${detail?.summary.name}»`}</h1>
          {detail && !detail.summary.custom && (
            <p className="text-sm text-stone-500">Plat del catàleg. Els canvis s'apliquen a partir d'ara als menús i llistes nous.</p>
          )}
        </div>
        {detail && (
          <Button
            variant={detail.summary.hidden ? 'secondary' : 'danger'}
            disabled={hide.isPending}
            onClick={() => hide.mutate({ id: detail.summary.id, hidden: !detail.summary.hidden }, { onSuccess: () => navigate({ screen: 'recipes' }) })}
          >
            {detail.summary.hidden ? 'Torna a mostrar' : 'Amaga aquest plat'}
          </Button>
        )}
      </header>

      <div className="grid grid-cols-[1fr_18rem] items-start gap-5">
        <div className="space-y-5">
          <Card className="space-y-4">
            <div className="grid grid-cols-[1fr_8rem_8rem] gap-4">
              <Field label="Nom del plat">
                <input className={inputClass} autoFocus={recipeId === null} value={draft.name} onChange={(e) => set('name', e.target.value)} />
              </Field>
              <Field label="Racions" hint="De la recepta tal com és">
                <input type="number" min={1} max={20} className={inputClass} value={draft.baseServings}
                  onChange={(e) => set('baseServings', Math.max(1, Number(e.target.value)))} />
              </Field>
              <Field label="Temps (min)">
                <input type="number" min={1} max={600} className={inputClass} value={draft.prepMinutes}
                  onChange={(e) => set('prepMinutes', Math.max(1, Number(e.target.value)))} />
              </Field>
            </div>
            <Field label="Descripció" hint="Opcional, una línia">
              <input className={inputClass} value={draft.description} onChange={(e) => set('description', e.target.value)} />
            </Field>
            <div className="grid grid-cols-2 gap-4">
              <ProteinField value={draft.proteinType} onChange={(code) => set('proteinType', code)} />
              <Field label="Cuina">
                <select className={inputClass} value={draft.cuisine} onChange={(e) => set('cuisine', e.target.value as Cuisine)}>
                  {(Object.keys(CUISINE) as Cuisine[]).map((c) => (
                    <option key={c} value={c}>{CUISINE[c]}</option>
                  ))}
                </select>
              </Field>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <Field label="Temporada">
                <div className="flex flex-wrap gap-1.5">
                  {(Object.keys(SEASON) as Season[]).map((season) => {
                    const on = draft.seasons.includes(season)
                    return (
                      <Toggle key={season} on={on} onClick={() => set('seasons', on ? draft.seasons.filter((x) => x !== season) : [...draft.seasons, season])}>
                        {SEASON[season]}
                      </Toggle>
                    )
                  })}
                </div>
              </Field>
              <Field label="Carmanyola" hint="Aguanta bé que el poseu en un tàper, a la nevera i fora de casa?">
                <div className="flex flex-wrap gap-1.5">
                  <Toggle on={!draft.portable} onClick={() => setDraft((d) => ({ ...d, portable: false, lunchboxServing: null }))}>
                    No
                  </Toggle>
                  <Toggle on={draft.portable && draft.lunchboxServing === 'COLD'} onClick={() => setDraft((d) => ({ ...d, portable: true, lunchboxServing: 'COLD' }))}>
                    <LunchboxIcon className="size-3.5" /> Sí, fred
                  </Toggle>
                  <Toggle on={draft.portable && draft.lunchboxServing === 'REHEAT'} onClick={() => setDraft((d) => ({ ...d, portable: true, lunchboxServing: 'REHEAT' }))}>
                    <LunchboxIcon className="size-3.5" /> Sí, escalfant
                  </Toggle>
                </div>
              </Field>
            </div>
          </Card>

          <Card className="space-y-3">
            <div className="flex items-baseline justify-between">
              <h2 className="font-semibold">Ingredients</h2>
              <span className="text-xs text-stone-500">per a {draft.baseServings} racions</span>
            </div>
            <div className="grid grid-cols-[1fr_6rem_9rem_9rem_2rem] gap-2 text-xs text-stone-500">
              <span>Ingredient</span>
              <span>Quantitat</span>
              <span>Unitat</span>
              <span title="Com canvia la quantitat quan cuineu per a més o menys gent">Si canvien les racions</span>
              <span />
            </div>
            {draft.lines.map((line) => {
              const ingredient = line.ingredientId !== null ? byId.get(line.ingredientId) : undefined
              const units = ingredient ? supportedUnits(ingredient) : (['G'] as Unit[])
              return (
                <div key={line.key} className="grid grid-cols-[1fr_6rem_9rem_9rem_2rem] items-center gap-2">
                  <IngredientPicker
                    ingredients={ingredients}
                    selected={ingredient}
                    onSelect={(i) => chooseIngredient(line.key, i)}
                    onCreate={(name) => setCreating({ lineKey: line.key, name })}
                    onEdit={ingredient?.custom ? () => setEditingIngredient(ingredient) : undefined}
                  />
                  <input type="number" min={0} step="any" className={inputClass} value={line.quantity}
                    onChange={(e) => setLine(line.key, { quantity: e.target.value })} />
                  <select className={inputClass} value={line.unit} disabled={!ingredient}
                    onChange={(e) => setLine(line.key, { unit: e.target.value as Unit })}>
                    {units.map((u) => (
                      <option key={u} value={u}>{UNIT[u]}</option>
                    ))}
                  </select>
                  <select className={inputClass} value={line.scalingMode} title={SCALING[line.scalingMode].hint}
                    onChange={(e) => setLine(line.key, { scalingMode: e.target.value as ScalingMode })}>
                    {(Object.keys(SCALING) as ScalingMode[]).map((m) => (
                      <option key={m} value={m} title={SCALING[m].hint}>{SCALING[m].label}</option>
                    ))}
                  </select>
                  <button type="button" aria-label="Treu l'ingredient" title="Treu"
                    className="text-stone-400 hover:text-terracotta-700 disabled:opacity-30"
                    disabled={draft.lines.length === 1}
                    onClick={() => set('lines', draft.lines.filter((l) => l.key !== line.key))}>
                    ✕
                  </button>
                </div>
              )
            })}
            <Button variant="ghost" className="px-2 py-1" onClick={() =>
              set('lines', [...draft.lines, { key: nextKey++, ingredientId: null, quantity: '', unit: 'G', scalingMode: 'LINEAR' }])}>
              + Afegeix un ingredient
            </Button>
          </Card>

          <Card className="space-y-3">
            <h2 className="font-semibold">Preparació</h2>
            {draft.steps.map((step, i) => (
              <div key={i} className="flex items-start gap-2">
                <span className="mt-2 w-6 text-right text-sm text-stone-400">{i + 1}.</span>
                <textarea rows={2} className={`${inputClass} flex-1`} value={step}
                  onChange={(e) => set('steps', draft.steps.map((s, j) => (j === i ? e.target.value : s)))} />
                <div className="flex flex-col">
                  <button type="button" className="text-xs text-stone-400 hover:text-stone-800 disabled:opacity-30" disabled={i === 0} onClick={() => moveStep(i, -1)} aria-label="Puja">▲</button>
                  <button type="button" className="text-xs text-stone-400 hover:text-stone-800 disabled:opacity-30" disabled={i === draft.steps.length - 1} onClick={() => moveStep(i, 1)} aria-label="Baixa">▼</button>
                </div>
                <button type="button" className="mt-2 text-stone-400 hover:text-terracotta-700 disabled:opacity-30" aria-label="Treu el pas"
                  disabled={draft.steps.length === 1} onClick={() => set('steps', draft.steps.filter((_, j) => j !== i))}>
                  ✕
                </button>
              </div>
            ))}
            <Button variant="ghost" className="px-2 py-1" onClick={() => set('steps', [...draft.steps, ''])}>
              + Afegeix un pas
            </Button>
          </Card>
        </div>

        <aside className="sticky top-4 space-y-4">
          <Card className="space-y-3">
            <h2 className="font-semibold">Per ració</h2>
            <p className="text-3xl font-semibold tabular-nums">
              {formatNumber(Math.round(nutrition.kcal))} <span className="text-base font-normal text-stone-500">kcal</span>
            </p>
            <dl className="grid grid-cols-3 gap-2 text-center text-xs">
              <Macro label="Proteïnes" value={nutrition.proteinG} />
              <Macro label="Hidrats" value={nutrition.carbsG} />
              <Macro label="Greixos" value={nutrition.fatG} />
            </dl>
            <div className="border-t border-stone-100 pt-3 text-sm">
              <p className="text-xs font-medium text-stone-500">Apte per a</p>
              <p>{diets.length > 0 ? diets.map((d) => DIET[d].toLowerCase()).join(', ') : '—'}</p>
            </div>
            <div className="text-sm">
              <p className="text-xs font-medium text-stone-500">Al·lèrgens</p>
              <p className={allergens.length > 0 ? 'text-terracotta-700' : ''}>
                {allergens.length > 0 ? allergens.map((a) => ALLERGEN[a].toLowerCase()).join(', ') : 'cap'}
              </p>
            </div>
          </Card>

          {problems.length > 0 && (
            <ul className="space-y-1 text-xs text-stone-500">
              {problems.map((p) => (
                <li key={p}>· {p}</li>
              ))}
            </ul>
          )}
          <ErrorBox error={save.error} />
          <Button className="w-full" disabled={problems.length > 0 || save.isPending} onClick={submit}>
            {save.isPending ? 'Desant…' : recipeId === null ? 'Crea el plat' : 'Desa els canvis'}
          </Button>
        </aside>
      </div>

      {creating && (
        <IngredientModal
          initialName={creating.name}
          onClose={() => setCreating(null)}
          onSaved={(ingredient) => {
            chooseIngredient(creating.lineKey, ingredient)
            setCreating(null)
          }}
        />
      )}
      {editingIngredient && (
        <IngredientModal initial={editingIngredient} onClose={() => setEditingIngredient(null)} onSaved={() => setEditingIngredient(null)} />
      )}
    </div>
  )
}

function IngredientPicker({
  ingredients,
  selected,
  onSelect,
  onCreate,
  onEdit,
}: {
  ingredients: Ingredient[]
  selected?: Ingredient
  onSelect: (ingredient: Ingredient) => void
  onCreate: (name: string) => void
  onEdit?: () => void
}) {
  const [query, setQuery] = useState<string | null>(null)
  const q = fold(query ?? '')
  const matches = q ? ingredients.filter((i) => fold(i.name).includes(q)).slice(0, 8) : []
  const exact = ingredients.some((i) => fold(i.name) === q)

  return (
    <div className="relative">
      <div className="flex items-center gap-1">
        <input
          className={inputClass}
          placeholder="Cerca un ingredient…"
          value={query ?? selected?.name ?? ''}
          onFocus={() => setQuery(query ?? '')}
          onBlur={() => window.setTimeout(() => setQuery(null), 150)}
          onChange={(e) => setQuery(e.target.value)}
        />
        {onEdit && query === null && (
          <button type="button" className="shrink-0 text-xs text-brand-700 hover:underline" onClick={onEdit} title="Edita aquest ingredient propi">
            edita
          </button>
        )}
      </div>
      {query !== null && q && (
        <ul className="absolute z-20 mt-1 w-full overflow-hidden rounded-lg bg-white shadow-lg ring-1 ring-stone-200">
          {matches.map((i) => (
            <li key={i.id}>
              <button type="button" className="w-full px-3 py-2 text-left text-sm hover:bg-brand-50"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => {
                  onSelect(i)
                  setQuery(null)
                }}>
                {i.name}
                {i.custom && <span className="ml-1 text-xs text-avocado-700">· propi</span>}
              </button>
            </li>
          ))}
          {!exact && (
            <li>
              <button type="button" className="w-full border-t border-stone-100 px-3 py-2 text-left text-sm font-medium text-brand-700 hover:bg-brand-50"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => {
                  onCreate((query ?? '').trim())
                  setQuery(null)
                }}>
                + Crea «{(query ?? '').trim()}» com a ingredient nou
              </button>
            </li>
          )}
        </ul>
      )}
    </div>
  )
}

function Toggle({ on, onClick, children }: { on: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button type="button" onClick={onClick} aria-pressed={on}
      className={`inline-flex items-center gap-1 rounded-lg px-3 py-1.5 text-sm ring-1 ${
        on ? 'bg-brand-600 text-white ring-brand-600' : 'bg-white text-stone-700 ring-stone-300 hover:bg-stone-50'
      }`}>
      {children}
    </button>
  )
}

function Macro({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg bg-stone-50 py-2">
      <dd className="font-semibold tabular-nums text-stone-800">{Math.round(value)} g</dd>
      <dt className="text-stone-500">{label}</dt>
    </div>
  )
}

function fold(text: string): string {
  return text.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim()
}

const NEW_PROTEIN = '__new__'

/** Protein type select, with an inline way to add a type of the family's own. */
function ProteinField({ value, onChange }: { value: string; onChange: (code: string) => void }) {
  const proteins = useProteins()
  const save = useSaveProtein()
  const [adding, setAdding] = useState(false)
  const [name, setName] = useState('')

  if (adding) {
    return (
      <Field label="Proteïna principal" hint="Nou tipus de proteïna, p. ex. «Tofu i seitan»">
        <div className="flex gap-2">
          <input className={inputClass} autoFocus value={name} placeholder="Nom" onChange={(e) => setName(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Escape') setAdding(false)
            }} />
          <Button disabled={!name.trim() || save.isPending}
            onClick={() => save.mutate({ name: name.trim() }, {
              onSuccess: (protein) => {
                onChange(protein.code)
                setAdding(false)
                setName('')
              },
            })}>
            Crea
          </Button>
          <Button variant="ghost" onClick={() => setAdding(false)}>
            Cancel·la
          </Button>
        </div>
        {save.error ? <ErrorBox error={save.error} /> : null}
      </Field>
    )
  }
  return (
    <Field label="Proteïna principal" hint="Per variar el menú d'un dia a l'altre">
      <select className={inputClass} value={value}
        onChange={(e) => (e.target.value === NEW_PROTEIN ? setAdding(true) : onChange(e.target.value))}>
        {(proteins.data ?? []).map((p) => (
          <option key={p.code} value={p.code}>{p.name}</option>
        ))}
        <option value={NEW_PROTEIN}>+ Nou tipus de proteïna…</option>
      </select>
    </Field>
  )
}
