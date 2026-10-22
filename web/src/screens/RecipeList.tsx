import { useMemo, useState } from 'react'
import { useHideRecipe, useRecipes } from '../api/hooks'
import type { ProteinType } from '../api/types'
import { useProteinName } from '../proteins'
import { ProteinManager } from './ProteinManager'
import { navigate } from '../router'
import { Button, ErrorBox, LunchboxIcon, Spinner, inputClass } from '../ui'

export function RecipeList() {
  const recipes = useRecipes()
  const hide = useHideRecipe()
  const proteinName = useProteinName()
  const [managingProteins, setManagingProteins] = useState(false)
  const [query, setQuery] = useState('')
  const [protein, setProtein] = useState<ProteinType | null>(null)
  const [onlyLunchbox, setOnlyLunchbox] = useState(false)
  const [onlyOwn, setOnlyOwn] = useState(false)
  const [showHidden, setShowHidden] = useState(false)

  const all = useMemo(() => recipes.data ?? [], [recipes.data])
  const proteins = useMemo(() => [...new Set(all.map((r) => r.proteinType))].sort(), [all])
  const shown = useMemo(() => {
    const q = fold(query)
    return all
      .filter((r) => showHidden || !r.hidden)
      .filter((r) => !protein || r.proteinType === protein)
      .filter((r) => !onlyLunchbox || r.portable)
      .filter((r) => !onlyOwn || r.custom)
      .filter((r) => !q || fold(r.name).includes(q))
  }, [all, query, protein, onlyLunchbox, onlyOwn, showHidden])
  const hiddenCount = all.filter((r) => r.hidden).length

  return (
    <div className="mx-auto max-w-5xl space-y-5">
      <header className="flex items-end justify-between">
        <div>
          <h1 className="text-2xl font-semibold">Plats</h1>
          <p className="mt-1 text-stone-600">
            {all.length - hiddenCount} plats disponibles per al menú
            {hiddenCount > 0 && ` · ${hiddenCount} amagats`}. Podeu editar-los tots i afegir-ne de propis.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={() => setManagingProteins(true)}>
            Tipus de proteïna
          </Button>
          <Button onClick={() => navigate({ screen: 'recipe', recipeId: null })}>+ Nou plat</Button>
        </div>
      </header>

      <div className="space-y-3 rounded-2xl bg-white p-4 shadow-sm ring-1 ring-stone-200">
        <input className={inputClass} placeholder="Cerca per nom…" value={query} onChange={(e) => setQuery(e.target.value)} />
        <div className="flex flex-wrap items-center gap-1.5">
          <Chip active={protein === null} onClick={() => setProtein(null)}>
            Tots
          </Chip>
          {proteins.map((p) => (
            <Chip key={p} active={protein === p} onClick={() => setProtein(p)}>
              {proteinName(p)}
            </Chip>
          ))}
          <span className="mx-2 h-5 w-px bg-stone-200" />
          <Chip active={onlyLunchbox} onClick={() => setOnlyLunchbox((v) => !v)}>
            <LunchboxIcon className="size-3.5" /> Per a carmanyola
          </Chip>
          <Chip active={onlyOwn} onClick={() => setOnlyOwn((v) => !v)}>
            Propis
          </Chip>
          <Chip active={showHidden} onClick={() => setShowHidden((v) => !v)}>
            Mostra els amagats
          </Chip>
        </div>
      </div>

      <ErrorBox error={recipes.error ?? hide.error} />
      {recipes.isPending ? (
        <Spinner />
      ) : (
        <div className="overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-stone-200">
          <table className="w-full text-sm">
            <thead className="bg-stone-50 text-left text-stone-600">
              <tr>
                <th className="px-5 py-3 font-medium">Plat</th>
                <th className="px-5 py-3 font-medium">Tipus</th>
                <th className="px-5 py-3 font-medium">Temps</th>
                <th className="px-5 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-100">
              {shown.map((recipe) => (
                <tr key={recipe.id} className={recipe.hidden ? 'bg-stone-50 text-stone-400' : ''}>
                  <td className="px-5 py-3">
                    <button
                      type="button"
                      className="text-left font-medium text-stone-900 hover:text-brand-700 hover:underline"
                      onClick={() => navigate({ screen: 'recipe', recipeId: recipe.id })}
                    >
                      <span className={recipe.hidden ? 'text-stone-400' : ''}>{recipe.name}</span>
                    </button>
                    <div className="mt-0.5 flex flex-wrap gap-1">
                      {recipe.portable && (
                        <Badge className="bg-terracotta-100 text-terracotta-700">
                          <LunchboxIcon className="size-3" /> carmanyola
                        </Badge>
                      )}
                      {recipe.custom && <Badge className="bg-avocado-100 text-avocado-800">propi</Badge>}
                      {recipe.hidden && <Badge className="bg-stone-200 text-stone-600">amagat</Badge>}
                    </div>
                  </td>
                  <td className="px-5 py-3 text-stone-600">{proteinName(recipe.proteinType)}</td>
                  <td className="px-5 py-3 text-stone-600">{recipe.prepMinutes} min</td>
                  <td className="px-5 py-3 text-right whitespace-nowrap">
                    <Button variant="ghost" className="px-2 py-1" onClick={() => navigate({ screen: 'recipe', recipeId: recipe.id })}>
                      Edita
                    </Button>
                    <Button
                      variant={recipe.hidden ? 'secondary' : 'danger'}
                      className="px-2 py-1"
                      disabled={hide.isPending}
                      onClick={() => hide.mutate({ id: recipe.id, hidden: !recipe.hidden })}
                      title={recipe.hidden ? 'Torna a oferir aquest plat' : 'No el proposis ni l’ofereixis més'}
                    >
                      {recipe.hidden ? 'Mostra' : 'Amaga'}
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {shown.length === 0 && <p className="p-8 text-center text-sm text-stone-500">Cap plat no coincideix.</p>}
        </div>
      )}
      {managingProteins && <ProteinManager onClose={() => setManagingProteins(false)} />}
      <p className="text-xs text-stone-500">
        Els plats amagats no surten al generador ni al selector, però les setmanes que ja els tenen els conserven.
      </p>
    </div>
  )
}

function Chip({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={`inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs ring-1 ${
        active ? 'bg-brand-600 text-white ring-brand-600' : 'bg-white text-stone-700 ring-stone-300 hover:bg-stone-50'
      }`}
    >
      {children}
    </button>
  )
}

function Badge({ className, children }: { className: string; children: React.ReactNode }) {
  return <span className={`inline-flex items-center gap-0.5 rounded px-1.5 text-[0.6875rem] ${className}`}>{children}</span>
}

function fold(text: string): string {
  return text.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim()
}
