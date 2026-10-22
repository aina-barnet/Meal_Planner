import type { Member, MemberInput } from './api/types'

export function emptyMember(): MemberInput {
  return {
    name: '',
    dietType: 'OMNIVORE',
    allergens: [],
  }
}

export function toInput(member: Member): MemberInput {
  const { id: _id, ...input } = member
  return input
}
