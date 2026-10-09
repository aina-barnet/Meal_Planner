import { useState } from 'react'

/**
 * Text size chosen by the user. Every size in the UI is in rem, so scaling the root font size
 * scales the whole interface. Remembered per browser.
 */
const STEPS = [1, 1.125, 1.25, 1.375, 1.5]
const STORAGE_KEY = 'mida-lletra'

function readStep(): number {
  try {
    const stored = Number(window.localStorage.getItem(STORAGE_KEY))
    return Number.isInteger(stored) && stored >= 0 && stored < STEPS.length ? stored : 0
  } catch {
    return 0
  }
}

function apply(step: number) {
  document.documentElement.style.fontSize = `${STEPS[step] * 100}%`
}

/** Applies the stored size before the first render, so the page does not jump. */
export function applyStoredFontScale() {
  apply(readStep())
}

export function useFontScale() {
  const [step, setStep] = useState(readStep)
  const change = (next: number) => {
    const clamped = Math.max(0, Math.min(STEPS.length - 1, next))
    setStep(clamped)
    apply(clamped)
    try {
      window.localStorage.setItem(STORAGE_KEY, String(clamped))
    } catch {
      // Private mode or storage disabled: the size still applies for this visit.
    }
  }
  return {
    percent: Math.round(STEPS[step] * 100),
    canShrink: step > 0,
    canGrow: step < STEPS.length - 1,
    shrink: () => change(step - 1),
    grow: () => change(step + 1),
    reset: () => change(0),
  }
}
