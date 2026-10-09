import { useEffect, useState } from 'react'

/**
 * Hash-based routes. Four screens do not justify a routing library:
 *   #/llar              setup wizard
 *   #/rebost            pantry
 *   #/plats             recipe list
 *   #/plats/nou         new recipe
 *   #/plats/:id         recipe editor
 *   #/setmana           week list / new week
 *   #/setmana/:id       week configuration
 *   #/menu/:id          menu board
 *   #/compra/:id        shopping list
 */
export type Route =
  | { screen: 'household' }
  | { screen: 'pantry' }
  | { screen: 'recipes' }
  | { screen: 'recipe'; recipeId: number | null }
  | { screen: 'weeks' }
  | { screen: 'week'; weekId: number }
  | { screen: 'menu'; weekId: number }
  | { screen: 'shopping'; weekId: number }

export function parseRoute(hash: string): Route | null {
  const parts = hash.replace(/^#\/?/, '').split('/').filter(Boolean)
  const id = Number(parts[1])
  switch (parts[0]) {
    case 'llar':
      return { screen: 'household' }
    case 'rebost':
      return { screen: 'pantry' }
    case 'plats':
      if (parts[1] === 'nou') return { screen: 'recipe', recipeId: null }
      return Number.isInteger(id) && id > 0 ? { screen: 'recipe', recipeId: id } : { screen: 'recipes' }
    case 'setmana':
      return Number.isInteger(id) && id > 0 ? { screen: 'week', weekId: id } : { screen: 'weeks' }
    case 'menu':
      return Number.isInteger(id) && id > 0 ? { screen: 'menu', weekId: id } : { screen: 'weeks' }
    case 'compra':
      return Number.isInteger(id) && id > 0 ? { screen: 'shopping', weekId: id } : { screen: 'weeks' }
    default:
      return null
  }
}

export function href(route: Route): string {
  switch (route.screen) {
    case 'household':
      return '#/llar'
    case 'pantry':
      return '#/rebost'
    case 'recipes':
      return '#/plats'
    case 'recipe':
      return route.recipeId === null ? '#/plats/nou' : `#/plats/${route.recipeId}`
    case 'weeks':
      return '#/setmana'
    case 'week':
      return `#/setmana/${route.weekId}`
    case 'menu':
      return `#/menu/${route.weekId}`
    case 'shopping':
      return `#/compra/${route.weekId}`
  }
}

export function navigate(route: Route) {
  window.location.hash = href(route)
}

export function useRoute(): Route | null {
  const [route, setRoute] = useState(() => parseRoute(window.location.hash))
  useEffect(() => {
    const onChange = () => setRoute(parseRoute(window.location.hash))
    window.addEventListener('hashchange', onChange)
    return () => window.removeEventListener('hashchange', onChange)
  }, [])
  return route
}
