import {
  DndContext,
  DragOverlay,
  KeyboardSensor,
  PointerSensor,
  pointerWithin,
  useDraggable,
  useDroppable,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragStartEvent,
} from '@dnd-kit/core'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { ApiError } from '../api/client'
import { useMealActions, useMembers, useWeek } from '../api/hooks'
import type { Meal, MealSlot, Member, Week } from '../api/types'
import { SLOT, addDays, formatDayMonth, formatNumber, formatWeekRange, formatWeekday } from '../labels'
import { navigate } from '../router'
import { PrintButton } from '../print/PrintButton'
import { useProteinName } from '../proteins'
import { confirmReopen } from '../reopen'
import { fitLevel, swapViolations } from '../rules'
import { Button, ErrorBox, HomeIcon, LunchboxIcon, Spinner } from '../ui'
import { CopyWeekDialog } from './CopyWeekDialog'
import { RecipePicker } from './RecipePicker'

const SLOTS: MealSlot[] = ['LUNCH', 'DINNER']

export function MenuBoard({ weekId }: { weekId: number }) {
  const week = useWeek(weekId)
  const members = useMembers()
  if (week.isPending || members.isPending) return <Spinner />
  if (week.error) return <ErrorBox error={week.error} />
  if (!week.data || !members.data) return null
  return <Board week={week.data} members={members.data} />
}

function Board({ week, members }: { week: Week; members: Member[] }) {
  const actions = useMealActions(week.id)
  const readOnly = week.status === 'CONFIRMED'
  const membersById = useMemo(() => new Map(members.map((m) => [m.id, m])), [members])
  const mealsById = useMemo(() => new Map(week.meals.map((m) => [m.id, m])), [week.meals])
  const days = useMemo(() => Array.from({ length: 7 }, (_, i) => addDays(week.weekStart, i)), [week.weekStart])
  const [dragging, setDragging] = useState<Meal | null>(null)
  const [picking, setPicking] = useState<Meal | null>(null)
  const [copying, setCopying] = useState(false)
  const [toast, setToast] = useState<{ title: string; reasons: string[] } | null>(null)

  const sensors = useSensors(
    // A small distance lets clicks on the card's buttons through without starting a drag.
    useSensor(PointerSensor, { activationConstraint: { distance: 6 } }),
    useSensor(KeyboardSensor),
  )

  const showError = useCallback((error: unknown) => {
    if (error instanceof ApiError) setToast({ title: error.message, reasons: error.reasons })
    else setToast({ title: 'No s’ha pogut desar el canvi.', reasons: [] })
  }, [])

  useEffect(() => {
    if (!toast) return
    const timer = window.setTimeout(() => setToast(null), 6000)
    return () => window.clearTimeout(timer)
  }, [toast])

  const onDragStart = (event: DragStartEvent) => {
    setToast(null)
    setDragging(mealsById.get(Number(event.active.id)) ?? null)
  }

  const onDragEnd = (event: DragEndEvent) => {
    setDragging(null)
    const from = mealsById.get(Number(event.active.id))
    const to = event.over ? mealsById.get(Number(event.over.id)) : undefined
    if (!from || !to || from.id === to.id) return
    const reasons = swapViolations(from, to, membersById)
    if (reasons.length > 0) {
      setToast({ title: 'Aquí no hi pot anar.', reasons })
      return
    }
    actions.swap.mutate({ mealIdA: from.id, mealIdB: to.id }, { onError: showError })
  }

  const assigned = week.meals.filter((m) => m.recipe).length
  const lunchboxes = week.meals.reduce((sum, m) => sum + m.dinersTakeaway, 0)
  const busy = actions.generate.isPending || actions.confirm.isPending

  return (
    <div className="space-y-5 pb-10">
      <header className="flex items-end justify-between gap-4">
        <div>
          <p className="text-sm text-stone-500">Menú de la setmana</p>
          <h1 className="text-2xl font-semibold">{formatWeekRange(week.weekStart)}</h1>
          <p className="mt-1 text-sm text-stone-600">
            {assigned} de {week.meals.length} àpats amb plat · {lunchboxes} carmanyoles
          </p>
        </div>
        {readOnly ? (
          <div className="flex items-center gap-3">
            <span className="rounded-full bg-avocado-100 px-3 py-1 text-sm font-medium text-avocado-800">Confirmada</span>
            <Button
              variant="secondary"
              disabled={actions.reopen.isPending}
              onClick={() => confirmReopen() && actions.reopen.mutate(undefined, { onError: showError })}
            >
              {actions.reopen.isPending ? 'Tornant…' : '↩ Torna a esborrany'}
            </Button>
            <Button variant="secondary" onClick={() => setCopying(true)}>Copia…</Button>
            <PrintButton weekId={week.id} />
            <Button onClick={() => navigate({ screen: 'shopping', weekId: week.id })}>Llista de la compra →</Button>
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <Button variant="secondary" disabled={assigned === 0} onClick={() => setCopying(true)}
              title={assigned === 0 ? 'Encara no hi ha plats per copiar' : 'Copia aquest menú a una altra setmana'}>
              Copia…
            </Button>
            <PrintButton weekId={week.id} />
            <Button
              variant="secondary"
              disabled={busy}
              onClick={() => actions.generate.mutate(undefined, { onError: showError })}
              title="Proposa plats per als àpats no bloquejats"
            >
              {actions.generate.isPending ? 'Generant…' : assigned > 0 ? '↻ Torna a generar' : '✦ Genera el menú'}
            </Button>
            <Button
              disabled={busy || assigned < week.meals.length || week.meals.length === 0}
              title={assigned < week.meals.length ? 'Tots els àpats han de tenir plat' : undefined}
              onClick={() =>
                actions.confirm.mutate(undefined, {
                  onSuccess: () => navigate({ screen: 'shopping', weekId: week.id }),
                  onError: showError,
                })
              }
            >
              {actions.confirm.isPending ? 'Confirmant…' : 'Confirma i fes la llista →'}
            </Button>
          </div>
        )}
      </header>

      {!readOnly && (
        <p className="text-xs text-stone-500">
          Arrossegueu un plat a un altre àpat per intercanviar-los. Els plats amb{' '}
          <LunchboxIcon className="inline size-3.5 text-terracotta-600" /> aguanten la carmanyola. Bloquegeu (🔒) els
          que vulgueu conservar abans de tornar a generar.
        </p>
      )}

      <DndContext sensors={sensors} collisionDetection={pointerWithin} onDragStart={onDragStart} onDragEnd={onDragEnd} onDragCancel={() => setDragging(null)}>
        <div className="grid grid-cols-[4.5rem_repeat(7,minmax(0,1fr))] gap-2">
          <div />
          {days.map((date) => (
            <div key={date} className="px-1">
              <p className="font-semibold">{formatWeekday(date)}</p>
              <p className="text-xs text-stone-500">{formatDayMonth(date)}</p>
            </div>
          ))}
          {SLOTS.map((slot) => (
            <SlotRow
              key={slot}
              slot={slot}
              days={days}
              week={week}
              dragging={dragging}
              membersById={membersById}
              readOnly={readOnly}
              onPick={setPicking}
              onClear={(meal) => actions.assign.mutate({ mealId: meal.id, recipeId: null }, { onError: showError })}
              onToggleLock={(meal) =>
                actions.update.mutate({ mealId: meal.id, locked: !meal.locked }, { onError: showError })
              }
            />
          ))}
        </div>
        <DragOverlay dropAnimation={{ duration: 180, easing: 'cubic-bezier(0.2, 0, 0, 1)' }}>
          {dragging?.recipe ? (
            <div className="rotate-2 scale-105 cursor-grabbing">
              <DishCardBody meal={dragging} overlay />
            </div>
          ) : null}
        </DragOverlay>
      </DndContext>

      {copying && <CopyWeekDialog sourceWeekId={week.id} sourceWeekStart={week.weekStart} onClose={() => setCopying(false)} />}

      {picking && (
        <RecipePicker
          weekId={week.id}
          meal={picking}
          busy={actions.assign.isPending}
          error={actions.assign.error}
          onClose={() => {
            actions.assign.reset()
            setPicking(null)
          }}
          onPick={(recipe) =>
            actions.assign.mutate({ mealId: picking.id, recipeId: recipe.id }, { onSuccess: () => setPicking(null) })
          }
        />
      )}

      {toast && (
        <div
          role="alert"
          className="fixed bottom-6 left-1/2 z-40 w-[36rem] -translate-x-1/2 rounded-xl bg-stone-900 px-5 py-4 text-sm text-white shadow-2xl"
        >
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="font-medium">{toast.title}</p>
              {toast.reasons.map((r) => (
                <p key={r} className="mt-1 text-stone-300">
                  {r}
                </p>
              ))}
            </div>
            <button type="button" className="text-stone-400 hover:text-white" onClick={() => setToast(null)}>
              ✕
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

function SlotRow({
  slot,
  days,
  week,
  dragging,
  membersById,
  readOnly,
  onPick,
  onClear,
  onToggleLock,
}: {
  slot: MealSlot
  days: string[]
  week: Week
  dragging: Meal | null
  membersById: Map<number, Member>
  readOnly: boolean
  onPick: (meal: Meal) => void
  onClear: (meal: Meal) => void
  onToggleLock: (meal: Meal) => void
}) {
  return (
    <>
      <div className="pt-3 font-semibold text-stone-700">{SLOT[slot]}</div>
      {days.map((date) => {
        const meal = week.meals.find((m) => m.date === date && m.slot === slot)
        if (!meal) {
          return (
            <div
              key={date}
              className="flex min-h-44 items-center justify-center rounded-xl bg-stone-50 text-xs text-stone-400 ring-1 ring-stone-200"
            >
              No planificat
            </div>
          )
        }
        const reasons = dragging && dragging.id !== meal.id ? swapViolations(dragging, meal, membersById) : []
        return (
          <MealSlotCell
            key={date}
            meal={meal}
            dragging={dragging}
            blockedReasons={reasons}
            readOnly={readOnly}
            onPick={() => onPick(meal)}
            onClear={() => onClear(meal)}
            onToggleLock={() => onToggleLock(meal)}
          />
        )
      })}
    </>
  )
}

function MealSlotCell({
  meal,
  dragging,
  blockedReasons,
  readOnly,
  onPick,
  onClear,
  onToggleLock,
}: {
  meal: Meal
  dragging: Meal | null
  blockedReasons: string[]
  readOnly: boolean
  onPick: () => void
  onClear: () => void
  onToggleLock: () => void
}) {
  const { setNodeRef, isOver } = useDroppable({ id: meal.id, disabled: readOnly })
  const isSource = dragging?.id === meal.id
  const blocked = dragging !== null && !isSource && blockedReasons.length > 0

  const ring = !dragging
    ? 'ring-stone-200'
    : isSource
      ? 'ring-stone-200 opacity-40'
      : blocked
        ? isOver
          ? 'ring-2 ring-terracotta-500 bg-terracotta-50'
          : 'ring-terracotta-200 bg-terracotta-50/40'
        : isOver
          ? 'ring-2 ring-brand-600 bg-brand-50'
          : 'ring-brand-300'

  return (
    <div
      ref={setNodeRef}
      className={`relative flex min-h-44 flex-col rounded-xl bg-white p-2 ring-1 transition-all duration-150 ${ring}`}
    >
      <Diners meal={meal} />
      {meal.recipe ? (
        <DraggableDish
          meal={meal}
          readOnly={readOnly}
          onPick={onPick}
          onClear={onClear}
          onToggleLock={onToggleLock}
        />
      ) : (
        <div className="flex flex-1 flex-col items-center justify-center gap-2 p-2 text-center">
          {meal.emptyReason && <p className="text-xs leading-snug text-amber-800">{meal.emptyReason}</p>}
          {!readOnly && (
            <Button variant="ghost" className="px-2 py-1 text-xs" onClick={onPick}>
              + Tria un plat
            </Button>
          )}
        </div>
      )}
      {blocked && isOver && (
        <div className="pointer-events-none absolute inset-x-1 bottom-1 rounded-lg bg-terracotta-600 px-2 py-1 text-[0.6875rem] leading-snug text-white">
          {blockedReasons[0]}
        </div>
      )}
    </div>
  )
}

function Diners({ meal }: { meal: Meal }) {
  return (
    <div className="mb-1.5 flex items-center gap-2 px-1 text-[0.6875rem] text-stone-500">
      {meal.dinersAtHome > 0 && (
        <span className="inline-flex items-center gap-0.5" title="A casa">
          <HomeIcon className="size-3" /> {meal.dinersAtHome}
        </span>
      )}
      {meal.dinersTakeaway > 0 && (
        <span className="inline-flex items-center gap-0.5 font-medium text-terracotta-700" title="Carmanyola">
          <LunchboxIcon className="size-3" /> {meal.dinersTakeaway}
        </span>
      )}
      {meal.extraServings > 0 && <span title="Racions de més">+{meal.extraServings}</span>}
    </div>
  )
}

function DraggableDish({
  meal,
  readOnly,
  onPick,
  onClear,
  onToggleLock,
}: {
  meal: Meal
  readOnly: boolean
  onPick: () => void
  onClear: () => void
  onToggleLock: () => void
}) {
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({ id: meal.id, disabled: readOnly })
  return (
    <div
      ref={setNodeRef}
      {...listeners}
      {...attributes}
      className={`flex-1 outline-none ${readOnly ? '' : 'cursor-grab active:cursor-grabbing'} ${isDragging ? 'invisible' : ''}`}
    >
      <DishCardBody
        meal={meal}
        actions={
          readOnly ? null : (
            <div className="mt-2 flex items-center gap-0.5 border-t border-stone-100 pt-1.5">
              <IconButton title={meal.locked ? 'Desbloqueja' : 'Bloqueja (no es canviarà en regenerar)'} onClick={onToggleLock} active={meal.locked}>
                {meal.locked ? '🔒' : '🔓'}
              </IconButton>
              <IconButton title="Canvia el plat" onClick={onPick}>
                ⇄
              </IconButton>
              <IconButton title="Treu el plat" onClick={onClear}>
                ✕
              </IconButton>
            </div>
          )
        }
      />
    </div>
  )
}

function DishCardBody({
  meal,
  actions,
  overlay = false,
}: {
  meal: Meal
  actions?: React.ReactNode
  overlay?: boolean
}) {
  const recipe = meal.recipe!
  const proteinName = useProteinName()
  return (
    <div
      className={`flex h-full flex-col rounded-lg p-2 ${
        overlay ? 'w-44 bg-white shadow-2xl ring-2 ring-brand-600' : meal.locked ? 'bg-brand-50' : 'bg-cream-50'
      }`}
    >
      <p className="text-sm font-medium leading-snug hyphens-auto break-words text-stone-900">{recipe.name}</p>
      <p className="mt-0.5 flex flex-wrap items-center gap-x-1 text-[0.6875rem] text-stone-500">
        {recipe.portable && (
          <span
            title={recipe.lunchboxServing === 'COLD' ? 'Apte per a carmanyola · fred' : 'Apte per a carmanyola · escalfar'}
            className="inline-flex items-center gap-0.5 rounded bg-terracotta-100 px-1 text-terracotta-700"
          >
            <LunchboxIcon className="size-3" />
            {recipe.lunchboxServing === 'COLD' ? 'fred' : 'escalfar'}
          </span>
        )}
        <span>
          {proteinName(recipe.proteinType)} · {recipe.prepMinutes} min
        </span>
      </p>
      <div className="mt-auto pt-2">
        <KcalFit meal={meal} />
        {meal.notes.length > 0 && (
          <p className="mt-1 text-[0.6875rem] leading-snug text-amber-700" title={meal.notes.join('\n')}>
            ⚠ {meal.notes[0]}
          </p>
        )}
      </div>
      {actions}
    </div>
  )
}

function KcalFit({ meal }: { meal: Meal }) {
  if (meal.kcalFit === null || meal.kcalTarget === 0) {
    return <p className="text-[0.6875rem] text-stone-400">{meal.recipe ? `${formatNumber(meal.recipe.nutrition.kcal)} kcal/ració` : ''}</p>
  }
  const level = fitLevel(meal.kcalFit)
  const bar = level === 'good' ? 'bg-avocado-500' : level === 'fair' ? 'bg-amber-500' : 'bg-terracotta-500'
  const width = Math.min(100, Math.round((meal.kcalFit / 1.5) * 100))
  return (
    <div title={`${formatNumber(meal.kcalOffered ?? 0)} kcal per a ${meal.dinersAtHome + meal.dinersTakeaway} · objectiu ${formatNumber(meal.kcalTarget)} kcal`}>
      <div className="flex flex-wrap justify-between gap-x-1 text-[0.6875rem] text-stone-500">
        <span className="whitespace-nowrap" title="kcal per ració">{formatNumber(meal.recipe!.nutrition.kcal)} kcal</span>
        <span className="tabular-nums">{Math.round(meal.kcalFit * 100)} %</span>
      </div>
      <div className="relative mt-0.5 h-1.5 overflow-hidden rounded-full bg-stone-200">
        <div className={`h-full rounded-full ${bar}`} style={{ width: `${width}%` }} />
        {/* Marker at 100 % of the target */}
        <div className="absolute inset-y-0 w-px bg-stone-500" style={{ left: `${(1 / 1.5) * 100}%` }} />
      </div>
    </div>
  )
}

function IconButton({
  title,
  onClick,
  active = false,
  children,
}: {
  title: string
  onClick: () => void
  active?: boolean
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      title={title}
      aria-label={title}
      // Keep pointer-down from starting a drag on the parent card.
      onPointerDown={(e) => e.stopPropagation()}
      onKeyDown={(e) => e.stopPropagation()}
      onClick={onClick}
      className={`flex size-6 items-center justify-center rounded text-xs hover:bg-stone-200 ${active ? 'bg-brand-200' : 'text-stone-500'}`}
    >
      {children}
    </button>
  )
}
