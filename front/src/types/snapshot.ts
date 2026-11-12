export type TipoVehiculo = 'AUTO' | 'MOTO' | 'BICICLETA'
export type EstadoVehiculo = 'EN_RUTA' | 'REGRESANDO' | 'ATENDIENDO' | 'LIBRE' | 'AVERIADO'
export type EstadoPedido = 'PENDIENTE' | 'PLANIFICADO' | 'EN_RUTA' | 'ENTREGADO'
export type Semaforo = 'VERDE' | 'AMBAR' | 'ROJO'
export type EstadoCorrida = 'EN_CURSO' | 'FINALIZADO' | 'COLAPSADO' | 'DETENIDA'
export type IdAlmacen = 'CENTRAL' | 'INTERMEDIO_NOROESTE' | 'INTERMEDIO_ESTE'
export interface Parada { pedidoId: string; x: number; y: number; eta: string | null; cantidad?: number }
/** Avería de la unidad en curso (resumen que viaja dentro del vehículo). */
export interface AveriaSnap { id?: string | null; tipo: 1 | 2 | 3; desde: string; hasta: string; remolcado: boolean }
export type EstadoAveria = 'ACTIVA' | 'RESUELTA'
/** Entidad Avería completa: el operador elige unidad y tipo; el motor fija y actualiza el resto. */
export interface AveriaDetalle {
  id: string; vehiculoId: string; tipo: 1 | 2 | 3; estado: EstadoAveria
  ocurrencia: string; ocurrenciaIso: string; fin: string; finIso: string; duracionHoras: number
  origenX: number; origenY: number
  /** Ubicación actual: el lugar de la avería hasta el remolque, luego el almacén central. */
  x: number; y: number
  remolqueEn: string | null; remolcada: boolean; resueltaEn: string | null; pedidosReasignados: number
}
export interface VehiculoSnap {
  id: string; tipo: TipoVehiculo; x: number; y: number; estado: EstadoVehiculo; almacenBase: IdAlmacen
  carga: number; capacidad: number; ruta: [number, number][]; proximaParada: Parada | null; semaforo: Semaforo
  /** Paradas que faltan (incluye las planificadas mientras regresa al almacén). */
  paradas?: Parada[]
  averia?: AveriaSnap | null
  kmRecorridos?: number
}
export interface PedidoSnap {
  id: string; clienteId: string; x: number; y: number; cantidad: number; registradoEn: string; deadline: string
  estado: EstadoPedido; vehiculoId: string | null; semaforo: Semaforo
  eta?: string | null; entregadoEn?: string | null; atrasado?: boolean
  almacenDespacho?: IdAlmacen | null
  despachadoEn?: string | null
}
export interface ActividadResumen { horas: number; enviosDespachados: number; paquetesDespachados: number; enviosEntregados: number; paquetesEntregados: number }
/** Actividad de un almacén en las últimas `horas` horas simuladas, con el detalle de cada envío. */
export interface ActividadAlmacen {
  almacenId: IdAlmacen; horas: number; desde: string; hasta: string; desdeIso: string; hastaIso: string
  enviosDespachados: number; paquetesDespachados: number; enviosEntregados: number; paquetesEntregados: number
  enTransito: number; paquetesEnTransito: number
  despachados: PedidoSnap[]; entregados: PedidoSnap[]; transito: PedidoSnap[]
}
export interface AlmacenSnap { id: IdAlmacen; x: number; y: number; stock: number | null; capacidad?: number | null; actividad?: ActividadResumen }
export interface BloqueoSnap { id: string; puntos: [number, number][]; inicio: string; fin: string }
export interface Metricas {
  pedidosTotal: number; entregados: number; enRuta: number; pendientes: number; atrasados: number
  costoTotal?: number; distanciaKm?: number; flota?: { auto: number; moto: number; bicicleta: number }
  vehiculosOperando?: number; vehiculosAveriados?: number; pedidosPorIngresar?: number
}
export interface ResumenFinal {
  pedidosTotal: number; entregados: number; atrasados: number; noAsignados: number; costoTotal: number
  distanciaKm?: number; cumplimientoPct?: number; simTimeFinal?: string
}
export interface Planificacion { algoritmo: string; enCurso: boolean; ejecuciones: number; ultima?: string; duracionMs?: number; pedidosAsignados?: number; noAsignados?: number }
export interface Snapshot {
  runId: string
  escenario: '5D' | 'COLAPSO' | 'DIARIO'
  estado: EstadoCorrida
  simTime: string
  simTimeIso?: string
  inicioIso?: string
  duracionSeg?: number
  factorVelocidad?: number
  algoritmo?: string
  vehiculos: VehiculoSnap[]
  pedidos: PedidoSnap[]
  almacenes: AlmacenSnap[]
  bloqueos?: BloqueoSnap[]
  /** Todas las averías de la corrida, activas y resueltas, de la más reciente a la más antigua. */
  averias?: AveriaDetalle[]
  metricas: Metricas
  planificacion?: Planificacion
  causa?: string
  resumen?: ResumenFinal
}
export type EstadoConexion = 'CONECTANDO' | 'CONECTADO' | 'DESCONECTADO' | 'ERROR'
