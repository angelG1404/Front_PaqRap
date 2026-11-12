import { ALMACENES } from '../config/defaults'
import type { AlmacenSnap } from '../types/snapshot'

export function compararOcupacion(a: { id: string; carga: number; capacidad: number }, b: { id: string; carga: number; capacidad: number }, orden: 'asc' | 'desc'): number {
  const diferencia = (a.capacidad > 0 ? a.carga / a.capacidad : 0) - (b.capacidad > 0 ? b.carga / b.capacidad : 0)
  return diferencia * (orden === 'asc' ? 1 : -1) || a.id.localeCompare(b.id)
}

export function capacidadAlmacen(almacen: AlmacenSnap): number {
  return almacen.capacidad ?? ALMACENES.find((a) => a.id === almacen.id)?.capacidad ?? 0
}

export function porcentaje(carga: number, capacidad: number): string {
  return capacidad > 0 && Number.isFinite(capacidad)
    ? `${(carga / capacidad * 100).toLocaleString('es-PE', { maximumFractionDigits: 1 })} %` : '—'
}

