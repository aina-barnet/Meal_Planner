import type { Member, MemberInput } from './api/types'

export function emptyMember(): MemberInput {
  return {
    name: '',
    dietType: 'OMNIVORE',
    dietNotes: null,
    allergens: [],
    dislikedIngredientIds: [],
  }
}

export function toInput(member: Member): MemberInput {
  const { id: _id, ...input } = member
  return input
}
