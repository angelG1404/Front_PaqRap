import type { ArchivoSubido, Escenario, MapaBase, ParametrosCorrida } from '../types'
import type { NuevoPedido, PedidoRegistrado } from '../types/pedidos'
import { validarNuevoPedido } from './validarNuevoPedido'
import { dailySource } from '../mock/dailySource'
export const API_URL: string | undefined = import.meta.env.VITE_API_URL
export const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

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
    const body: unknown = await response.json().catch(() => null)
    const message = body && typeof body === 'object' && ('message' in body ? body.message : 'mensaje' in body ? body.mensaje : null)
    throw new Error(typeof message === 'string' ? message : `No se pudo registrar el pedido (HTTP ${response.status}).`)
  }
  return response.json() as Promise<PedidoRegistrado>
}
export async function getMapaBase(): Promise<MapaBase> { throw new Error('no implementado') }
/** Contrato: multipart/form-data, campo "file". No realiza solicitudes. */
export async function subirArchivoPedidos(file: File): Promise<ArchivoSubido> {
  const multipart = new FormData()
  multipart.append('file', file)
  throw new Error('no implementado')
}
export async function iniciarEscenario(tipo: ParametrosCorrida['escenario'], params: ParametrosCorrida): Promise<Escenario> {
  void tipo
  void params
  throw new Error('no implementado')
}
export async function getEscenarioActivo(): Promise<Escenario | null> { throw new Error('no implementado') }
