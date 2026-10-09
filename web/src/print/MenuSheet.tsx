import { useHousehold } from '../api/hooks'
import type { Meal, MealSlot, Week } from '../api/types'
import { useProteinName } from '../proteins'
import { SLOT, addDays, formatDayMonth, formatWeekRange, formatWeekday } from '../labels'

const SLOTS: MealSlot[] = ['LUNCH', 'DINNER']

/** The week on one landscape page: days across, lunch and dinner down. Sizes in points. */
export function MenuSheet({ week }: { week: Week }) {
  const household = useHousehold()
  const days = Array.from({ length: 7 }, (_, i) => addDays(week.weekStart, i))
  const meal = (date: string, slot: MealSlot) => week.meals.find((m) => m.date === date && m.slot === slot)

  return (
    <div className="text-black" style={{ fontSize: '9pt', lineHeight: 1.3 }}>
      <header className="mb-[4mm] flex items-baseline justify-between border-b-2 border-black pb-[2mm]">
        <h1 style={{ fontSize: '16pt' }} className="font-semibold">
          Menú setmanal · {formatWeekRange(week.weekStart)}
        </h1>
        <span style={{ fontSize: '9pt' }}>{household.data?.name}</span>
      </header>
      <table className="w-full table-fixed border-collapse">
        <thead>
          <tr>
            <th className="w-[16mm]" />
            {days.map((date) => (
              <th key={date} className="border border-black bg-stone-100 px-[1.5mm] py-[1mm] text-left font-semibold">
                {formatWeekday(date)}
                <span className="block font-normal" style={{ fontSize: '8pt' }}>{formatDayMonth(date)}</span>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {SLOTS.map((slot) => (
            <tr key={slot}>
              <th className="border border-black bg-stone-100 px-[1.5mm] text-left align-top font-semibold" style={{ paddingTop: '1.5mm' }}>
                {SLOT[slot]}
              </th>
              {days.map((date) => (
                <td key={date} className="h-[62mm] border border-black px-[1.5mm] py-[1.5mm] align-top">
                  <Cell meal={meal(date, slot)} />
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      <p className="mt-[3mm]" style={{ fontSize: '8pt' }}>
        Casa: comensals a casa · Carmanyola: racions per endur-se · +N: racions de més per a un altre dia.
      </p>
    </div>
  )
}

function Cell({ meal }: { meal?: Meal }) {
  const proteinName = useProteinName()
  if (!meal) return <span className="text-stone-500">—</span>
  if (!meal.recipe) return <span className="italic text-stone-600">Sense plat</span>
  const r = meal.recipe
  return (
    <div>
      <p className="font-semibold" style={{ fontSize: '10pt', lineHeight: 1.2 }}>{r.name}</p>
      <p className="mt-[1mm] text-stone-700" style={{ fontSize: '8pt' }}>
        {proteinName(r.proteinType)} · {r.prepMinutes} min
      </p>
      <p className="mt-[1.5mm]" style={{ fontSize: '8pt' }}>
        {meal.dinersAtHome > 0 && <>Casa {meal.dinersAtHome}</>}
        {meal.dinersAtHome > 0 && meal.dinersTakeaway > 0 && ' · '}
        {meal.dinersTakeaway > 0 && (
          <strong>
            Carmanyola {meal.dinersTakeaway} ({r.lunchboxServing === 'COLD' ? 'fred' : 'escalfar'})
          </strong>
        )}
        {meal.extraServings > 0 && ` · +${meal.extraServings}`}
      </p>
    </div>
  )
}
