import { ALMACENES } from '../../config/defaults'
import { readableSimTime } from '../../services/simulationTime'
import type { Snapshot } from '../../types/snapshot'
import { NivelCarga } from './Ocupacion'
import { capacidadAlmacen, porcentaje } from '../../services/ocupacion'

export function AlmacenesPanel({ snapshot, selectedId, onSelect }: { snapshot: Snapshot | null; selectedId: string | null; onSelect: (id: string) => void }) {
  const selected = snapshot?.almacenes.find((a) => a.id === selectedId)
  const pedidos = snapshot?.pedidos.filter((p) => !!selected && p.estado === 'EN_RUTA' && p.almacenDespacho === selected.id) ?? []
  return <div role="tabpanel" id="warehouses-list" aria-labelledby="warehouses-tab">
    <div className="entity-list warehouse-list">
      {snapshot?.almacenes.map((a) => <button type="button" key={a.id} className={`entity-row ${selectedId === a.id ? 'selected' : ''}`} aria-pressed={selectedId === a.id} onClick={() => onSelect(a.id)}>
        <span className="entity-main"><strong>{ALMACENES.find((item) => item.id === a.id)?.nombre ?? a.id}</strong>
          <span>{a.stock === null ? 'Stock ilimitado' : `${a.stock.toLocaleString('es-PE')} / ${capacidadAlmacen(a).toLocaleString('es-PE')} paquetes · ${porcentaje(a.stock, capacidadAlmacen(a))}`}</span>
          {a.stock !== null && <NivelCarga carga={a.stock} capacidad={capacidadAlmacen(a)} />}
        </span>
      </button>)}
      {!snapshot && <p className="no-results">Esperando almacenes…</p>}
      {selected ? <section className="warehouse-transit" aria-label="Envíos en tránsito del almacén">
        <h3>En tránsito desde {ALMACENES.find((a) => a.id === selected.id)?.nombre ?? selected.id}</h3>
        <p>{pedidos.length} envíos · {pedidos.reduce((sum, p) => sum + p.cantidad, 0)} paquetes</p>
        {pedidos.map((p) => <article key={p.id} className="warehouse-order"><strong>{p.id} · {p.cantidad} paq.</strong><span>Unidad: {p.vehiculoId ?? '—'}</span><span>Cliente: {p.clienteId} · Destino: ({p.x}, {p.y})</span><span>Llegada estimada: {p.eta ? readableSimTime(p.eta) : 'Sin estimación'}</span></article>)}
        {!pedidos.length && <p className="no-results">Sin envíos en tránsito desde este almacén.</p>}
      </section> : <p className="no-results">Selecciona un almacén para consultar sus envíos en tránsito.</p>}
    </div>
  </div>
}
