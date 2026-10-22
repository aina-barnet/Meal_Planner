import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError, api } from './client'
import type {
  Household,
  Ingredient,
  IngredientInput,
  MealConfigInput,
  Member,
  MemberInput,
  Protein,
  RecipeDetail,
  RecipeInput,
  RecipeSummary,
  Week,
  WeekSummary,
} from './types'

export const keys = {
  household: ['household'] as const,
  members: ['members'] as const,
  ingredients: ['ingredients'] as const,
  weeks: ['weeks'] as const,
  week: (id: number) => ['weeks', id] as const,
  allowedRecipes: (weekId: number, mealId: number) => ['weeks', weekId, 'meals', mealId, 'allowed'] as const,
  recipes: ['recipes'] as const,
  proteins: ['proteins'] as const,
  recipe: (id: number) => ['recipes', id] as const,
}

/** The household, or null while the setup wizard has not run. */
export function useHousehold() {
  return useQuery({
    queryKey: keys.household,
    queryFn: async () => {
      try {
        return await api.get<Household>('/api/household')
      } catch (e) {
        if (e instanceof ApiError && e.code === 'HOUSEHOLD_NOT_SET_UP') return null
        throw e
      }
    },
  })
}

export function useSaveHousehold() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (name: string) => api.put<Household>('/api/household', { name }),
    onSuccess: (household) => client.setQueryData(keys.household, household),
  })
}

export function useMembers() {
  return useQuery({ queryKey: keys.members, queryFn: () => api.get<Member[]>('/api/household/members') })
}

export function useSaveMember() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: ({ id, input }: { id?: number; input: MemberInput }) =>
      id === undefined
        ? api.post<Member>('/api/household/members', input)
        : api.put<Member>(`/api/household/members/${id}`, input),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: keys.members })
      client.invalidateQueries({ queryKey: keys.weeks })
    },
  })
}

export function useDeleteMember() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api.delete(`/api/household/members/${id}`),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: keys.members })
      client.invalidateQueries({ queryKey: keys.weeks })
    },
  })
}

export function useIngredients() {
  return useQuery({
    queryKey: keys.ingredients,
    queryFn: () => api.get<Ingredient[]>('/api/ingredients'),
    staleTime: Infinity,
  })
}

export function useWeeks() {
  return useQuery({ queryKey: keys.weeks, queryFn: () => api.get<WeekSummary[]>('/api/weeks') })
}

export function useWeek(id: number | null) {
  return useQuery({
    queryKey: keys.week(id ?? 0),
    queryFn: () => api.get<Week>(`/api/weeks/${id}`),
    enabled: id !== null,
  })
}

export function useCreateWeek() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (weekStart: string) => api.post<Week>('/api/weeks', { weekStart }),
    onSuccess: (week) => {
      client.setQueryData(keys.week(week.id), week)
      client.invalidateQueries({ queryKey: keys.weeks, exact: true })
    },
  })
}

export function useDeleteWeek() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api.delete(`/api/weeks/${id}`),
    onSuccess: () => client.invalidateQueries({ queryKey: keys.weeks }),
  })
}

export function useConfigureWeek(id: number) {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (meals: MealConfigInput[]) => api.put<Week>(`/api/weeks/${id}/meals`, { meals }),
    onSuccess: (week) => {
      client.setQueryData(keys.week(id), week)
      client.invalidateQueries({ queryKey: keys.weeks, exact: true })
    },
  })
}

/** Mutations on a draft week's meals. Each returns the whole updated week. */
export function useMealActions(weekId: number) {
  const client = useQueryClient()
  const onSuccess = (week: Week) => {
    client.setQueryData(keys.week(weekId), week)
    client.invalidateQueries({ queryKey: keys.weeks, exact: true })
  }
  return {
    assign: useMutation({
      mutationFn: ({ mealId, recipeId }: { mealId: number; recipeId: number | null }) =>
        api.put<Week>(`/api/weeks/${weekId}/meals/${mealId}/recipe`, { recipeId }),
      onSuccess,
    }),
    update: useMutation({
      mutationFn: ({ mealId, locked, extraServings }: { mealId: number; locked?: boolean; extraServings?: number }) =>
        api.patch<Week>(`/api/weeks/${weekId}/meals/${mealId}`, { locked, extraServings }),
      onSuccess,
    }),
    swap: useMutation({
      mutationFn: ({ mealIdA, mealIdB }: { mealIdA: number; mealIdB: number }) =>
        api.post<Week>(`/api/weeks/${weekId}/swap`, { mealIdA, mealIdB }),
      // Optimistic: swap the dishes locally so the drop feels instant; roll back on error.
      onMutate: async ({ mealIdA, mealIdB }) => {
        await client.cancelQueries({ queryKey: keys.week(weekId) })
        const previous = client.getQueryData<Week>(keys.week(weekId))
        if (previous) {
          const a = previous.meals.find((m) => m.id === mealIdA)
          const b = previous.meals.find((m) => m.id === mealIdB)
          if (a && b) {
            client.setQueryData<Week>(keys.week(weekId), {
              ...previous,
              meals: previous.meals.map((m) =>
                m.id === mealIdA
                  ? { ...m, recipe: b.recipe, locked: b.locked, emptyReason: null }
                  : m.id === mealIdB
                    ? { ...m, recipe: a.recipe, locked: a.locked, emptyReason: null }
                    : m,
              ),
            })
          }
        }
        return { previous }
      },
      onError: (_error, _vars, context) => {
        if (context?.previous) client.setQueryData(keys.week(weekId), context.previous)
      },
      onSuccess,
    }),
    generate: useMutation({
      mutationFn: () => api.post<Week>(`/api/weeks/${weekId}/generate`),
      onSuccess,
    }),
    reopen: useMutation({
      mutationFn: () => api.post<Week>(`/api/weeks/${weekId}/reopen`),
      onSuccess,
    }),
    confirm: useMutation({
      mutationFn: () => api.post<Week>(`/api/weeks/${weekId}/confirm`),
      onSuccess,
    }),
  }
}

export function useAllowedRecipes(weekId: number, mealId: number | null) {
  return useQuery({
    queryKey: keys.allowedRecipes(weekId, mealId ?? 0),
    queryFn: () => api.get<RecipeSummary[]>(`/api/weeks/${weekId}/meals/${mealId}/allowed-recipes`),
    enabled: mealId !== null,
  })
}

export function useRecipes() {
  return useQuery({
    queryKey: keys.recipes,
    queryFn: () => api.get<RecipeSummary[]>('/api/recipes?includeHidden=true'),
  })
}

export function useRecipe(id: number | null) {
  return useQuery({
    queryKey: keys.recipe(id ?? 0),
    queryFn: () => api.get<RecipeDetail>(`/api/recipes/${id}`),
    enabled: id !== null,
  })
}

/** After a catalogue change, everything that shows recipes may be stale. */
function useCatalogueChanged() {
  const client = useQueryClient()
  return () => {
    client.invalidateQueries({ queryKey: keys.recipes })
    client.invalidateQueries({ queryKey: keys.proteins })
    client.invalidateQueries({ queryKey: keys.ingredients })
    client.invalidateQueries({ queryKey: keys.weeks })
  }
}

export function useSaveRecipe() {
  const changed = useCatalogueChanged()
  return useMutation({
    mutationFn: ({ id, input }: { id?: number; input: RecipeInput }) =>
      id === undefined ? api.post<RecipeDetail>('/api/recipes', input) : api.put<RecipeDetail>(`/api/recipes/${id}`, input),
    onSuccess: changed,
  })
}

export function useHideRecipe() {
  const changed = useCatalogueChanged()
  return useMutation({
    mutationFn: ({ id, hidden }: { id: number; hidden: boolean }) =>
      api.patch<RecipeSummary>(`/api/recipes/${id}`, { hidden }),
    onSuccess: changed,
  })
}

export function useSaveIngredient() {
  const changed = useCatalogueChanged()
  return useMutation({
    mutationFn: ({ id, input }: { id?: number; input: IngredientInput }) =>
      id === undefined ? api.post<Ingredient>('/api/ingredients', input) : api.put<Ingredient>(`/api/ingredients/${id}`, input),
    onSuccess: changed,
  })
}

export function useProteins() {
  return useQuery({ queryKey: keys.proteins, queryFn: () => api.get<Protein[]>('/api/proteins'), staleTime: 60_000 })
}

export function useSaveProtein() {
  const changed = useCatalogueChanged()
  return useMutation({
    mutationFn: ({ code, name }: { code?: string; name: string }) =>
      code === undefined ? api.post<Protein>('/api/proteins', { name }) : api.put<Protein>(`/api/proteins/${code}`, { name }),
    onSuccess: changed,
  })
}

export function useDeleteProtein() {
  const changed = useCatalogueChanged()
  return useMutation({
    mutationFn: (code: string) => api.delete(`/api/proteins/${code}`),
    onSuccess: changed,
  })
}

/** Copies a week's menu onto the week starting on the given Monday; returns the target week. */
export function useCopyWeek(sourceWeekId: number) {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (targetWeekStart: string) => api.post<Week>(`/api/weeks/${sourceWeekId}/copy`, { targetWeekStart }),
    onSuccess: (week) => {
      client.setQueryData(keys.week(week.id), week)
      client.invalidateQueries({ queryKey: keys.weeks })
    },
  })
}
