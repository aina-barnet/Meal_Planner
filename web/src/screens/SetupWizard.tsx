import { useState } from 'react'
import { useDeleteMember, useHousehold, useMembers, useSaveHousehold, useSaveMember } from '../api/hooks'
import type { Member } from '../api/types'
import { ACTIVITY, ALLERGEN, DIET, formatNumber } from '../labels'
import { navigate } from '../router'
import { Button, Card, ErrorBox, Field, Spinner, inputClass } from '../ui'
import { emptyMember, toInput } from '../members'
import { MemberForm } from './MemberForm'

type Step = 1 | 2 | 3

const STEPS: { step: Step; title: string }[] = [
  { step: 1, title: 'La llar' },
  { step: 2, title: 'Qui hi viu' },
  { step: 3, title: 'Necessitats' },
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
        <p className="mt-1 text-stone-600">Tres passos i ja podreu planificar la setmana.</p>
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
      {current === 2 && <MembersStep onDone={() => setStep(3)} />}
      {current === 3 && <RequirementsStep onBack={() => setStep(2)} />}
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
          Continua
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
      <div className="flex items-start justify-between">
        <div>
          <p className="font-medium text-stone-900">{member.name}</p>
          <p className="text-sm text-stone-500">
            {member.requirement.ageYears} anys · {DIET[member.dietType]} · {ACTIVITY[member.activityLevel].label}
          </p>
        </div>
        <span className="rounded-full bg-brand-50 px-2 py-0.5 text-sm font-medium text-brand-800">
          ~{formatNumber(member.requirement.dailyKcal)} kcal
        </span>
      </div>
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

function RequirementsStep({ onBack }: { onBack: () => void }) {
  const members = useMembers()
  if (members.isPending) return <Spinner />
  const list = members.data ?? []

  return (
    <div className="space-y-4">
      <Card className="bg-brand-50 ring-brand-200">
        <p className="text-sm text-brand-900">
          <strong>Estimació orientativa.</strong> Les calories es calculen amb l'equació de Mifflin-St Jeor i el nivell
          d'activitat. Serveixen per equilibrar els menús, no són un consell mèdic. Si teniu una xifra més adequada,
          escriviu-la a mà.
        </p>
      </Card>

      <div className="overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-stone-200">
        <table className="w-full text-sm">
          <thead className="bg-stone-50 text-left text-stone-600">
            <tr>
              <th className="px-5 py-3 font-medium">Membre</th>
              <th className="px-5 py-3 font-medium">Estimació</th>
              <th className="px-5 py-3 font-medium">Xifra manual (kcal/dia)</th>
              <th className="px-5 py-3 font-medium">% al dinar i sopar</th>
              <th className="px-5 py-3 font-medium">Objectiu per àpat</th>
              <th className="px-5 py-3" />
            </tr>
          </thead>
          <tbody className="divide-y divide-stone-100">
            {list.map((member) => (
              <RequirementRow key={member.id} member={member} />
            ))}
          </tbody>
        </table>
      </div>

      <p className="text-xs text-stone-500">
        L'aplicació només planifica dinars i sopars. L'esmorzar i el berenar són personals: el percentatge indica quina
        part de les calories del dia es fa entre dinar i sopar, repartida a parts iguals.
      </p>

      <div className="flex justify-between">
        <Button variant="ghost" onClick={onBack}>
          ← Membres
        </Button>
        <Button onClick={() => navigate({ screen: 'weeks' })}>Planifica la setmana →</Button>
      </div>
    </div>
  )
}

function RequirementRow({ member }: { member: Member }) {
  const save = useSaveMember()
  const [override, setOverride] = useState(member.kcalOverride?.toString() ?? '')
  const [pct, setPct] = useState(member.mainMealsPct)
  const r = member.requirement
  const overrideValue = override.trim() === '' ? null : Number(override)
  const dirty = overrideValue !== member.kcalOverride || pct !== member.mainMealsPct
  const valid = overrideValue === null || (overrideValue >= 500 && overrideValue <= 6000)

  return (
    <tr className="align-top">
      <td className="px-5 py-4">
        <p className="font-medium">{member.name}</p>
        <p className="text-stone-500">{r.ageYears} anys</p>
      </td>
      <td className="px-5 py-4">
        <p>~{formatNumber(r.estimatedDailyKcal)} kcal/dia</p>
        <p className="text-xs text-stone-500">Metabolisme basal {formatNumber(r.basalKcal)} kcal</p>
        {!r.equationValidForAge && (
          <p className="mt-1 max-w-48 text-xs text-terracotta-700">
            L'equació està pensada per a adults: per a menors, millor indicar una xifra a mà.
          </p>
        )}
      </td>
      <td className="px-5 py-4">
        <input
          type="number"
          min={500}
          max={6000}
          step={50}
          placeholder="Sense"
          className={`${inputClass} w-32! shrink-0`}
          value={override}
          onChange={(e) => setOverride(e.target.value)}
        />
        {!valid && <p className="mt-1 text-xs text-terracotta-700">Entre 500 i 6.000</p>}
      </td>
      <td className="px-5 py-4">
        <div className="flex items-center gap-3">
          <input
            type="range"
            min={30}
            max={100}
            step={5}
            value={pct}
            className="w-28 accent-brand-700"
            onChange={(e) => setPct(Number(e.target.value))}
          />
          <span className="w-10 tabular-nums">{pct}%</span>
        </div>
      </td>
      <td className="px-5 py-4">
        <p className="font-medium">~{formatNumber(r.perMainMealKcal)} kcal</p>
        <p className="text-xs text-stone-500">
          sobre {formatNumber(r.dailyKcal)} kcal/dia{r.overridden ? ' (manual)' : ''}
        </p>
      </td>
      <td className="px-5 py-4 text-right">
        <Button
          variant="secondary"
          disabled={!dirty || !valid || save.isPending}
          onClick={() =>
            save.mutate({ id: member.id, input: { ...toInput(member), kcalOverride: overrideValue, mainMealsPct: pct } })
          }
        >
          {save.isPending ? 'Desant…' : 'Desa'}
        </Button>
        {save.error ? <ErrorBox error={save.error} /> : null}
      </td>
    </tr>
  )
}
