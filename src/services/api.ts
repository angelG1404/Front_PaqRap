import type { ArchivoSubido, Escenario, MapaBase, ParametrosCorrida } from '../types'
export const API_URL: string | undefined = import.meta.env.VITE_API_URL
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
