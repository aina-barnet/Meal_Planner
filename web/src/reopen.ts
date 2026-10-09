/** Asks before taking a confirmed week back to draft, explaining what it undoes. */
export function confirmReopen(): boolean {
  return window.confirm(
    [
      'Voleu tornar aquesta setmana a esborrany?',
      '',
      "· La llista de la compra s'esborrarà (i les marques que hi hàgiu fet).",
      '· El rebost tornarà a com estava abans de confirmar.',
      '· Podreu tornar a canviar el menú i confirmar-lo de nou.',
    ].join('\n'),
  )
}
