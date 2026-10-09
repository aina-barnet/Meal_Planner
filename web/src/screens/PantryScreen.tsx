import { useMemo, useState } from 'react'
import { useClearPantry, useIngredients, usePantry, useSetPantry } from '../api/hooks'
import type { Ingredient, PantryItem } from '../api/types'
import { AISLE } from '../labels'
import { formatQuantity } from '../quantities'
import { Button, Card, ErrorBox, Spinner, inputClass } from '../ui'

/** What is already at home. Confirming a week updates it automatically. */
export function PantryScreen() {
  const pantry = usePantry()
  const ingredients = useIngredients()
  const set = useSetPantry()
  const clear = useClearPantry()
  const [query, setQuery] = useState('')
  const [adding, setAdding] = useState<Ingredient | null>(null)
  const [quantity, setQuantity] = useState('')

  const inPantry = useMemo(() => new Set((pantry.data ?? []).map((p) => p.ingredientId)), [pantry.data])
  const matches = useMemo(() => {
    const q = fold(query)
    if (!q) return []
    return (ingredients.data ?? [])
      .filter((i) => !i.pantryStaple && !inPantry.has(i.id) && fold(i.name).includes(q))
      .slice(0, 8)
  }, [ingredients.data, query, inPantry])

  if (pantry.isPending) return <Spinner />

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <header className="space-y-1">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-semibold">Rebost</h1>
          <Button
            variant="danger"
            disabled={(pantry.data ?? []).length === 0 || clear.isPending}
            onClick={() => {
              if (window.confirm('Segur que voleu buidar el rebost? S\'esborraran tots els productes apuntats.')) clear.mutate()
            }}
          >
            {clear.isPending ? 'Buidant…' : 'Buida el rebost'}
          </Button>
        </div>
        <p className="mt-1 text-stone-600">
          El que ja teniu a casa es descompta de la llista de la compra. En confirmar una setmana, el rebost s'actualitza
          sol amb el que sobri dels paquets. L'oli i les espècies es donen sempre per tinguts.
        </p>
      </header>

      <Card className="space-y-3">
        <h2 className="font-semibold">Afegeix un producte</h2>
        <div className="flex gap-2">
          <div className="relative flex-1">
            <input
              className={inputClass}
              placeholder="Cerca un ingredient…"
              value={adding ? adding.name : query}
              onChange={(e) => {
                setAdding(null)
                setQuery(e.target.value)
              }}
            />
            {!adding && matches.length > 0 && (
              <ul className="absolute z-10 mt-1 w-full overflow-hidden rounded-lg bg-white shadow-lg ring-1 ring-stone-200">
                {matches.map((i) => (
                  <li key={i.id}>
                    <button
                      type="button"
                      className="w-full px-3 py-2 text-left text-sm hover:bg-brand-50"
                      onClick={() => setAdding(i)}
                    >
                      {i.name}
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
          <input
            type="number"
            min={0}
            className={`${inputClass} w-32! shrink-0`}
            placeholder={adding?.canonicalUnit === 'ML' ? 'ml' : 'g'}
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
          />
          <Button
            disabled={!adding || !(Number(quantity) > 0) || set.isPending}
            onClick={() =>
              adding &&
              set.mutate(
                { ingredientId: adding.id, quantity: Number(quantity) },
                {
                  onSuccess: () => {
                    setAdding(null)
                    setQuery('')
                    setQuantity('')
                  },
                },
              )
            }
          >
            Afegeix
          </Button>
        </div>
        <ErrorBox error={set.error ?? clear.error} />
      </Card>

      <div className="overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-stone-200">
        {(pantry.data ?? []).length === 0 ? (
          <p className="p-8 text-center text-sm text-stone-500">El rebost és buit.</p>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-stone-50 text-left text-stone-600">
              <tr>
                <th className="px-5 py-3 font-medium">Producte</th>
                <th className="px-5 py-3 font-medium">Passadís</th>
                <th className="px-5 py-3 font-medium">Quantitat</th>
                <th className="px-5 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-100">
              {(pantry.data ?? []).map((item) => (
                <PantryRow key={item.ingredientId} item={item} />
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}

function PantryRow({ item }: { item: PantryItem }) {
  const set = useSetPantry()
  const [value, setValue] = useState(String(item.quantity))
  const changed = Number(value) !== item.quantity && Number(value) >= 0 && value.trim() !== ''
  return (
    <tr>
      <td className="px-5 py-3 font-medium">{item.name}</td>
      <td className="px-5 py-3 text-stone-500">{AISLE[item.aisle]}</td>
      <td className="px-5 py-3">
        <div className="flex items-center gap-2">
          <input
            type="number"
            min={0}
            className={`${inputClass} w-28! shrink-0`}
            value={value}
            onChange={(e) => setValue(e.target.value)}
          />
          <span className="text-stone-500">{item.canonicalUnit === 'ML' ? 'ml' : 'g'}</span>
          <span className="text-xs text-stone-400">({formatQuantity(item.quantity, item.canonicalUnit)})</span>
        </div>
      </td>
      <td className="px-5 py-3 text-right">
        {changed && (
          <Button variant="secondary" className="px-2 py-1" onClick={() => set.mutate({ ingredientId: item.ingredientId, quantity: Number(value) })}>
            Desa
          </Button>
        )}
        <Button variant="danger" className="px-2 py-1" onClick={() => set.mutate({ ingredientId: item.ingredientId, quantity: 0 })}>
          Treu
        </Button>
      </td>
    </tr>
  )
}

function fold(text: string): string {
  return text.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim()
}
