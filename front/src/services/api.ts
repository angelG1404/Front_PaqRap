import type { ArchivoSubido, EscenarioActivoInfo, MapaBase, ParametrosCorrida } from '../types'
import type { NuevoPedido, PedidoRegistrado } from '../types/pedidos'
import type { Snapshot } from '../types/snapshot'
import { validarNuevoPedido } from './validarNuevoPedido'
import { dailySource } from '../mock/dailySource'
import { ALMACENES, GRID_ALTO, GRID_ANCHO, UMBRALES_SEMAFORO } from '../config/defaults'

export const API_URL: string | undefined = import.meta.env.VITE_API_URL
export const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

export class ApiError extends Error {
  status: number
  codigo: string
  mensaje: string

  constructor(status: number, codigo: string, mensaje: string) {
    super(mensaje)
    this.status = status
    this.codigo = codigo
    this.mensaje = mensaje
    this.name = 'ApiError'
  }
}

const mockActiveRuns: Map<string, EscenarioActivoInfo> = new Map([
  ['DIARIO', { runId: 'mock-diario-default', escenario: 'DIARIO', estado: 'EN_CURSO', topic: '/topic/sim/mock-diario-default' }]
])

export async function registrarPedido(p: NuevoPedido): Promise<PedidoRegistrado> {
  const error = validarNuevoPedido(p)
  if (error) throw new Error(error)
  const payload = { ...p, clienteId: p.clienteId.trim() }
  if (USE_MOCK) {
    return dailySource.registrarPedido(payload)
  }
  let response: Response
  try {
    response = await fetch(`${(API_URL ?? '').replace(/\/$/, '')}/api/pedidos`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload),
    })
  } catch { throw new Error('No se pudo conectar con el servidor. Intenta de nuevo.') }
  if (response.status !== 201) {
    const body: any = await response.json().catch(() => null)
    const mensaje = body?.mensaje || body?.error || `No se pudo registrar el pedido (HTTP ${response.status}).`
    const codigo = body?.codigo || 'ERROR'
    throw new ApiError(response.status, codigo, mensaje)
  }
  return response.json() as Promise<PedidoRegistrado>
}

export async function getMapaBase(): Promise<MapaBase> {
  if (USE_MOCK) {
    return {
      ancho: GRID_ANCHO,
      alto: GRID_ALTO,
      almacenes: ALMACENES.map((a) => ({ id: a.id, nombre: a.nombre, x: a.x, y: a.y, tipo: a.id === 'CENTRAL' ? 'CENTRAL' : 'INTERMEDIO', capacidad: a.capacidad })),
      semaforo: UMBRALES_SEMAFORO,
    }
  }
  let response: Response
  try {
    response = await fetch(`${(API_URL ?? '').replace(/\/$/, '')}/api/mapa/base`)
  } catch { throw new Error('No se pudo conectar con el servidor.') }
  if (!response.ok) {
    const body: any = await response.json().catch(() => null)
    throw new ApiError(response.status, body?.codigo || 'ERROR', body?.mensaje || body?.error || 'Error al obtener mapa base')
  }
  return response.json() as Promise<MapaBase>
}

export async function subirArchivoPedidos(file: File): Promise<ArchivoSubido> {
  if (USE_MOCK) {
    return {
      archivoId: `mock-file-${Date.now()}`,
      totalPedidos: 150,
      errores: [],
    }
  }
  const multipart = new FormData()
  multipart.append('file', file)
  let response: Response
  try {
    response = await fetch(`${(API_URL ?? '').replace(/\/$/, '')}/api/archivos/pedidos`, {
      method: 'POST',
      body: multipart,
    })
  } catch { throw new Error('No se pudo conectar con el servidor al subir archivo.') }
  if (!response.ok) {
    const body: any = await response.json().catch(() => null)
    throw new ApiError(response.status, body?.codigo || 'ERROR', body?.mensaje || body?.error || 'Error al subir archivo de pedidos')
  }
  return response.json() as Promise<ArchivoSubido>
}

export async function iniciarEscenario(tipo: ParametrosCorrida['escenario'], params: ParametrosCorrida & { archivoPedidosId?: string }): Promise<EscenarioActivoInfo> {
  if (USE_MOCK) {
    if (mockActiveRuns.has(tipo) && mockActiveRuns.get(tipo)?.estado === 'EN_CURSO') {
      throw new ApiError(409, 'CONFLICT', `Ya hay una corrida activa para el escenario ${tipo}`)
    }
    const runId = `mock-${tipo.toLowerCase()}-${Date.now()}`
    const info: EscenarioActivoInfo = {
      runId,
      escenario: tipo,
      estado: 'EN_CURSO',
      topic: `/topic/sim/${runId}`,
    }
    mockActiveRuns.set(tipo, info)
    return info
  }

  let response: Response
  try {
    response = await fetch(`${(API_URL ?? '').replace(/\/$/, '')}/api/escenarios/${tipo}/iniciar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params),
    })
  } catch { throw new Error('No se pudo conectar con el servidor al iniciar escenario.') }

  if (!response.ok) {
    const body: any = await response.json().catch(() => null)
    const status = response.status
    const mensaje = body?.mensaje || body?.error || (status === 409 ? `Ya hay una corrida activa para ${tipo}` : 'Error al iniciar escenario')
    const codigo = body?.codigo || (status === 409 ? 'CONFLICT' : 'ERROR')
    throw new ApiError(status, codigo, mensaje)
  }
  return response.json() as Promise<EscenarioActivoInfo>
}

export async function getEscenariosActivos(): Promise<EscenarioActivoInfo[]> {
  if (USE_MOCK) {
    return Array.from(mockActiveRuns.values()).filter((r) => r.estado === 'EN_CURSO')
  }
  let response: Response
  try {
    response = await fetch(`${(API_URL ?? '').replace(/\/$/, '')}/api/escenarios/activos`)
  } catch { return [] }
  if (!response.ok) return []
  return response.json() as Promise<EscenarioActivoInfo[]>
}

export async function getEstado(runId: string): Promise<Snapshot> {
  if (USE_MOCK) {
    throw new Error('getEstado no requerido en modo mock directo')
  }
  let response: Response
  try {
    response = await fetch(`${(API_URL ?? '').replace(/\/$/, '')}/api/escenarios/${runId}/estado`)
  } catch { throw new Error('No se pudo obtener el estado del escenario.') }
  if (!response.ok) {
    const body: any = await response.json().catch(() => null)
    throw new ApiError(response.status, body?.codigo || 'ERROR', body?.mensaje || body?.error || 'Error al obtener estado')
  }
  return response.json() as Promise<Snapshot>
}
