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
export type Aisle =
  | 'PRODUCE' | 'BAKERY' | 'BUTCHER' | 'FISHMONGER' | 'DAIRY_EGGS'
  | 'PANTRY' | 'CANNED' | 'SPICES' | 'DRINKS' | 'FROZEN'
/** Code of a protein type; the list comes from /api/proteins. */
export type ProteinType = string
export type Cuisine = 'CATALAN' | 'SPANISH' | 'ITALIAN' | 'GREEK' | 'MOROCCAN' | 'LEVANTINE' | 'FRENCH' | 'OTHER'
export type Unit =
  | 'G' | 'KG' | 'ML' | 'CL' | 'L' | 'UNIT' | 'TBSP' | 'TSP' | 'CLOVE' | 'PINCH'
  | 'SLICE' | 'BUNCH' | 'SPRIG' | 'CAN' | 'CUP'
export type ScalingMode = 'LINEAR' | 'FIXED' | 'SUBLINEAR'
export type IngredientCategory =
  | 'VEGETABLE' | 'FRUIT' | 'HERB' | 'LEGUME' | 'GRAIN' | 'NUT' | 'OIL' | 'SPICE' | 'CONDIMENT'
  | 'DAIRY' | 'EGG' | 'FISH' | 'SEAFOOD' | 'MEAT' | 'OTHER'
export type Season = 'SPRING' | 'SUMMER' | 'AUTUMN' | 'WINTER'

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
  dietNotes: string | null
  allergens: AllergenEntry[]
  dislikedIngredientIds: number[]
}

export interface Member extends MemberInput {
  id: number
}

export interface Ingredient {
  id: number
  name: string
  category: IngredientCategory
  canonicalUnit: CanonicalUnit
  packageSize: number
  aisle: Aisle
  pantryStaple: boolean
  allergens: Allergen[]
  conversions: Partial<Record<Unit, number>>
  custom: boolean
}

export interface IngredientInput {
  name: string
  category: IngredientCategory
  canonicalUnit: CanonicalUnit
  packageSize: number
  aisle: Aisle
  allergens: Allergen[]
  conversions: Partial<Record<Unit, number>>
  pantryStaple: boolean
}

export interface RecipeSummary {
  id: number
  code: string
  name: string
  description: string
  baseServings: number
  prepMinutes: number
  proteinType: ProteinType
  cuisine: Cuisine
  seasons: Season[]
  portable: boolean
  lunchboxServing: 'COLD' | 'REHEAT' | null
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
  scalingMode: ScalingMode
  canonicalQuantity: number
  canonicalUnit: CanonicalUnit
  pantryStaple: boolean
}

export interface RecipeDetail {
  summary: RecipeSummary
  ingredients: RecipeLine[]
  steps: string[]
}

export interface RecipeInput {
  name: string
  description: string | null
  baseServings: number
  prepMinutes: number
  proteinType: ProteinType
  cuisine: Cuisine
  seasons: Season[]
  portable: boolean
  lunchboxServing: 'COLD' | 'REHEAT' | null
  ingredients: { ingredientId: number; quantity: number; unit: Unit; scalingMode: ScalingMode }[]
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
  notes: string[]
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

export interface ShoppingItem {
  id: number
  ingredientId: number
  name: string
  canonicalUnit: CanonicalUnit
  required: number
  fromPantry: number
  toBuy: number
  packages: number
  packageSize: number
  leftover: number
  coveredByPantry: boolean
  checked: boolean
}

export interface ShoppingList {
  weekId: number
  generatedAt: string
  aisles: { aisle: Aisle; items: ShoppingItem[] }[]
  staples: string[]
}

export interface PantryItem {
  ingredientId: number
  name: string
  canonicalUnit: CanonicalUnit
  aisle: Aisle
  quantity: number
}

export interface Protein {
  code: string
  name: string
  custom: boolean
  recipes: number
}
