import { useState } from 'react'
import { useDeleteMember, useHousehold, useMembers, useSaveHousehold, useSaveMember } from '../api/hooks'
import type { Member } from '../api/types'
import { ALLERGEN, DIET } from '../labels'
import { navigate } from '../router'
import { Button, Card, ErrorBox, Field, Spinner, inputClass } from '../ui'
import { emptyMember, toInput } from '../members'
import { MemberForm } from './MemberForm'

type Step = 1 | 2

const STEPS: { step: Step; title: string }[] = [
  { step: 1, title: 'La llar' },
  { step: 2, title: 'Qui hi viu' },
]

export function SetupWizard() {
  const household = useHousehold()
  const [step, setStep] = useState<Step | null>(null)

  if (household.isPending) return <Spinner />
  const current: Step = step ?? (household.data ? 2 : 1)
  const reachable = (s: Step) => s === 1 || household.data != null

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <header>
        <h1 className="text-2xl font-semibold text-stone-900">Configuració de la llar</h1>
        <p className="mt-1 text-stone-600">Dos passos i ja podreu planificar la setmana.</p>
      </header>

      <ol className="flex gap-2">
        {STEPS.map(({ step: s, title }) => (
          <li key={s} className="flex-1">
            <button
              type="button"
              disabled={!reachable(s)}
              onClick={() => setStep(s)}
              className={`w-full rounded-xl px-4 py-3 text-left transition-colors ${
                current === s
                  ? 'bg-brand-600 text-white'
                  : reachable(s)
                    ? 'bg-white text-stone-700 ring-1 ring-stone-200 hover:bg-stone-50'
                    : 'bg-stone-50 text-stone-400 ring-1 ring-stone-200'
              }`}
            >
              <span className="text-xs uppercase tracking-wide opacity-75">Pas {s}</span>
              <span className="block font-medium">{title}</span>
            </button>
          </li>
        ))}
      </ol>

      {current === 1 && <HouseholdStep initialName={household.data?.name ?? ''} onDone={() => setStep(2)} />}
      {current === 2 && <MembersStep onDone={() => navigate({ screen: 'weeks' })} />}
    </div>
  )
}

function HouseholdStep({ initialName, onDone }: { initialName: string; onDone: () => void }) {
  const [name, setName] = useState(initialName)
  const save = useSaveHousehold()
  return (
    <Card>
      <form
        className="max-w-md space-y-4"
        onSubmit={(e) => {
          e.preventDefault()
          if (name.trim()) save.mutate(name.trim(), { onSuccess: onDone })
        }}
      >
        <Field label="Com es diu la vostra llar?" hint="Per exemple, «Casa Puig» o «Pis de Gràcia».">
          <input className={inputClass} value={name} autoFocus onChange={(e) => setName(e.target.value)} />
        </Field>
        <ErrorBox error={save.error} />
        <Button type="submit" disabled={!name.trim() || save.isPending}>
          Continua
        </Button>
      </form>
    </Card>
  )
}

function MembersStep({ onDone }: { onDone: () => void }) {
  const members = useMembers()
  const save = useSaveMember()
  const remove = useDeleteMember()
  // null: adding a new member; a number: editing that member; undefined: form closed.
  const [editing, setEditing] = useState<number | null | undefined>(undefined)
  const list = members.data ?? []
  const formOpen = editing !== undefined || (members.isSuccess && list.length === 0)
  const editingMember = typeof editing === 'number' ? list.find((m) => m.id === editing) : undefined

  return (
    <div className="grid grid-cols-[22rem_1fr] gap-6">
      <div className="space-y-3">
        {members.isPending && <Spinner />}
        {list.map((member) => (
          <MemberCard
            key={member.id}
            member={member}
            active={editing === member.id}
            onEdit={() => {
              save.reset()
              setEditing(member.id)
            }}
            onDelete={() => {
              if (window.confirm(`Segur que voleu treure ${member.name} de la llar?`)) remove.mutate(member.id)
            }}
          />
        ))}
        <Button
          variant="secondary"
          className="w-full"
          onClick={() => {
            save.reset()
            setEditing(null)
          }}
        >
          + Afegeix un membre
        </Button>
        <Button className="w-full" disabled={list.length === 0} onClick={onDone}>
          Planifica la setmana →
        </Button>
      </div>

      <Card>
        {formOpen ? (
          <>
            <h2 className="mb-4 text-lg font-semibold">{editingMember ? `Edita ${editingMember.name}` : 'Nou membre'}</h2>
            <MemberForm
              key={editing ?? 'new'}
              initial={editingMember ? toInput(editingMember) : emptyMember()}
              saving={save.isPending}
              error={save.error}
              onCancel={list.length > 0 ? () => setEditing(undefined) : undefined}
              onSubmit={(input) =>
                save.mutate({ id: editingMember?.id, input }, { onSuccess: () => setEditing(undefined) })
              }
            />
          </>
        ) : (
          <div className="flex h-full flex-col items-center justify-center py-16 text-center text-stone-500">
            <p>Seleccioneu un membre per editar-lo o afegiu-ne un de nou.</p>
          </div>
        )}
      </Card>
    </div>
  )
}

function MemberCard({
  member,
  active,
  onEdit,
  onDelete,
}: {
  member: Member
  active: boolean
  onEdit: () => void
  onDelete: () => void
}) {
  return (
    <div className={`rounded-xl bg-white p-4 ring-1 ${active ? 'ring-2 ring-brand-600' : 'ring-stone-200'}`}>
      <p className="font-medium text-stone-900">{member.name}</p>
      <p className="text-sm text-stone-500">{DIET[member.dietType]}</p>
      {member.allergens.length > 0 && (
        <p className="mt-2 text-xs text-terracotta-700">
          Sense: {member.allergens.map((a) => ALLERGEN[a.allergen].toLowerCase()).join(', ')}
        </p>
      )}
      <div className="mt-3 flex gap-1">
        <Button variant="ghost" className="px-2 py-1" onClick={onEdit}>
          Edita
        </Button>
        <Button variant="danger" className="px-2 py-1" onClick={onDelete}>
          Treu
        </Button>
      </div>
    </div>
  )
}
