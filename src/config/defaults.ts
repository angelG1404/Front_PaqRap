import type { ParametrosCorrida } from '../types'
import { COLORES_TIPO } from './theme'

export const GRID_ANCHO = 70
export const GRID_ALTO = 50
export const HORIZONTES_VALIDOS = [4, 8, 12, 18, 36] as const
export const HORIZONTE_5D = { dias: 5, horas: 120 }
export const PORCENTAJE_MAXIMO = 100
export const UMBRALES_SEMAFORO = { verdeMinPct: 60, ambarMinPct: 30 }
export const GRILLA_INTERVALO = 10
export const MOCK_CONFIG = {
  tickMs: 1000, movimientoMs: 3000, minutosPorTick: 30,
  pedidosIniciales: 40, pedidosDiariosIniciales: 6, cantidadMin: 1, cantidadMax: 4,
  pedidosEntregadosIniciales: 4, pedidosPlanificadosIniciales: 5,
  llegadaDiariaSegundos: 20, maxHistorialDiario: 200,
  colapsoMinTicks: 90, colapsoFraccionAtrasados: 0.5,
  stockIntermedioInicial: 820,
  inicioSimuladoMinutos: 480, llegadasColapsoTicks: 6, pedidosPorLlegadaColapso: 4,
  fraccionesAntiguedad: [0, 0.25, 0.5, 0.85],
} as const
export const MAX_ERRORES_VISIBLES = 5
export const EXTENSIONES_PEDIDOS = ['.txt', '.csv']
export const FORMATO_PEDIDO = /^\d{2}d\d{2}h\d{2}m:\d+,\d+,[A-Za-z0-9]+,\d+,\d+$/
export const FLOTA = [
  { id: 'auto', nombre: 'Auto', cantidad: 10, capacidad: 24, velocidad: 40, costoKm: 8, color: COLORES_TIPO.AUTO },
  { id: 'moto', nombre: 'Moto', cantidad: 15, capacidad: 8, velocidad: 25, costoKm: 6, color: COLORES_TIPO.MOTO },
  { id: 'bicicleta', nombre: 'Bicicleta', cantidad: 12, capacidad: 4, velocidad: 12, costoKm: 3, color: COLORES_TIPO.BICICLETA },
] as const
export const ALMACENES = [
  { id: 'CENTRAL', nombre: 'Almacén Central', x: 27, y: 14, capacidad: Infinity, recarga: 'Permanente' },
  { id: 'INTERMEDIO_NOROESTE', nombre: 'Almacén Intermedio Nor-Oeste', x: 12, y: 38, capacidad: 1000, recarga: 'Diaria 23:59:59' },
  { id: 'INTERMEDIO_ESTE', nombre: 'Almacén Intermedio Este', x: 57, y: 27, capacidad: 1000, recarga: 'Diaria 23:59:59' },
] as const
export const MODOS = [
  { id: '5D', nombre: 'Simulación 5D', descripcion: 'Optimización continua regular con recálculo ortogonal' },
  { id: 'COLAPSO', nombre: 'Colapso Logístico', descripcion: 'Prueba de saturación y congelamiento por SLA' },
] as const
export const ALGORITMOS = ['ALNS', 'ATS'] as const
export function crearParametrosIniciales(): ParametrosCorrida {
  const ahora = new Date()
  const local = new Date(ahora.getTime() - ahora.getTimezoneOffset() * 60000).toISOString().slice(0, 16)
  return {
    escenario: '5D', algoritmo: 'ALNS', fechaInicio: local,
    umbralesSemaforo: { ...UMBRALES_SEMAFORO },
    flota: { auto: FLOTA[0].cantidad, moto: FLOTA[1].cantidad, bicicleta: FLOTA[2].cantidad },
  }
}
