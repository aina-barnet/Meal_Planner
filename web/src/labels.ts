import type {
  IngredientCategory,
  ScalingMode,
  Unit,
  Aisle,
  Allergen,
  AllergenKind,
  Cuisine,
  DietType,
  MealSlot,
  Season,
} from './api/types'

export const DIET: Record<DietType, string> = {
  OMNIVORE: 'Omnívora',
  PESCATARIAN: 'Pescetariana',
  VEGETARIAN: 'Vegetariana',
  VEGAN: 'Vegana',
}

export const ALLERGEN: Record<Allergen, string> = {
  GLUTEN: 'Gluten',
  CRUSTACEANS: 'Crustacis',
  EGGS: 'Ou',
  FISH: 'Peix',
  PEANUTS: 'Cacauets',
  SOY: 'Soja',
  MILK: 'Llet',
  TREE_NUTS: 'Fruits de closca',
  CELERY: 'Api',
  MUSTARD: 'Mostassa',
  SESAME: 'Sèsam',
  SULPHITES: 'Sulfits',
  LUPIN: 'Tramussos',
  MOLLUSCS: 'Mol·luscs',
}

export const ALLERGEN_KIND: Record<AllergenKind, string> = {
  ALLERGY: 'Al·lèrgia',
  INTOLERANCE: 'Intolerància',
}

export const SLOT: Record<MealSlot, string> = { LUNCH: 'Dinar', DINNER: 'Sopar' }

export const CUISINE: Record<Cuisine, string> = {
  CATALAN: 'Catalana',
  SPANISH: 'Espanyola',
  ITALIAN: 'Italiana',
  GREEK: 'Grega',
  MOROCCAN: 'Marroquina',
  LEVANTINE: 'Llevantina',
  FRENCH: 'Francesa',
  OTHER: 'Altres',
}

export const UNIT: Record<Unit, string> = {
  G: 'g',
  KG: 'kg',
  ML: 'ml',
  CL: 'cl',
  L: 'l',
  UNIT: 'unitat',
  TBSP: 'cullerada',
  TSP: 'culleradeta',
  CLOVE: 'gra',
  PINCH: 'pessic',
  SLICE: 'llesca',
  BUNCH: 'manat',
  SPRIG: 'branca',
  CAN: 'llauna',
  CUP: 'tassa',
}

/** Units that mean something different per ingredient (1 egg = 60 g) and need a conversion. */
export const COUNTABLE_UNITS: Unit[] = ['UNIT', 'TBSP', 'TSP', 'CLOVE', 'PINCH', 'SLICE', 'BUNCH', 'SPRIG', 'CAN', 'CUP']

export const SCALING: Record<ScalingMode, { label: string; hint: string }> = {
  LINEAR: { label: 'Proporcional', hint: 'Augmenta amb les racions' },
  FIXED: { label: 'Fixa', hint: 'Sempre igual (llorer, un pessic de sal)' },
  SUBLINEAR: { label: 'Moderada', hint: 'Augmenta a mig ritme (oli de fregir)' },
}

export const CATEGORY: Record<IngredientCategory, string> = {
  VEGETABLE: 'Verdura',
  FRUIT: 'Fruita',
  HERB: 'Herba aromàtica',
  LEGUME: 'Llegum',
  GRAIN: 'Cereal, pasta o pa',
  NUT: 'Fruit sec',
  OIL: 'Oli',
  SPICE: 'Espècia',
  CONDIMENT: 'Condiment o brou',
  DAIRY: 'Làctic',
  EGG: 'Ou',
  FISH: 'Peix',
  SEAFOOD: 'Marisc',
  MEAT: 'Carn',
  OTHER: 'Altres',
}

export const SEASON: Record<Season, string> = {
  SPRING: 'Primavera',
  SUMMER: 'Estiu',
  AUTUMN: 'Tardor',
  WINTER: 'Hivern',
}

export const AISLE: Record<Aisle, string> = {
  PRODUCE: 'Fruita i verdura',
  BAKERY: 'Fleca',
  BUTCHER: 'Carnisseria',
  FISHMONGER: 'Peixateria',
  DAIRY_EGGS: 'Làctics i ous',
  PANTRY: 'Rebost',
  CANNED: 'Conserves',
  SPICES: 'Espècies',
  DRINKS: 'Begudes',
  FROZEN: 'Congelats',
}

const WEEKDAY = new Intl.DateTimeFormat('ca', { weekday: 'long' })
const WEEKDAY_SHORT = new Intl.DateTimeFormat('ca', { weekday: 'short' })
const DAY_MONTH = new Intl.DateTimeFormat('ca', { day: 'numeric', month: 'long' })
const NUMBER = new Intl.NumberFormat('ca')

/** Parses an ISO date (yyyy-mm-dd) as a local date, avoiding timezone shifts. */
export function parseDate(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function toIso(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function addDays(iso: string, days: number): string {
  const date = parseDate(iso)
  date.setDate(date.getDate() + days)
  return toIso(date)
}

export const formatWeekday = (iso: string) => capitalise(WEEKDAY.format(parseDate(iso)))
export const formatWeekdayShort = (iso: string) => capitalise(WEEKDAY_SHORT.format(parseDate(iso)).replace('.', ''))
export const formatDayMonth = (iso: string) => DAY_MONTH.format(parseDate(iso))
export const formatNumber = (n: number) => NUMBER.format(n)

export function formatWeekRange(weekStart: string): string {
  const end = addDays(weekStart, 6)
  return `${formatDayMonth(weekStart)} – ${formatDayMonth(end)}`
}

function capitalise(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1)
}
