import type { Allergen, DietType, Ingredient, IngredientCategory, Nutrition, Unit } from './api/types'

/**
 * Client-side mirror of the domain's unit conversion and recipe facts, for the live preview in
 * the recipe editor. The API recomputes everything on save.
 */
const METRIC: Partial<Record<Unit, { base: 'G' | 'ML'; factor: number }>> = {
  G: { base: 'G', factor: 1 },
  KG: { base: 'G', factor: 1000 },
  ML: { base: 'ML', factor: 1 },
  CL: { base: 'ML', factor: 10 },
  L: { base: 'ML', factor: 1000 },
}

export function supportedUnits(ingredient: Ingredient): Unit[] {
  const metric = (Object.keys(METRIC) as Unit[]).filter((u) => METRIC[u]!.base === ingredient.canonicalUnit)
  return [...metric, ...(Object.keys(ingredient.conversions) as Unit[])]
}

export function toCanonical(ingredient: Ingredient, quantity: number, unit: Unit): number | null {
  const metric = METRIC[unit]
  if (metric) return metric.base === ingredient.canonicalUnit ? quantity * metric.factor : null
  const factor = ingredient.conversions[unit]
  return factor === undefined ? null : quantity * factor
}

export interface Line {
  ingredient: Ingredient
  quantity: number
  unit: Unit
}

export function nutritionPerServing(lines: Line[], servings: number): Nutrition {
  const total = lines.reduce(
    (sum, { ingredient, quantity, unit }) => {
      const grams = toCanonical(ingredient, quantity, unit) ?? 0
      return {
        kcal: sum.kcal + (grams * ingredient.kcalPer100) / 100,
        proteinG: sum.proteinG + (grams * ingredient.proteinPer100) / 100,
        carbsG: sum.carbsG + (grams * ingredient.carbsPer100) / 100,
        fatG: sum.fatG + (grams * ingredient.fatPer100) / 100,
      }
    },
    { kcal: 0, proteinG: 0, carbsG: 0, fatG: 0 },
  )
  const n = Math.max(servings, 1)
  return { kcal: total.kcal / n, proteinG: total.proteinG / n, carbsG: total.carbsG / n, fatG: total.fatG / n }
}

export function allergensOf(lines: Line[]): Allergen[] {
  return [...new Set(lines.flatMap((l) => l.ingredient.allergens))].sort()
}

const EXCLUDED: Record<DietType, IngredientCategory[]> = {
  OMNIVORE: [],
  PESCATARIAN: ['MEAT'],
  VEGETARIAN: ['MEAT', 'FISH', 'SEAFOOD'],
  VEGAN: ['MEAT', 'FISH', 'SEAFOOD', 'DAIRY', 'EGG'],
}

export function dietsOf(lines: Line[]): DietType[] {
  return (Object.keys(EXCLUDED) as DietType[]).filter((diet) =>
    lines.every((l) => !EXCLUDED[diet].includes(l.ingredient.category)),
  )
}
