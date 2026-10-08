export interface ArchivoPedidosInfo {
  nombre: string
  tamanoBytes: number
  totalPedidos: number
  errores: { linea: number; mensaje: string }[]
  valido: boolean
}
export interface ParametrosCorrida {
  escenario: '5D' | 'COLAPSO'
  algoritmo: 'ALNS' | 'ATS'
  fechaInicio: string
  umbralesSemaforo: { verdeMinPct: number; ambarMinPct: number }
  flota: { auto: number; moto: number; bicicleta: number }
}
export interface EstadoSimulacion {
  parametros: ParametrosCorrida
  archivoPedidos: File | null
  infoArchivoPedidos: ArchivoPedidosInfo | null
}
// Contratos preliminares, pendientes de acordar con el backend.
export interface MapaBase { ancho: number; alto: number }
export interface ArchivoSubido { id: string }
export interface Escenario { id: string; tipo: ParametrosCorrida['escenario'] }
