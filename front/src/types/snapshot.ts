export type TipoVehiculo = 'AUTO' | 'MOTO' | 'BICICLETA'
export type EstadoVehiculo = 'EN_RUTA' | 'REGRESANDO' | 'ATENDIENDO' | 'LIBRE' | 'AVERIADO'
export type EstadoPedido = 'PENDIENTE' | 'PLANIFICADO' | 'EN_RUTA' | 'ENTREGADO'
export type Semaforo = 'VERDE' | 'AMBAR' | 'ROJO'
export type EstadoCorrida = 'EN_CURSO' | 'FINALIZADO' | 'COLAPSADO' | 'DETENIDA'
export type IdAlmacen = 'CENTRAL' | 'INTERMEDIO_NOROESTE' | 'INTERMEDIO_ESTE'
export interface Parada { pedidoId: string; x: number; y: number; eta: string | null; cantidad?: number }
export interface AveriaSnap { tipo: 1 | 2 | 3; desde: string; hasta: string; remolcado: boolean }
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
}
export interface AlmacenSnap { id: IdAlmacen; x: number; y: number; stock: number | null; capacidad?: number | null }
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
  metricas: Metricas
  planificacion?: Planificacion
  causa?: string
  resumen?: ResumenFinal
}
export type EstadoConexion = 'CONECTANDO' | 'CONECTADO' | 'DESCONECTADO' | 'ERROR'
