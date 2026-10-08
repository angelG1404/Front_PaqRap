import type { EstadoPedido, EstadoVehiculo, Semaforo, TipoVehiculo } from '../types/snapshot'
export const COLORES_TIPO: Record<TipoVehiculo, string> = { AUTO: '#203c61', MOTO: '#f28b28', BICICLETA: '#0a9c70' }
export const COLORES_SEMAFORO: Record<Semaforo, string> = { VERDE: '#10b981', AMBAR: '#f4b323', ROJO: '#ed4655' }
export const NOMBRES_TIPO: Record<TipoVehiculo, string> = { AUTO: 'Carro', MOTO: 'Moto', BICICLETA: 'Bicicleta' }
export const ICONOS_TIPO: Record<TipoVehiculo, string> = { AUTO: 'car', MOTO: 'moto', BICICLETA: 'bike' }
export const NOMBRES_ESTADO_VEHICULO: Record<EstadoVehiculo, string> = { EN_RUTA: 'En ruta', REGRESANDO: 'Regresando', ATENDIENDO: 'Atendiendo', LIBRE: 'Libre' }
export const NOMBRES_ESTADO_PEDIDO: Record<EstadoPedido, string> = { PENDIENTE: 'Pendiente', PLANIFICADO: 'Planificado', EN_RUTA: 'En ruta', ENTREGADO: 'Entregado' }
export const MAP_THEME = { fondo: '#f7f9fb', grilla: '#e4eaf1', borde: '#cbd7e6', almacen: '#072d50', texto: '#233d5f' }
