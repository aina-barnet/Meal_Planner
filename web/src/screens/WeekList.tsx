import { useMemo, useState } from 'react'
import { useCreateWeek, useDeleteWeek, useWeeks } from '../api/hooks'
import { addDays, formatWeekRange, toIso } from '../labels'
import { navigate } from '../router'
import { Button, Card, ErrorBox, Spinner } from '../ui'
import { CopyWeekDialog } from './CopyWeekDialog'

/** The next Mondays a family might plan for, starting with this week's. */
function upcomingMondays(count: number): string[] {
  const today = new Date()
  const offset = (today.getDay() + 6) % 7 // days since Monday
  const thisMonday = toIso(new Date(today.getFullYear(), today.getMonth(), today.getDate() - offset))
  return Array.from({ length: count }, (_, i) => addDays(thisMonday, 7 * i))
}

export function WeekList() {
  const weeks = useWeeks()
  const create = useCreateWeek()
  const remove = useDeleteWeek()
  const taken = useMemo(() => new Set((weeks.data ?? []).map((w) => w.weekStart)), [weeks.data])
  const options = useMemo(() => upcomingMondays(6).filter((m) => !taken.has(m)), [taken])
  const [chosen, setChosen] = useState<string | null>(null)
  const [copying, setCopying] = useState<{ id: number; weekStart: string } | null>(null)
  const selected = chosen && options.includes(chosen) ? chosen : options[0]

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <header>
        <h1 className="text-2xl font-semibold">Setmanes</h1>
        <p className="mt-1 text-stone-600">Trieu una setmana per configurar qui menja a cada àpat.</p>
      </header>

      <Card>
        <h2 className="text-lg font-semibold">Nova setmana</h2>
        {options.length === 0 ? (
          <p className="mt-2 text-sm text-stone-500">Ja teniu planificades les properes setmanes.</p>
        ) : (
          <div className="mt-4 flex flex-wrap items-center gap-2">
            {options.map((monday) => (
              <button
                key={monday}
                type="button"
                onClick={() => setChosen(monday)}
                className={`rounded-lg px-4 py-2 text-sm ring-1 ${
                  monday === selected
                    ? 'bg-brand-600 text-white ring-brand-600'
                    : 'bg-white text-stone-700 ring-stone-300 hover:bg-stone-50'
                }`}
              >
                {formatWeekRange(monday)}
              </button>
            ))}
            <Button
              className="ml-auto"
              disabled={!selected || create.isPending}
              onClick={() =>
                selected &&
                create.mutate(selected, { onSuccess: (week) => navigate({ screen: 'week', weekId: week.id }) })
              }
            >
              Crea i configura →
            </Button>
          </div>
        )}
        <div className="mt-3">
          <ErrorBox error={create.error} />
        </div>
      </Card>

      {weeks.isPending ? (
        <Spinner />
      ) : (
        <div className="grid grid-cols-2 gap-4">
          {(weeks.data ?? []).map((week) => (
            <Card key={week.id} className="flex flex-col gap-3">
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-lg font-semibold">{formatWeekRange(week.weekStart)}</p>
                  <p className="text-sm text-stone-500">
                    {week.mealCount} àpats · {week.assignedCount} amb plat
                  </p>
                </div>
                <span
                  className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${
                    week.status === 'CONFIRMED' ? 'bg-avocado-100 text-avocado-800' : 'bg-stone-100 text-stone-600'
                  }`}
                >
                  {week.status === 'CONFIRMED' ? 'Confirmada' : 'Esborrany'}
                </span>
              </div>
              <div className="mt-auto flex gap-2">
                <Button variant="secondary" onClick={() => navigate({ screen: 'week', weekId: week.id })}>
                  Configuració
                </Button>
                <Button variant="secondary" onClick={() => navigate({ screen: 'menu', weekId: week.id })}>
                  Menú
                </Button>
                <Button
                  variant="ghost"
                  disabled={week.assignedCount === 0}
                  title={week.assignedCount === 0 ? 'Aquesta setmana encara no té plats' : 'Copia aquest menú a una altra setmana'}
                  onClick={() => setCopying({ id: week.id, weekStart: week.weekStart })}
                >
                  Copia…
                </Button>
                {week.status === 'DRAFT' && (
                  <Button
                    variant="danger"
                    className="ml-auto"
                    onClick={() => {
                      if (window.confirm('Segur que voleu esborrar aquesta setmana?')) remove.mutate(week.id)
                    }}
                  >
                    Esborra
                  </Button>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}
      {copying && (
        <CopyWeekDialog sourceWeekId={copying.id} sourceWeekStart={copying.weekStart} onClose={() => setCopying(null)} />
      )}
    </div>
  )
}
