import { useEffect, useMemo, useState } from 'react'
import { useCopyWeek, useWeeks } from '../api/hooks'
import { addDays, formatWeekRange, toIso } from '../labels'
import { navigate } from '../router'
import { Button, ErrorBox } from '../ui'

/** Mondays from three weeks ago to eight weeks ahead: where a menu is usually copied to. */
function candidateMondays(): string[] {
  const today = new Date()
  const offset = (today.getDay() + 6) % 7
  const thisMonday = toIso(new Date(today.getFullYear(), today.getMonth(), today.getDate() - offset))
  return Array.from({ length: 12 }, (_, i) => addDays(thisMonday, 7 * (i - 3)))
}

/** Copies a week's menu (dishes, diners, extra servings) onto another week. */
export function CopyWeekDialog({ sourceWeekId, sourceWeekStart, onClose }: { sourceWeekId: number; sourceWeekStart: string; onClose: () => void }) {
  const weeks = useWeeks()
  const copy = useCopyWeek(sourceWeekId)
  const byStart = useMemo(() => new Map((weeks.data ?? []).map((w) => [w.weekStart, w])), [weeks.data])
  const options = useMemo(() => candidateMondays().filter((m) => m !== sourceWeekStart), [sourceWeekStart])
  const firstFree = options.find((m) => m > sourceWeekStart && !byStart.has(m)) ?? options.find((m) => byStart.get(m)?.status !== 'CONFIRMED')
  const [target, setTarget] = useState<string | undefined>(undefined)
  const selected = target ?? firstFree
  const existing = selected ? byStart.get(selected) : undefined

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  const submit = () => {
    if (!selected) return
    if (existing && existing.mealCount > 0 &&
      !window.confirm(`La setmana del ${formatWeekRange(selected)} ja té un menú. Voleu substituir-lo per aquest?`)) {
      return
    }
    copy.mutate(selected, { onSuccess: (week) => navigate({ screen: 'menu', weekId: week.id }) })
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-stone-900/40 p-8" onClick={onClose}>
      <div role="dialog" aria-modal="true" aria-label="Copia el menú"
        className="w-full max-w-xl overflow-hidden rounded-2xl bg-white shadow-2xl" onClick={(e) => e.stopPropagation()}>
        <div className="border-b border-stone-200 px-6 py-4">
          <h2 className="text-lg font-semibold">Copia el menú</h2>
          <p className="text-sm text-stone-500">
            Del {formatWeekRange(sourceWeekStart)} a una altra setmana: els mateixos plats, la mateixa gent i les mateixes carmanyoles.
          </p>
        </div>
        <div className="grid grid-cols-2 gap-2 px-6 py-4">
          {options.map((monday) => {
            const week = byStart.get(monday)
            const confirmed = week?.status === 'CONFIRMED'
            return (
              <button key={monday} type="button" disabled={confirmed} onClick={() => setTarget(monday)}
                className={`rounded-lg px-3 py-2 text-left text-sm ring-1 disabled:cursor-not-allowed disabled:opacity-40 ${
                  monday === selected ? 'bg-brand-600 text-white ring-brand-600' : 'bg-white text-stone-700 ring-stone-300 hover:bg-stone-50'
                }`}>
                <span className="block font-medium">{formatWeekRange(monday)}</span>
                <span className="block text-xs opacity-75">
                  {confirmed ? 'confirmada' : week ? 'ja té menú · se substituirà' : 'nova'}
                </span>
              </button>
            )
          })}
        </div>
        <div className="space-y-3 border-t border-stone-200 px-6 py-4">
          <ErrorBox error={copy.error} />
          <div className="flex justify-end gap-2">
            <Button variant="ghost" onClick={onClose}>Cancel·la</Button>
            <Button disabled={!selected || copy.isPending} onClick={submit}>
              {copy.isPending ? 'Copiant…' : 'Copia el menú →'}
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}
