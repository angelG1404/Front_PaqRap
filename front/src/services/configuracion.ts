import { GRID_ALTO, GRID_ANCHO, INICIOS_DE_TURNO } from '../config/defaults'
import type { ConfiguracionCorrida } from '../types'

const dentro = (valor: number, min: number, max: number) => Number.isFinite(valor) && valor >= min && valor <= max

/** Mismas reglas que el backend (que es quien las impone); sirve para avisar antes de enviar. Devuelve el primer problema. */
export function validarConfiguracion(c: ConfiguracionCorrida): string | null {
  const { tipo1, tipo2, tipo3 } = c.averias
  if (!dentro(tipo1.duracionHoras, 0.25, 72)) return 'Avería tipo 1: la duración debe estar entre 0,25 y 72 h.'
  if (!Number.isInteger(tipo2.turnosEspera) || !dentro(tipo2.turnosEspera, 1, 3)) return 'Avería tipo 2: los turnos de espera deben ser 1, 2 o 3.'
  if (!dentro(tipo2.horasRemolque, 0, 48)) return 'Avería tipo 2: las horas hasta el remolque deben estar entre 0 y 48.'
  if (!dentro(tipo3.horasMinimas, 1, 240)) return 'Avería tipo 3: las horas mínimas deben estar entre 1 y 240.'
  if (!(INICIOS_DE_TURNO as readonly number[]).includes(tipo3.horaRetorno)) return 'Avería tipo 3: el turno de regreso debe iniciar a las 7, 15 o 23 h.'
  if (!dentro(tipo3.horasRemolque, 0, 48)) return 'Avería tipo 3: las horas hasta el remolque deben estar entre 0 y 48.'
  for (const a of c.almacenes) {
    if (!Number.isInteger(a.x) || !dentro(a.x, 0, GRID_ANCHO)) return `${a.nombre}: X debe ser un entero entre 0 y ${GRID_ANCHO}.`
    if (!Number.isInteger(a.y) || !dentro(a.y, 0, GRID_ALTO)) return `${a.nombre}: Y debe ser un entero entre 0 y ${GRID_ALTO}.`
    if (a.capacidad !== null && (!Number.isInteger(a.capacidad) || !dentro(a.capacidad, 1, 1_000_000))) return `${a.nombre}: la capacidad debe ser un entero entre 1 y 1 000 000.`
  }
  for (let i = 0; i < c.almacenes.length; i++) for (let j = i + 1; j < c.almacenes.length; j++) {
    if (c.almacenes[i].x === c.almacenes[j].x && c.almacenes[i].y === c.almacenes[j].y) return `${c.almacenes[i].nombre} y ${c.almacenes[j].nombre} no pueden estar en la misma ubicación.`
  }
  for (const v of c.vehiculos) if (!Number.isInteger(v.capacidad) || !dentro(v.capacidad, 1, 1000)) return `Capacidad de ${v.tipo}: debe ser un entero entre 1 y 1000 paquetes.`
  return null
}
