import { useEffect, useState } from 'react'
import { useDeleteProtein, useProteins, useSaveProtein } from '../api/hooks'
import type { Protein } from '../api/types'
import { Button, ErrorBox, inputClass } from '../ui'

/** Lists protein types; any can be renamed, and deleted while no recipe uses it. */
export function ProteinManager({ onClose }: { onClose: () => void }) {
  const proteins = useProteins()
  const save = useSaveProtein()
  const remove = useDeleteProtein()
  const [name, setName] = useState('')

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-stone-900/40 p-8" onClick={onClose}>
      <div role="dialog" aria-modal="true" aria-label="Tipus de proteïna"
        className="flex max-h-[85vh] w-full max-w-lg flex-col overflow-hidden rounded-2xl bg-white shadow-2xl"
        onClick={(e) => e.stopPropagation()}>
        <div className="border-b border-stone-200 px-6 py-4">
          <h2 className="text-lg font-semibold">Tipus de proteïna</h2>
          <p className="text-xs text-stone-500">
            El generador evita repetir el mateix tipus en dies seguits. Podeu afegir-ne, canviar-ne el nom i
            esborrar els que no facin servir cap plat.
          </p>
        </div>
        <ul className="flex-1 divide-y divide-stone-100 overflow-y-auto px-6">
          {(proteins.data ?? []).map((p) => (
            <ProteinRow key={p.code} protein={p}
              onRename={(newName) => save.mutate({ code: p.code, name: newName })}
              onDelete={() => remove.mutate(p.code)} />
          ))}
        </ul>
        <form className="space-y-2 border-t border-stone-200 px-6 py-4"
          onSubmit={(e) => {
            e.preventDefault()
            if (name.trim()) save.mutate({ name: name.trim() }, { onSuccess: () => setName('') })
          }}>
          <div className="flex gap-2">
            <input className={inputClass} placeholder="Nou tipus, p. ex. «Tofu i seitan»" value={name} onChange={(e) => setName(e.target.value)} />
            <Button type="submit" disabled={!name.trim() || save.isPending}>Afegeix</Button>
          </div>
          <ErrorBox error={save.error ?? remove.error} />
          <div className="flex justify-end">
            <Button variant="ghost" onClick={onClose}>Tanca</Button>
          </div>
        </form>
      </div>
    </div>
  )
}

function ProteinRow({ protein, onRename, onDelete }: { protein: Protein; onRename: (name: string) => void; onDelete: () => void }) {
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState(protein.name)
  return (
    <li className="flex items-center gap-3 py-2.5 text-sm">
      {editing ? (
        <input className={`${inputClass} flex-1`} autoFocus value={name} onChange={(e) => setName(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && name.trim()) {
              onRename(name.trim())
              setEditing(false)
            }
            if (e.key === 'Escape') setEditing(false)
          }} />
      ) : (
        <span className="flex-1">
          {protein.name}
          {protein.custom && <span className="ml-2 rounded bg-avocado-100 px-1.5 text-xs text-avocado-800">propi</span>}
        </span>
      )}
      <span className="text-xs text-stone-500">{protein.recipes === 1 ? '1 plat' : `${protein.recipes} plats`}</span>
      {!editing && (
        <>
          <Button variant="ghost" className="px-2 py-1" onClick={() => setEditing(true)}>Canvia el nom</Button>
          <Button variant="danger" className="px-2 py-1" disabled={protein.recipes > 0} onClick={onDelete}
            title={protein.recipes > 0 ? 'Hi ha plats que la fan servir' : undefined}>
            Esborra
          </Button>
        </>
      )}
      {editing && (
        <Button variant="secondary" className="px-2 py-1" disabled={!name.trim()} onClick={() => {
          onRename(name.trim())
          setEditing(false)
        }}>
          Desa
        </Button>
      )}
    </li>
  )
}
