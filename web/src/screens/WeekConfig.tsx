import { useMemo, useState } from 'react'
import { useConfigureWeek, useMembers, useWeek } from '../api/hooks'
import type { DinerLocation, MealConfigInput, MealSlot, Member, Week } from '../api/types'
import { SLOT, addDays, formatDayMonth, formatWeekRange, formatWeekday } from '../labels'
import { navigate } from '../router'
import { Button, Card, ErrorBox, HomeIcon, LunchboxIcon, Spinner } from '../ui'

type Presence = DinerLocation | 'OUT'

interface Cell {
  planned: boolean
  presence: Record<number, Presence>
  extraServings: number
}

type Draft = Record<string, Cell>

const SLOTS: MealSlot[] = ['LUNCH', 'DINNER']
const key = (date: string, slot: MealSlot) => `${date}|${slot}`

function initialDraft(week: Week, members: Member[]): Draft {
  const draft: Draft = {}
  for (let day = 0; day < 7; day++) {
    const date = addDays(week.weekStart, day)
    for (const slot of SLOTS) {
      const meal = week.meals.find((m) => m.date === date && m.slot === slot)
      const presence: Record<number, Presence> = {}
      for (const member of members) {
        const attendee = meal?.attendees.find((a) => a.memberId === member.id)
        presence[member.id] = meal ? (attendee?.location ?? 'OUT') : 'HOME'
      }
      draft[key(date, slot)] = { planned: meal !== undefined, presence, extraServings: meal?.extraServings ?? 0 }
    }
  }
  return draft
}

function toConfig(week: Week, draft: Draft): MealConfigInput[] {
  const meals: MealConfigInput[] = []
  for (let day = 0; day < 7; day++) {
    const date = addDays(week.weekStart, day)
    for (const slot of SLOTS) {
      const cell = draft[key(date, slot)]
      if (!cell.planned) continue
      const attendees = Object.entries(cell.presence)
        .filter(([, p]) => p !== 'OUT')
        .map(([memberId, p]) => ({ memberId: Number(memberId), location: p as DinerLocation }))
      meals.push({ date, slot, attendees, extraServings: cell.extraServings })
    }
  }
  return meals
}

export function WeekConfig({ weekId }: { weekId: number }) {
  const week = useWeek(weekId)
  const members = useMembers()
  if (week.isPending || members.isPending) return <Spinner />
  if (week.error) return <ErrorBox error={week.error} />
  if (!week.data || !members.data) return null
  // Re-mount the editor when the server state changes identity (e.g. after saving).
  return <WeekEditor key={week.dataUpdatedAt} week={week.data} members={members.data} />
}

function WeekEditor({ week, members }: { week: Week; members: Member[] }) {
  const [draft, setDraft] = useState<Draft>(() => initialDraft(week, members))
  const [dirty, setDirty] = useState(false)
  const configure = useConfigureWeek(week.id)
  const readOnly = week.status === 'CONFIRMED'
  const days = useMemo(() => Array.from({ length: 7 }, (_, i) => addDays(week.weekStart, i)), [week.weekStart])

  const update = (k: string, change: (cell: Cell) => Cell) => {
    setDraft((d) => ({ ...d, [k]: change(d[k]) }))
    setDirty(true)
  }

  const applyToWeekdayLunches = (memberId: number, presence: Presence) => {
    setDraft((d) => {
      const next = { ...d }
      for (const date of days.slice(0, 5)) {
        const k = key(date, 'LUNCH')
        next[k] = { ...next[k], planned: true, presence: { ...next[k].presence, [memberId]: presence } }
      }
      return next
    })
    setDirty(true)
  }

  const setSlotPlanned = (slot: MealSlot, planned: boolean) => {
    setDraft((d) => {
      const next = { ...d }
      for (const date of days) next[key(date, slot)] = { ...next[key(date, slot)], planned }
      return next
    })
    setDirty(true)
  }

  const planned = Object.values(draft).filter((c) => c.planned)
  const empty = planned.filter((c) => Object.values(c.presence).every((p) => p === 'OUT'))
  const lunchboxes = planned.reduce(
    (sum, c) => sum + Object.values(c.presence).filter((p) => p === 'TAKEAWAY').length,
    0,
  )
  const removed = week.meals.filter((m) => m.emptyReason)

  return (
    <div className="space-y-6 pb-24">
      <header className="flex items-end justify-between">
        <div>
          <p className="text-sm text-stone-500">Configuració de la setmana</p>
          <h1 className="text-2xl font-semibold">{formatWeekRange(week.weekStart)}</h1>
        </div>
        {readOnly && (
          <span className="rounded-full bg-brand-100 px-3 py-1 text-sm font-medium text-brand-800">
            Confirmada · només lectura
          </span>
        )}
      </header>

      {!readOnly && (
        <Card className="space-y-4">
          <h2 className="font-semibold">Patrons ràpids</h2>
          <div className="grid grid-cols-2 gap-x-8 gap-y-3">
            {members.map((member) => (
              <div key={member.id} className="flex items-center gap-2 text-sm">
                <span className="w-24 font-medium">{member.name}</span>
                <span className="text-stone-500">dinars de dl a dv:</span>
                <Button variant="secondary" className="px-2 py-1" onClick={() => applyToWeekdayLunches(member.id, 'HOME')}>
                  <HomeIcon /> a casa
                </Button>
                <Button
                  variant="secondary"
                  className="px-2 py-1"
                  onClick={() => applyToWeekdayLunches(member.id, 'TAKEAWAY')}
                >
                  <LunchboxIcon /> carmanyola
                </Button>
                <Button variant="secondary" className="px-2 py-1" onClick={() => applyToWeekdayLunches(member.id, 'OUT')}>
                  no hi és
                </Button>
              </div>
            ))}
          </div>
          <div className="flex gap-2 border-t border-stone-100 pt-4 text-sm">
            <Button variant="ghost" className="px-2 py-1" onClick={() => setSlotPlanned('LUNCH', true)}>
              Planifica tots els dinars
            </Button>
            <Button variant="ghost" className="px-2 py-1" onClick={() => setSlotPlanned('LUNCH', false)}>
              Cap dinar
            </Button>
            <Button variant="ghost" className="px-2 py-1" onClick={() => setSlotPlanned('DINNER', true)}>
              Planifica tots els sopars
            </Button>
            <Button variant="ghost" className="px-2 py-1" onClick={() => setSlotPlanned('DINNER', false)}>
              Cap sopar
            </Button>
          </div>
        </Card>
      )}

      {removed.length > 0 && (
        <div className="rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-900 ring-1 ring-amber-200">
          {removed.map((m) => (
            <p key={m.id}>{m.emptyReason}</p>
          ))}
        </div>
      )}

      <div className="grid grid-cols-[5rem_repeat(7,minmax(0,1fr))] gap-2">
        <div />
        {days.map((date) => (
          <div key={date} className="px-1 pb-1">
            <p className="font-semibold">{formatWeekday(date)}</p>
            <p className="text-xs text-stone-500">{formatDayMonth(date)}</p>
          </div>
        ))}
        {SLOTS.map((slot) => (
          <Row
            key={slot}
            slot={slot}
            days={days}
            draft={draft}
            members={members}
            readOnly={readOnly}
            onChange={update}
          />
        ))}
      </div>

      {!readOnly && (
        <div className="fixed inset-x-0 bottom-0 border-t border-stone-200 bg-white/95 backdrop-blur">
          <div className="mx-auto flex max-w-7xl items-center gap-6 px-8 py-3">
            <p className="text-sm text-stone-600">
              <strong className="text-stone-900">{planned.length}</strong> àpats ·{' '}
              <strong className="text-stone-900">{lunchboxes}</strong> carmanyoles
            </p>
            {empty.length > 0 && (
              <p className="text-sm text-terracotta-700">Hi ha {empty.length} àpats sense ningú: traieu-los o afegiu-hi algú.</p>
            )}
            <div className="ml-auto flex items-center gap-3">
              {configure.error ? <ErrorBox error={configure.error} /> : null}
              {!dirty && configure.isSuccess && <span className="text-sm text-avocado-700">Desat ✓</span>}
              <Button
                variant="secondary"
                disabled={dirty}
                title={dirty ? 'Deseu primer els canvis' : undefined}
                onClick={() => navigate({ screen: 'menu', weekId: week.id })}
              >
                Menú →
              </Button>
              <Button
                disabled={!dirty || empty.length > 0 || configure.isPending}
                onClick={() => configure.mutate(toConfig(week, draft), { onSuccess: () => setDirty(false) })}
              >
                {configure.isPending ? 'Desant…' : 'Desa la configuració'}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function Row({
  slot,
  days,
  draft,
  members,
  readOnly,
  onChange,
}: {
  slot: MealSlot
  days: string[]
  draft: Draft
  members: Member[]
  readOnly: boolean
  onChange: (k: string, change: (cell: Cell) => Cell) => void
}) {
  return (
    <>
      <div className="flex items-start pt-3 font-semibold text-stone-700">{SLOT[slot]}</div>
      {days.map((date) => {
        const k = key(date, slot)
        return (
          <MealCell
            key={k}
            cell={draft[k]}
            members={members}
            readOnly={readOnly}
            onChange={(change) => onChange(k, change)}
          />
        )
      })}
    </>
  )
}

function MealCell({
  cell,
  members,
  readOnly,
  onChange,
}: {
  cell: Cell
  members: Member[]
  readOnly: boolean
  onChange: (change: (cell: Cell) => Cell) => void
}) {
  const nobody = Object.values(cell.presence).every((p) => p === 'OUT')

  if (!cell.planned) {
    return (
      <button
        type="button"
        disabled={readOnly}
        onClick={() => onChange((c) => ({ ...c, planned: true }))}
        className="flex min-h-40 items-center justify-center rounded-xl border-2 border-dashed border-stone-300 text-sm text-stone-400 hover:border-brand-400 hover:text-brand-700 disabled:hover:border-stone-300 disabled:hover:text-stone-400"
      >
        {readOnly ? 'No planificat' : '+ Planifica'}
      </button>
    )
  }

  return (
    <div className={`space-y-2 rounded-xl bg-white p-2.5 ring-1 ${nobody ? 'ring-terracotta-300' : 'ring-stone-200'}`}>
      {members.map((member) => (
        <PresenceToggle
          key={member.id}
          name={member.name}
          value={cell.presence[member.id] ?? 'OUT'}
          readOnly={readOnly}
          onChange={(p) => onChange((c) => ({ ...c, presence: { ...c.presence, [member.id]: p } }))}
        />
      ))}
      <div className="flex flex-wrap items-center justify-between gap-1 border-t border-stone-100 pt-2 text-xs text-stone-500">
        <span className="whitespace-nowrap" title="Racions de més per menjar un altre dia">+ racions</span>
        <div className="ml-auto flex items-center gap-1">
          <StepButton
            disabled={readOnly || cell.extraServings === 0}
            onClick={() => onChange((c) => ({ ...c, extraServings: c.extraServings - 1 }))}
          >
            −
          </StepButton>
          <span className="w-4 text-center tabular-nums text-stone-800">{cell.extraServings}</span>
          <StepButton
            disabled={readOnly || cell.extraServings >= 20}
            onClick={() => onChange((c) => ({ ...c, extraServings: c.extraServings + 1 }))}
          >
            +
          </StepButton>
        </div>
      </div>
      {!readOnly && (
        <button
          type="button"
          className="w-full text-center text-xs text-stone-400 hover:text-terracotta-700"
          onClick={() => onChange((c) => ({ ...c, planned: false }))}
        >
          No planificar
        </button>
      )}
    </div>
  )
}

const PRESENCE: { value: Presence; title: string }[] = [
  { value: 'HOME', title: 'Menja a casa' },
  { value: 'TAKEAWAY', title: "S'ho emporta en carmanyola" },
  { value: 'OUT', title: 'No hi és' },
]

function PresenceToggle({
  name,
  value,
  readOnly,
  onChange,
}: {
  name: string
  value: Presence
  readOnly: boolean
  onChange: (p: Presence) => void
}) {
  return (
    // Wraps: when the name and the toggles do not fit side by side (narrow column or large text),
    // the toggles move under the name instead of truncating it.
    <div className="flex flex-wrap items-center justify-between gap-x-1 gap-y-0.5">
      <span className={`text-xs break-words ${value === 'OUT' ? 'text-stone-400 line-through' : 'text-stone-800'}`}>
        {name}
      </span>
      <div className="ml-auto flex shrink-0 overflow-hidden rounded-md ring-1 ring-stone-200">
        {PRESENCE.map((option) => (
          <button
            key={option.value}
            type="button"
            title={option.title}
            aria-label={`${name}: ${option.title}`}
            aria-pressed={value === option.value}
            disabled={readOnly}
            onClick={() => onChange(option.value)}
            className={`flex size-6 items-center justify-center text-xs transition-colors ${
              value === option.value
                ? option.value === 'TAKEAWAY'
                  ? 'bg-terracotta-500 text-white'
                  : option.value === 'HOME'
                    ? 'bg-avocado-600 text-white'
                    : 'bg-stone-400 text-white'
                : 'bg-white text-stone-400 hover:bg-stone-50'
            }`}
          >
            {option.value === 'HOME' ? (
              <HomeIcon className="size-3.5" />
            ) : option.value === 'TAKEAWAY' ? (
              <LunchboxIcon className="size-3.5" />
            ) : (
              '–'
            )}
          </button>
        ))}
      </div>
    </div>
  )
}

function StepButton({
  disabled,
  onClick,
  children,
}: {
  disabled: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      className="flex size-5 items-center justify-center rounded bg-stone-100 text-stone-700 hover:bg-stone-200 disabled:opacity-40"
    >
      {children}
    </button>
  )
}
