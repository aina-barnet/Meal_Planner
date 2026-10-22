/** Asks before taking a confirmed week back to draft. */
export function confirmReopen(): boolean {
  return window.confirm(
    [
      'Voleu tornar aquesta setmana a esborrany?',
      '',
      'Podreu tornar a canviar el menú i confirmar-lo de nou.',
    ].join('\n'),
  )
}
