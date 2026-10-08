export interface ArchivoPedidosInfo {
  nombre: string
  tamanoBytes: number
  totalPedidos: number
  errores: { linea: number; mensaje: string }[]
  valido: boolean
}
export interface ParametrosCorrida {
  escenario: '5D' | 'COLAPSO' | 'DIARIO'
  algoritmo: 'ALNS' | 'ATS'
  fechaInicio: string
  umbralesSemaforo: { verdeMinPct: number; ambarMinPct: number }
  flota: { auto: number; moto: number; bicicleta: number }
}
export interface AlmacenBaseInfo {
  id: string
  nombre: string
  x: number
  y: number
  tipo: string
  capacidad?: number
}
export interface MapaBase {
  ancho: number
  alto: number
  almacenes: AlmacenBaseInfo[]
  semaforo: { verdeMinPct: number; ambarMinPct: number }
}
export interface ArchivoSubido {
  archivoId: string
  totalPedidos: number
  errores: { linea: number; mensaje: string }[]
}
export interface EscenarioActivoInfo {
  runId: string
  escenario: '5D' | 'COLAPSO' | 'DIARIO'
  estado: 'EN_CURSO' | 'FINALIZADO' | 'COLAPSADO'
  topic: string
}
export interface EstadoSimulacion {
  parametros: ParametrosCorrida
  archivoPedidos: File | null
  infoArchivoPedidos: ArchivoPedidosInfo | null
  runIds: { DIARIO?: string; '5D'?: string; COLAPSO?: string }
}
