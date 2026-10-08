import { GRID_ALTO, GRID_ANCHO, HORIZONTES_VALIDOS } from '../config/defaults'
import type { NuevoPedido } from '../types/pedidos'

export function validarNuevoPedido(p: NuevoPedido): string | null {
  if (!Number.isInteger(p.x) || p.x < 0 || p.x > GRID_ANCHO) return `La coordenada X debe ser un entero entre 0 y ${GRID_ANCHO}.`
  if (!Number.isInteger(p.y) || p.y < 0 || p.y > GRID_ALTO) return `La coordenada Y debe ser un entero entre 0 y ${GRID_ALTO}.`
  if (!p.clienteId.trim()) return 'El ID de cliente es obligatorio.'
  if (!Number.isSafeInteger(p.cantidad) || p.cantidad <= 0) return 'La cantidad debe ser un entero mayor que cero.'
  if (!HORIZONTES_VALIDOS.some((h) => h === p.horizonteHoras)) return 'Selecciona un plazo de entrega válido.'
  return null
}
