import type { Meal, Member, RecipeSummary } from './api/types'
import { ALLERGEN, DIET } from './labels'

/**
 * Client-side mirror of the API's hard constraints (MealAssignmentRules), used to give instant
 * feedback while dragging. The API remains the authority and re-checks every change.
 */
export function violations(recipe: RecipeSummary, meal: Meal, members: Map<number, Member>): string[] {
  const reasons: string[] = []
  if (meal.dinersTakeaway > 0 && !recipe.portable) {
    reasons.push(
      `«${recipe.name}» no aguanta bé la carmanyola i ${
        meal.dinersTakeaway === 1 ? "1 persona se l'emporta" : `${meal.dinersTakeaway} persones se l'emporten`
      } en aquest àpat.`,
    )
  }
  for (const attendee of meal.attendees) {
    const member = members.get(attendee.memberId)
    if (!member) continue
    for (const { allergen } of member.allergens) {
      if (recipe.allergens.includes(allergen)) {
        reasons.push(`«${recipe.name}» conté ${ALLERGEN[allergen].toLowerCase()} i ${member.name} hi és al·lèrgic/a o intolerant.`)
      }
    }
    if (!recipe.diets.includes(member.dietType)) {
      reasons.push(`«${recipe.name}» no és compatible amb la dieta ${DIET[member.dietType].toLowerCase()} de ${member.name}.`)
    }
  }
  return reasons
}

/** Reasons why swapping the dishes of two meals is not allowed (both directions). */
export function swapViolations(from: Meal, to: Meal, members: Map<number, Member>): string[] {
  return [
    ...(from.recipe ? violations(from.recipe, to, members) : []),
    ...(to.recipe ? violations(to.recipe, from, members) : []),
  ]
}
