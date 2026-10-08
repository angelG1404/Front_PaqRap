export type TipoVehiculo = 'AUTO' | 'MOTO' | 'BICICLETA'
export type EstadoVehiculo = 'EN_RUTA' | 'REGRESANDO' | 'ATENDIENDO' | 'LIBRE'
export type EstadoPedido = 'PENDIENTE' | 'PLANIFICADO' | 'EN_RUTA' | 'ENTREGADO'
export type Semaforo = 'VERDE' | 'AMBAR' | 'ROJO'
export type EstadoCorrida = 'EN_CURSO' | 'FINALIZADO' | 'COLAPSADO'
export type IdAlmacen = 'CENTRAL' | 'INTERMEDIO_NOROESTE' | 'INTERMEDIO_ESTE'
export interface Parada { pedidoId: string; x: number; y: number; eta: string }
export interface VehiculoSnap { id: string; tipo: TipoVehiculo; x: number; y: number; estado: EstadoVehiculo; almacenBase: IdAlmacen; carga: number; capacidad: number; ruta: [number, number][]; proximaParada: Parada | null; semaforo: Semaforo }
export interface PedidoSnap { id: string; clienteId: string; x: number; y: number; cantidad: number; registradoEn: string; deadline: string; estado: EstadoPedido; vehiculoId: string | null; semaforo: Semaforo }
export interface AlmacenSnap { id: IdAlmacen; x: number; y: number; stock: number | null }
export interface Metricas { pedidosTotal: number; entregados: number; enRuta: number; pendientes: number; atrasados: number }
export interface ResumenFinal { pedidosTotal: number; entregados: number; atrasados: number; noAsignados: number; costoTotal: number }
export interface Snapshot {
  runId: string
  escenario: '5D' | 'COLAPSO' | 'DIARIO'
  estado: EstadoCorrida
  simTime: string
  simTimeIso?: string
  vehiculos: VehiculoSnap[]
  pedidos: PedidoSnap[]
  almacenes: AlmacenSnap[]
  metricas: Metricas
  causa?: string
  resumen?: ResumenFinal
}
export type EstadoConexion = 'CONECTANDO' | 'CONECTADO' | 'DESCONECTADO' | 'ERROR'
