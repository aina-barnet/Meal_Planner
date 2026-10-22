// Mirrors the API's DTOs (see /api/docs).

export type DietType = 'OMNIVORE' | 'PESCATARIAN' | 'VEGETARIAN' | 'VEGAN'
export type Allergen =
  | 'GLUTEN' | 'CRUSTACEANS' | 'EGGS' | 'FISH' | 'PEANUTS' | 'SOY' | 'MILK'
  | 'TREE_NUTS' | 'CELERY' | 'MUSTARD' | 'SESAME' | 'SULPHITES' | 'LUPIN' | 'MOLLUSCS'
export type AllergenKind = 'ALLERGY' | 'INTOLERANCE'
export type MealSlot = 'LUNCH' | 'DINNER'
export type DinerLocation = 'HOME' | 'TAKEAWAY'
export type WeekStatus = 'DRAFT' | 'CONFIRMED'
export type CanonicalUnit = 'G' | 'ML'
/** Code of a protein type; the list comes from /api/proteins. */
export type ProteinType = string
export type Unit =
  | 'G' | 'KG' | 'ML' | 'CL' | 'L' | 'UNIT' | 'TBSP' | 'TSP' | 'CLOVE' | 'PINCH'
  | 'SLICE' | 'BUNCH' | 'SPRIG' | 'CAN' | 'CUP'
export type IngredientCategory =
  | 'VEGETABLE' | 'FRUIT' | 'HERB' | 'LEGUME' | 'GRAIN' | 'NUT' | 'OIL' | 'SPICE' | 'CONDIMENT'
  | 'DAIRY' | 'EGG' | 'FISH' | 'SEAFOOD' | 'MEAT' | 'OTHER'

export interface Household {
  id: number
  name: string
}

export interface AllergenEntry {
  allergen: Allergen
  kind: AllergenKind
}

export interface MemberInput {
  name: string
  dietType: DietType
  allergens: AllergenEntry[]
}

export interface Member extends MemberInput {
  id: number
}

export interface Ingredient {
  id: number
  name: string
  category: IngredientCategory
  canonicalUnit: CanonicalUnit
  allergens: Allergen[]
  conversions: Partial<Record<Unit, number>>
  custom: boolean
}

export interface IngredientInput {
  name: string
  category: IngredientCategory
  canonicalUnit: CanonicalUnit
  allergens: Allergen[]
  conversions: Partial<Record<Unit, number>>
}

export interface RecipeSummary {
  id: number
  code: string
  name: string
  baseServings: number
  prepMinutes: number
  proteinType: ProteinType
  /** Also works in a lunchbox; every dish can be eaten at home. */
  portable: boolean
  allergens: Allergen[]
  diets: DietType[]
  custom: boolean
  hidden: boolean
}

export interface RecipeLine {
  ingredientId: number
  name: string
  quantity: number
  unit: Unit
  canonicalQuantity: number
  canonicalUnit: CanonicalUnit
}

export interface RecipeDetail {
  summary: RecipeSummary
  ingredients: RecipeLine[]
  steps: string[]
}

export interface RecipeInput {
  name: string
  baseServings: number
  prepMinutes: number
  proteinType: ProteinType
  portable: boolean
  ingredients: { ingredientId: number; quantity: number; unit: Unit }[]
  steps: string[]
}

export interface Attendee {
  memberId: number
  location: DinerLocation
}

export interface Meal {
  id: number
  date: string
  slot: MealSlot
  attendees: Attendee[]
  dinersAtHome: number
  dinersTakeaway: number
  extraServings: number
  totalServings: number
  locked: boolean
  emptyReason: string | null
  recipe: RecipeSummary | null
}

export interface Week {
  id: number
  weekStart: string
  status: WeekStatus
  confirmedAt: string | null
  meals: Meal[]
}

export interface WeekSummary {
  id: number
  weekStart: string
  status: WeekStatus
  confirmedAt: string | null
  mealCount: number
  assignedCount: number
}

export interface MealConfigInput {
  date: string
  slot: MealSlot
  attendees: Attendee[]
  extraServings: number
}

export interface Protein {
  code: string
  name: string
  custom: boolean
  recipes: number
}
