export interface ArchivoPedidosInfo {
  nombre: string
  tamanoBytes: number
  totalPedidos: number
  errores: { linea: number; mensaje: string }[]
  valido: boolean
}
/** Reglas de los tres tipos de avería (las que registre el operador desde que se aplican). */
export interface ConfigAverias {
  tipo1: { duracionHoras: number }
  tipo2: { turnosEspera: number; horasRemolque: number }
  tipo3: { horasMinimas: number; horaRetorno: number; horasRemolque: number }
}
/** Ubicación y capacidad de un almacén; la capacidad es null en el central (inventario infinito). */
export interface ConfigAlmacen { id: string; nombre: string; x: number; y: number; capacidad: number | null }
/** Capacidad de un tipo de unidad; velocidad y costo se muestran pero no se modifican. */
export interface ConfigVehiculo { tipo: 'auto' | 'moto' | 'bicicleta'; capacidad: number; velocidadKmH?: number; costoPorKm?: number }
export interface ConfiguracionCorrida { averias: ConfigAverias; almacenes: ConfigAlmacen[]; vehiculos: ConfigVehiculo[] }
export interface ParametrosCorrida {
  escenario: '5D' | 'COLAPSO' | 'DIARIO'
  algoritmo: 'ALNS' | 'ATS'
  fechaInicio: string
  umbralesSemaforo: { verdeMinPct: number; ambarMinPct: number }
  flota: { auto: number; moto: number; bicicleta: number }
  configuracion: ConfiguracionCorrida
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
  estado: 'EN_CURSO' | 'FINALIZADO' | 'COLAPSADO' | 'DETENIDA'
  topic: string
}
export interface EstadoSimulacion {
  parametros: ParametrosCorrida
  archivoPedidos: File | null
  infoArchivoPedidos: ArchivoPedidosInfo | null
  /** Opcional: bloqueos viales de la simulación (totalPedidos = bloqueos válidos). */
  archivoBloqueos: File | null
  infoArchivoBloqueos: ArchivoPedidosInfo | null
  runIds: { DIARIO?: string; '5D'?: string; COLAPSO?: string }
}
