import { useMemo } from 'react'
import { useProteins } from './api/hooks'

/** Display name of a protein type code, from the catalogue's list (seeded and the family's own). */
export function useProteinName(): (code: string) => string {
  const proteins = useProteins()
  return useMemo(() => {
    const names = new Map((proteins.data ?? []).map((p) => [p.code, p.name]))
    return (code: string) => names.get(code) ?? code
  }, [proteins.data])
}
