/** Tiempo del contrato: día 01 es el inicio de la corrida. */
export function formatSimTime(minutes: number): string {
  const whole = Math.max(0, Math.floor(minutes))
  return `${String(Math.floor(whole / 1440) + 1).padStart(2, '0')}d${String(Math.floor(whole % 1440 / 60)).padStart(2, '0')}h${String(whole % 60).padStart(2, '0')}m`
}
export function parseSimTime(time: string): number {
  const match = /^(\d+)d(\d{2})h(\d{2})m$/.exec(time)
  return match ? (Number(match[1]) - 1) * 1440 + Number(match[2]) * 60 + Number(match[3]) : 0
}
export function readableSimTime(time: string): string {
  const match = /^(\d+)d(\d{2})h(\d{2})m$/.exec(time)
  return match ? `Día ${Number(match[1])} · ${match[2]}:${match[3]}` : time
}
export function localIso(date: Date): string {
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 19)
}
export function formatDuration(seconds: number): string {
  return [Math.floor(seconds / 3600), Math.floor(seconds % 3600 / 60), seconds % 60].map((part) => String(part).padStart(2, '0')).join(':')
}

export function formatFechaHora(iso?: string): string {
  if (!iso) return 'Esperando hora…'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return 'Fecha no disponible'
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

/** Fecha y hora a nivel de minuto: 04/11/2026 23:14. */
export function formatFechaHoraMinuto(iso?: string): string {
  const texto = formatFechaHora(iso)
  return /\d{2}:\d{2}:\d{2}$/.test(texto) ? texto.slice(0, -3) : texto
}

/** Tiempo entre dos instantes, a nivel de minuto: "2 d 05 h 37 min". */
export function formatTranscurrido(desdeIso?: string, hastaIso?: string): string {
  if (!desdeIso || !hastaIso) return '—'
  const desde = new Date(desdeIso).getTime(), hasta = new Date(hastaIso).getTime()
  if (Number.isNaN(desde) || Number.isNaN(hasta)) return '—'
  const minutos = Math.max(0, Math.floor((hasta - desde) / 60000))
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${Math.floor(minutos / 1440)} d ${pad(Math.floor(minutos % 1440 / 60))} h ${pad(minutos % 60)} min`
}
