import type { ArchivoSubido, EscenarioActivoInfo, MapaBase, ParametrosCorrida } from '../types'
import type { NuevoPedido, PedidoRegistrado } from '../types/pedidos'
import type { Snapshot } from '../types/snapshot'
import { validarNuevoPedido } from './validarNuevoPedido'
import { dailySource } from '../mock/dailySource'
import { ALMACENES, GRID_ALTO, GRID_ANCHO, UMBRALES_SEMAFORO } from '../config/defaults'

/** Vacía = mismo origen que la página (proxy de Vite en desarrollo o el JAR en producción). */
export const API_URL: string = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')
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

export interface AveriaRegistrada { vehiculoId: string; tipo: number; desde: string; hasta: string; hastaIso: string; pedidosReasignados: number; remolqueAlCentral: boolean }

const mockActiveRuns: Map<string, EscenarioActivoInfo> = new Map([
  ['DIARIO', { runId: 'mock-diario-default', escenario: 'DIARIO', estado: 'EN_CURSO', topic: '/topic/sim/mock-diario-default' }]
])

/** fetch con el contrato de errores del back: { error, mensaje } y status 400/404/409. */
async function solicitar<T>(ruta: string, init: RequestInit | undefined, sinConexion: string, porDefecto: string, esperado?: number): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${API_URL}${ruta}`, init)
  } catch { throw new Error(sinConexion) }
  if (esperado ? response.status !== esperado : !response.ok) {
    const body = await response.json().catch(() => null) as { mensaje?: string; error?: string; codigo?: string } | null
    throw new ApiError(response.status, body?.codigo || body?.error || 'ERROR', body?.mensaje || porDefecto)
  }
  return response.json() as Promise<T>
}

const json = (body: unknown): RequestInit => ({ method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
const archivo = (file: File): RequestInit => { const data = new FormData(); data.append('file', file); return { method: 'POST', body: data } }

export async function registrarPedido(p: NuevoPedido): Promise<PedidoRegistrado> {
  const error = validarNuevoPedido(p)
  if (error) throw new Error(error)
  const payload = { ...p, clienteId: p.clienteId.trim() }
  if (USE_MOCK) return dailySource.registrarPedido(payload)
  return solicitar('/api/pedidos', json(payload), 'No se pudo conectar con el servidor. Intenta de nuevo.', 'No se pudo registrar el pedido.', 201)
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
  return solicitar('/api/mapa/base', undefined, 'No se pudo conectar con el servidor.', 'Error al obtener mapa base')
}

export async function subirArchivoPedidos(file: File): Promise<ArchivoSubido> {
  if (USE_MOCK) return { archivoId: `mock-file-${Date.now()}`, totalPedidos: 150, errores: [] }
  return solicitar('/api/archivos/pedidos', archivo(file), 'No se pudo conectar con el servidor al subir archivo.', 'Error al subir archivo de pedidos')
}

export async function subirArchivoBloqueos(file: File): Promise<ArchivoSubido> {
  if (USE_MOCK) return { archivoId: `mock-blq-${Date.now()}`, totalPedidos: 0, errores: [] }
  return solicitar('/api/archivos/bloqueos', archivo(file), 'No se pudo conectar con el servidor al subir bloqueos.', 'Error al subir archivo de bloqueos')
}

/** Agrega bloqueos a una corrida en curso (tiempos relativos al día 01 de la corrida). */
export async function cargarBloqueosEnCorrida(runId: string, file: File): Promise<ArchivoSubido> {
  if (USE_MOCK) throw new Error('Los bloqueos requieren el backend real (VITE_USE_MOCK=false).')
  return solicitar(`/api/escenarios/${runId}/bloqueos`, archivo(file), 'No se pudo conectar con el servidor.', 'No se pudieron cargar los bloqueos')
}

export async function registrarAveria(runId: string, vehiculoId: string, tipo: 1 | 2 | 3): Promise<AveriaRegistrada> {
  if (USE_MOCK) throw new Error('Las averías requieren el backend real (VITE_USE_MOCK=false).')
  return solicitar(`/api/escenarios/${runId}/averias`, json({ vehiculoId, tipo }), 'No se pudo conectar con el servidor.', 'No se pudo registrar la avería', 201)
}

export async function detenerCorrida(runId: string): Promise<EscenarioActivoInfo> {
  if (USE_MOCK) throw new Error('Detener requiere el backend real (VITE_USE_MOCK=false).')
  return solicitar(`/api/escenarios/${runId}/detener`, { method: 'POST' }, 'No se pudo conectar con el servidor.', 'No se pudo detener la corrida')
}

export async function iniciarEscenario(tipo: ParametrosCorrida['escenario'], params: ParametrosCorrida & { archivoPedidosId?: string; archivoBloqueosId?: string }): Promise<EscenarioActivoInfo> {
  if (USE_MOCK) {
    if (mockActiveRuns.has(tipo) && mockActiveRuns.get(tipo)?.estado === 'EN_CURSO') {
      throw new ApiError(409, 'CONFLICT', `Ya hay una corrida activa para el escenario ${tipo}`)
    }
    const runId = `mock-${tipo.toLowerCase()}-${Date.now()}`
    const info: EscenarioActivoInfo = { runId, escenario: tipo, estado: 'EN_CURSO', topic: `/topic/sim/${runId}` }
    mockActiveRuns.set(tipo, info)
    return info
  }
  return solicitar(`/api/escenarios/${tipo}/iniciar`, json(params), 'No se pudo conectar con el servidor al iniciar escenario.', 'Error al iniciar escenario')
}

export async function getEscenariosActivos(): Promise<EscenarioActivoInfo[]> {
  if (USE_MOCK) return Array.from(mockActiveRuns.values()).filter((r) => r.estado === 'EN_CURSO')
  try {
    return await solicitar('/api/escenarios/activos', undefined, '', '')
  } catch { return [] }
}

export async function getEstado(runId: string): Promise<Snapshot> {
  if (USE_MOCK) throw new Error('getEstado no requerido en modo mock directo')
  return solicitar(`/api/escenarios/${runId}/estado`, undefined, 'No se pudo obtener el estado del escenario.', 'Error al obtener estado')
}
