import type { Member, MemberInput } from './api/types'

export function emptyMember(): MemberInput {
  return {
    name: '',
    birthDate: '',
    sex: 'FEMALE',
    weightKg: 0,
    heightCm: 0,
    activityLevel: 'LIGHT',
    dietType: 'OMNIVORE',
    dietNotes: null,
    kcalOverride: null,
    mainMealsPct: 65,
    allergens: [],
    dislikedIngredientIds: [],
  }
}

export function toInput(member: Member): MemberInput {
  const { id: _id, requirement: _requirement, ...input } = member
  return input
}
