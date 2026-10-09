import { useCheckItem, useMealActions, useShoppingList, useWeek } from '../api/hooks'
import type { ShoppingItem } from '../api/types'
import { AISLE, formatWeekRange } from '../labels'
import { formatPackages, formatQuantity } from '../quantities'
import { PrintButton } from '../print/PrintButton'
import { confirmReopen } from '../reopen'
import { navigate } from '../router'
import { Button, Card, ErrorBox, Spinner } from '../ui'

export function ShoppingListScreen({ weekId }: { weekId: number }) {
  const week = useWeek(weekId)
  const confirmed = week.data?.status === 'CONFIRMED'
  const list = useShoppingList(weekId, confirmed)
  const check = useCheckItem(weekId)
  const { reopen } = useMealActions(weekId)

  if (week.isPending) return <Spinner />
  if (week.error) return <ErrorBox error={week.error} />
  if (!confirmed) {
    return (
      <Card className="mx-auto max-w-xl text-center">
        <h1 className="text-xl font-semibold">Encara no hi ha llista</h1>
        <p className="mt-2 text-stone-600">
          La llista de la compra es genera quan confirmeu el menú de la setmana.
        </p>
        <Button className="mt-4" onClick={() => navigate({ screen: 'menu', weekId })}>
          Ves al menú
        </Button>
      </Card>
    )
  }
  if (list.isPending) return <Spinner />
  if (list.error || !list.data) return <ErrorBox error={list.error} />

  const items = list.data.aisles.flatMap((a) => a.items)
  const toBuy = items.filter((i) => !i.coveredByPantry)
  const done = toBuy.filter((i) => i.checked).length

  return (
    <div className="shopping mx-auto max-w-4xl space-y-6">
      <header className="flex items-end justify-between">
        <div>
          <p className="text-sm text-stone-500">Llista de la compra</p>
          <h1 className="text-2xl font-semibold">{formatWeekRange(week.data!.weekStart)}</h1>
          <p className="mt-1 text-sm text-stone-600 print:hidden">
            {done} de {toBuy.length} productes al carro
          </p>
        </div>
        <div className="flex gap-2 print:hidden">
          <Button
            variant="secondary"
            disabled={reopen.isPending}
            onClick={() =>
              confirmReopen() && reopen.mutate(undefined, { onSuccess: () => navigate({ screen: 'menu', weekId }) })
            }
          >
            {reopen.isPending ? 'Tornant…' : '↩ Torna a esborrany'}
          </Button>
          <PrintButton weekId={weekId} variant="primary" />
        </div>
      </header>
      <div className="print:hidden">
        <ErrorBox error={reopen.error} />
      </div>

      <div className="columns-2 gap-6 print:columns-2">
        {list.data.aisles.map(({ aisle, items: aisleItems }) => (
          <section key={aisle} className="mb-6 break-inside-avoid rounded-2xl bg-white p-5 shadow-sm ring-1 ring-stone-200 print:mb-4 print:rounded-none print:p-0 print:shadow-none print:ring-0">
            <h2 className="mb-2 border-b border-stone-200 pb-1 text-sm font-semibold uppercase tracking-wide text-brand-800 print:text-black">
              {AISLE[aisle]}
            </h2>
            <ul className="space-y-1">
              {aisleItems.map((item) => (
                <Item key={item.id} item={item} onToggle={() => check.mutate({ itemId: item.id, checked: !item.checked })} />
              ))}
            </ul>
          </section>
        ))}
      </div>

      {list.data.staples.length > 0 && (
        <p className="text-sm text-stone-500 print:text-black">
          <strong className="font-medium text-stone-700 print:text-black">Bàsics que es donen per tinguts a casa:</strong>{' '}
          {list.data.staples.join(', ')}.
        </p>
      )}
      <p className="text-xs text-stone-400 print:hidden">
        El que sobri dels paquets queda apuntat al rebost per a la setmana següent.
      </p>
    </div>
  )
}

function Item({ item, onToggle }: { item: ShoppingItem; onToggle: () => void }) {
  if (item.coveredByPantry) {
    return (
      <li className="flex items-baseline gap-3 py-1 text-sm text-stone-400 print:text-stone-600">
        <span className="w-4 text-center">✓</span>
        <span className="flex-1 line-through decoration-stone-300">{item.name}</span>
        <span className="text-xs">ja ho teniu ({formatQuantity(item.required, item.canonicalUnit)})</span>
      </li>
    )
  }
  return (
    <li>
      <label className="flex cursor-pointer items-baseline gap-3 rounded-md py-1 text-sm hover:bg-stone-50 print:cursor-auto">
        <input
          type="checkbox"
          checked={item.checked}
          onChange={onToggle}
          className="size-4 translate-y-0.5 accent-brand-700 print:hidden"
        />
        <span className="hidden size-3.5 translate-y-0.5 border border-black print:inline-block" aria-hidden />
        <span className={`flex-1 ${item.checked ? 'text-stone-400 line-through' : 'text-stone-900'} print:text-black print:no-underline`}>
          {item.name}
          <span className="block text-xs text-stone-500 print:text-stone-700">
            cal {formatQuantity(item.required, item.canonicalUnit)}
            {item.fromPantry > 0 && ` · ${formatQuantity(item.fromPantry, item.canonicalUnit)} del rebost`}
            {item.leftover > 0 && ` · en sobraran ${formatQuantity(item.leftover, item.canonicalUnit)}`}
          </span>
        </span>
        <span className="font-medium tabular-nums text-stone-800 print:text-black">
          {formatPackages(item.packages, item.packageSize, item.canonicalUnit)}
        </span>
      </label>
    </li>
  )
}
