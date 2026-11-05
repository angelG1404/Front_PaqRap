import type { EstadoPedido, EstadoVehiculo, Semaforo, TipoVehiculo } from '../types/snapshot'
export const COLORES_TIPO: Record<TipoVehiculo, string> = { AUTO: '#203c61', MOTO: '#f28b28', BICICLETA: '#0a9c70' }
export const COLORES_SEMAFORO: Record<Semaforo, string> = { VERDE: '#10b981', AMBAR: '#f4b323', ROJO: '#ed4655' }
export const NOMBRES_TIPO: Record<TipoVehiculo, string> = { AUTO: 'Carro', MOTO: 'Moto', BICICLETA: 'Bicicleta' }
export const ICONOS_TIPO: Record<TipoVehiculo, string> = { AUTO: 'car', MOTO: 'moto', BICICLETA: 'bike' }
export const NOMBRES_ESTADO_VEHICULO: Record<EstadoVehiculo, string> = { EN_RUTA: 'En ruta', REGRESANDO: 'Regresando', ATENDIENDO: 'Atendiendo', LIBRE: 'Libre', AVERIADO: 'Averiado' }
export const NOMBRES_ESTADO_PEDIDO: Record<EstadoPedido, string> = { PENDIENTE: 'Pendiente', PLANIFICADO: 'Planificado', EN_RUTA: 'En ruta', ENTREGADO: 'Entregado' }
export const NOMBRES_AVERIA: Record<1 | 2 | 3, string> = { 1: 'Tipo 1 (menor, 2 h)', 2: 'Tipo 2 (hasta fin del turno siguiente)', 3: 'Tipo 3 (mayor, 48 h + turno 15:00)' }
export const MAP_THEME = {
  fondo: '#eef2f7', lienzo: '#fbfcfe', grilla: '#e4eaf1', grillaMayor: '#cfd9e6', borde: '#b9c7d9', almacen: '#072d50', texto: '#233d5f',
  bloqueo: '#d62839', averiado: '#c81e33', entregado: '#9aa8ba',
}

export type EstadoCarga = Semaforo | 'VACIO'
export const COLORES_CARGA: Record<EstadoCarga, string> = { VERDE: '#087f5b', AMBAR: '#a86500', ROJO: '#c92a3b', VACIO: '#64748b' }
export const NOMBRES_CARGA: Record<EstadoCarga, string> = { VERDE: 'Alta', AMBAR: 'Media', ROJO: 'Baja', VACIO: 'Vacío' }
export const COLORES_AVERIA: Record<1 | 2 | 3, string> = { 1: '#7c3aed', 2: '#c2410c', 3: '#be185d' }
export function estadoCarga(vehicle: { carga: number; capacidad: number }): EstadoCarga {
  if (vehicle.carga <= 0) return 'VACIO'
  const porcentaje = vehicle.capacidad > 0 ? vehicle.carga / vehicle.capacidad * 100 : 0
  return porcentaje > 50 ? 'VERDE' : porcentaje >= 25 ? 'AMBAR' : 'ROJO'
}
