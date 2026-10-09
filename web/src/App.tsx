import { useEffect } from 'react'
import { useHousehold, useWeek } from './api/hooks'
import { useFontScale } from './fontScale'
import { formatWeekRange } from './labels'
import { type Route, href, navigate, useRoute } from './router'
import { MenuBoard } from './screens/MenuBoard'
import { PantryScreen } from './screens/PantryScreen'
import { RecipeEditor } from './screens/RecipeEditor'
import { RecipeList } from './screens/RecipeList'
import { SetupWizard } from './screens/SetupWizard'
import { ShoppingListScreen } from './screens/ShoppingListScreen'
import { WeekConfig } from './screens/WeekConfig'
import { WeekList } from './screens/WeekList'
import { Spinner } from './ui'

export default function App() {
  const household = useHousehold()
  const route = useRoute()

  // Default route: the wizard until the household exists, then the weeks.
  useEffect(() => {
    if (route === null && household.isSuccess) {
      navigate(household.data ? { screen: 'weeks' } : { screen: 'household' })
    }
  }, [route, household.isSuccess, household.data])

  return (
    <div className="min-h-screen">
      <Header route={route} householdName={household.data?.name} />
      <main className="mx-auto max-w-7xl px-8 py-8 print:max-w-none print:p-0">
        {household.isPending || route === null ? <Spinner /> : <Screen route={route} />}
      </main>
    </div>
  )
}

function Screen({ route }: { route: Route }) {
  switch (route.screen) {
    case 'household':
      return <SetupWizard />
    case 'weeks':
      return <WeekList />
    case 'week':
      return <WeekConfig weekId={route.weekId} />
    case 'pantry':
      return <PantryScreen />
    case 'recipes':
      return <RecipeList />
    case 'recipe':
      return <RecipeEditor key={route.recipeId ?? 'nou'} recipeId={route.recipeId} />
    case 'menu':
      return <MenuBoard weekId={route.weekId} />
    case 'shopping':
      return <ShoppingListScreen weekId={route.weekId} />
  }
}

function Header({ route, householdName }: { route: Route | null; householdName?: string }) {
  const weekId = route && 'weekId' in route ? route.weekId : null
  const week = useWeek(weekId)
  const tab = (target: Route, label: string) => {
    const active = route?.screen === target.screen || (target.screen === 'recipes' && route?.screen === 'recipe')
    return (
      <a
        href={href(target)}
        className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
          active ? 'bg-brand-600 text-white shadow-sm shadow-brand-600/25' : 'text-stone-700 hover:bg-brand-50 hover:text-brand-800'
        }`}
      >
        {label}
      </a>
    )
  }

  return (
    <header className="border-b-2 border-brand-100 bg-cream-50/90 backdrop-blur print:hidden">
      <div className="mx-auto flex max-w-7xl flex-wrap items-center gap-x-6 gap-y-2 px-8 py-3">
        <a href="#/setmana" className="flex items-center gap-2.5">
          <AvocadoLogo />
          <span className="leading-none">
            <span className="block font-display text-xl uppercase tracking-wide text-brand-600">Planificador d'àpats</span>
            {householdName && (
              <span className="block text-[0.6875rem] font-semibold uppercase tracking-widest text-brand-800/70">
                {householdName}
              </span>
            )}
          </span>
        </a>
        <nav className="flex items-center gap-1">
          {tab({ screen: 'household' }, 'Llar')}
          {tab({ screen: 'weeks' }, 'Setmanes')}
          {tab({ screen: 'recipes' }, 'Plats')}
          {tab({ screen: 'pantry' }, 'Rebost')}
        </nav>
        {weekId !== null && (
          <nav className="ml-auto flex items-center gap-1 rounded-2xl bg-white p-1 ring-1 ring-brand-100">
            <span className="px-2 text-sm text-stone-500">{week.data ? formatWeekRange(week.data.weekStart) : '…'}</span>
            {tab({ screen: 'week', weekId }, 'Configuració')}
            {tab({ screen: 'menu', weekId }, 'Menú')}
            {tab({ screen: 'shopping', weekId }, 'Compra')}
          </nav>
        )}
        <FontSizeControl className={weekId === null ? 'ml-auto' : ''} />
      </div>
    </header>
  )
}

function FontSizeControl({ className }: { className: string }) {
  const font = useFontScale()
  const button =
    'flex h-8 min-w-8 items-center justify-center rounded-md px-1.5 font-semibold text-brand-700 hover:bg-brand-50 disabled:text-stone-300 disabled:hover:bg-transparent'
  return (
    <div className={`flex items-center gap-0.5 rounded-xl bg-white p-0.5 ring-1 ring-brand-100 ${className}`} role="group" aria-label="Mida de la lletra">
      <button type="button" className={`${button} text-xs`} disabled={!font.canShrink} onClick={font.shrink} title="Lletra més petita" aria-label="Lletra més petita">
        A−
      </button>
      <button type="button" className="px-1 text-xs tabular-nums text-stone-500 hover:text-stone-800" onClick={font.reset} title="Mida normal">
        {font.percent}%
      </button>
      <button type="button" className={`${button} text-base`} disabled={!font.canGrow} onClick={font.grow} title="Lletra més gran" aria-label="Lletra més gran">
        A+
      </button>
    </div>
  )
}

/** Half an avocado with its coral stone, after the poster. */
function AvocadoLogo() {
  return (
    <svg viewBox="0 0 40 40" className="size-10 shrink-0" aria-hidden>
      <path d="M20 3c6.5 0 11 7.5 13 15.5 2.4 9.6-3.8 18.5-13 18.5S4.6 28.1 7 18.5C9 10.5 13.5 3 20 3z" fill="#75932c" />
      <path d="M20 6.5c5 0 8.6 6.4 10.2 13 1.9 7.7-3 14.5-10.2 14.5S7.9 27.2 9.8 19.5C11.4 12.9 15 6.5 20 6.5z" fill="#e5efc8" />
      <circle cx="20" cy="24" r="6.5" fill="#f2603f" />
      <circle cx="18" cy="22" r="2" fill="#ffbfa9" />
      <path d="M33 4l1.2 3.3L37.5 8l-3.3 1.2L33 12.5l-1.2-3.3L28.5 8l3.3-.7z" fill="#ff5a0a" />
    </svg>
  )
}
