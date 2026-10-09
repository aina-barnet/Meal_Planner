import { useHousehold } from '../api/hooks'
import type { ShoppingList, Week } from '../api/types'
import { AISLE, formatWeekRange } from '../labels'
import { formatPackages } from '../quantities'

/**
 * The shopping list as it goes to the shop: one line per product to buy, three columns, aisle by
 * aisle. What the pantry already covers is summarised on one line at the end.
 */
export function ShoppingSheet({ week, list }: { week: Week; list: ShoppingList }) {
  const household = useHousehold()
  const aisles = list.aisles
    .map((a) => ({ ...a, items: a.items.filter((i) => !i.coveredByPantry) }))
    .filter((a) => a.items.length > 0)
  const covered = list.aisles.flatMap((a) => a.items.filter((i) => i.coveredByPantry).map((i) => i.name))
  const count = aisles.reduce((n, a) => n + a.items.length, 0)

  return (
    <div className="text-black" style={{ fontSize: '9.5pt', lineHeight: 1.25 }}>
      <header className="mb-[3mm] flex items-baseline justify-between border-b-2 border-black pb-[1.5mm]">
        <h1 style={{ fontSize: '15pt' }} className="font-semibold">
          Llista de la compra · {formatWeekRange(week.weekStart)}
        </h1>
        <span style={{ fontSize: '9pt' }}>
          {household.data?.name} · {count} productes
        </span>
      </header>

      <div style={{ columnCount: 3, columnGap: '6mm' }}>
        {aisles.map(({ aisle, items }) => (
          <section key={aisle} className="mb-[3mm]" style={{ breakInside: 'avoid' }}>
            <h2 className="mb-[1mm] border-b border-black font-semibold uppercase" style={{ fontSize: '8.5pt', letterSpacing: '0.04em' }}>
              {AISLE[aisle]}
            </h2>
            <ul>
              {items.map((item) => (
                <li key={item.id} className="flex items-baseline gap-[1.5mm] py-[0.4mm]">
                  <span
                    aria-hidden
                    className="inline-block shrink-0 border border-black text-center"
                    style={{ width: '3mm', height: '3mm', fontSize: '7pt', lineHeight: '3mm' }}
                  >
                    {item.checked ? '✓' : ''}
                  </span>
                  <span className="min-w-0 flex-1">{item.name}</span>
                  <span className="shrink-0 tabular-nums">{formatPackages(item.packages, item.packageSize, item.canonicalUnit)}</span>
                </li>
              ))}
            </ul>
          </section>
        ))}
      </div>

      {(covered.length > 0 || list.staples.length > 0) && (
        <footer className="mt-[2mm] space-y-[1mm] border-t border-black pt-[1.5mm]" style={{ fontSize: '8pt' }}>
          {covered.length > 0 && (
            <p>
              <strong>Ja ho teniu al rebost:</strong> {covered.join(', ')}.
            </p>
          )}
          {list.staples.length > 0 && (
            <p>
              <strong>Bàsics que es donen per tinguts:</strong> {list.staples.join(', ')}.
            </p>
          )}
        </footer>
      )}
    </div>
  )
}
