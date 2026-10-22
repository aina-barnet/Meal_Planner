import { useHousehold } from '../api/hooks'
import type { RecipeDetail } from '../api/types'
import { ALLERGEN, DIET, UNIT, formatNumber } from '../labels'
import { useProteinName } from '../proteins'

/** One dish on a portrait page: everything needed to cook it. Sizes in points. */
export function RecipeSheet({ recipe }: { recipe: RecipeDetail }) {
  const household = useHousehold()
  const proteinName = useProteinName()
  const s = recipe.summary

  return (
    <div className="text-black" style={{ fontSize: '11pt', lineHeight: 1.4 }}>
      <header className="mb-[5mm] border-b-2 border-black pb-[2mm]">
        <div className="flex items-baseline justify-between gap-[4mm]">
          <h1 style={{ fontSize: '20pt', lineHeight: 1.15 }} className="font-semibold">{s.name}</h1>
          <span style={{ fontSize: '9pt' }}>{household.data?.name}</span>
        </div>
        <p className="mt-[1.5mm]" style={{ fontSize: '10pt' }}>
          {s.baseServings} racions · {s.prepMinutes} min · {proteinName(s.proteinType)} ·{' '}
          {s.portable ? 'Apte per a carmanyola' : 'Per menjar a casa'}
        </p>
        <p style={{ fontSize: '10pt' }}>
          Apte per a dieta: {s.diets.length > 0 ? s.diets.map((d) => DIET[d].toLowerCase()).join(', ') : '—'} ·
          Al·lèrgens: {s.allergens.length > 0 ? s.allergens.map((a) => ALLERGEN[a].toLowerCase()).join(', ') : 'cap'}
        </p>
      </header>

      <div className="grid grid-cols-[65mm_1fr] gap-[8mm]">
        <section>
          <h2 className="mb-[2mm] font-semibold" style={{ fontSize: '13pt' }}>Ingredients</h2>
          <ul className="space-y-[1mm]">
            {recipe.ingredients.map((line) => (
              <li key={line.ingredientId} className="flex gap-[2mm]">
                <span className="w-[22mm] shrink-0 text-right tabular-nums">
                  {formatNumber(line.quantity)} {UNIT[line.unit]}
                </span>
                <span>{line.name}</span>
              </li>
            ))}
          </ul>
        </section>
        <section>
          <h2 className="mb-[2mm] font-semibold" style={{ fontSize: '13pt' }}>Preparació</h2>
          <ol className="list-decimal space-y-[2mm] pl-[5mm]">
            {recipe.steps.map((step, i) => (
              <li key={i}>{step}</li>
            ))}
          </ol>
        </section>
      </div>
    </div>
  )
}
