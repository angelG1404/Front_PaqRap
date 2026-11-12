import { capacidadAlmacen, porcentaje } from '../../services/ocupacion'
import { COLORES_CARGA, NOMBRES_CARGA, estadoCarga } from '../../config/theme'
import type { Snapshot } from '../../types/snapshot'

export function NivelCarga({ carga, capacidad }: { carga: number; capacidad: number }) {
  const nivel = estadoCarga({ carga, capacidad })
  return <span className="stock-signal" style={{ color: COLORES_CARGA[nivel] }} title="Alta: más de 50 %. Media: 25–50 %. Baja: menos de 25 %. Vacío: 0."><i style={{ background: COLORES_CARGA[nivel] }} />{NOMBRES_CARGA[nivel]}</span>
}

export function Ocupacion({ snapshot }: { snapshot: Snapshot | null }) {
  const flota = snapshot?.vehiculos ?? []
  const carga = flota.reduce((sum, v) => sum + v.carga, 0)
  const capacidad = flota.reduce((sum, v) => sum + v.capacidad, 0)
  const almacenes = snapshot?.almacenes.filter((a) => a.stock !== null && Number.isFinite(capacidadAlmacen(a))) ?? []
  const stock = almacenes.reduce((sum, a) => sum + (a.stock ?? 0), 0)
  const limite = almacenes.reduce((sum, a) => sum + capacidadAlmacen(a), 0)
  return <div className="occupancy-cards" aria-label="Ocupación global">
    <section className="occupancy-card" aria-label="Ocupación de flota">
      <div><strong>Ocupación de flota</strong><span>{snapshot ? `${carga.toLocaleString('es-PE')} / ${capacidad.toLocaleString('es-PE')} paquetes` : 'Esperando datos…'}</span></div>
      <div><b>{snapshot ? porcentaje(carga, capacidad) : '—'}</b>{snapshot && capacidad > 0 ? <NivelCarga carga={carga} capacidad={capacidad} /> : <small>Sin capacidad disponible</small>}</div>
    </section>
    <section className="occupancy-card" aria-label="Stock de almacenes intermedios">
      <div><strong>Stock de almacenes intermedios</strong><span>{snapshot ? `${stock.toLocaleString('es-PE')} / ${limite.toLocaleString('es-PE')} paquetes` : 'Esperando datos…'}{snapshot?.almacenes.some((a) => a.stock === null) ? ' · Central: ilimitado' : ''}</span></div>
      <div><b>{snapshot ? porcentaje(stock, limite) : '—'}</b>{snapshot && limite > 0 ? <NivelCarga carga={stock} capacidad={limite} /> : <small>Sin capacidad disponible</small>}</div>
    </section>
  </div>
}
