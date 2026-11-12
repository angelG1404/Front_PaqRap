import { useEffect, useRef, useState } from 'react'
import { ALMACENES } from '../../config/defaults'
import { useActividadAlmacen } from '../../hooks/useActividadAlmacen'
import { readableSimTime } from '../../services/simulationTime'
import type { PedidoSnap, Snapshot } from '../../types/snapshot'
import { NivelCarga } from './Ocupacion'
import { capacidadAlmacen, porcentaje } from '../../services/ocupacion'

type Vista = 'despachados' | 'entregados' | 'transito'
const hora = (simTime?: string | null) => simTime ? readableSimTime(simTime).replace(/^Día (\d+) · /, 'D$1 ') : '—'

function Envio({ p, vista }: { p: PedidoSnap; vista: Vista }) {
  const momento = vista === 'entregados' ? `Entregado ${hora(p.entregadoEn)}` : vista === 'despachados' ? `Salió ${hora(p.despachadoEn)}${p.estado === 'ENTREGADO' ? ' · entregado' : ''}` : `Salió ${hora(p.despachadoEn)}`
  return <article className="warehouse-order"><strong>{p.id} · {p.cantidad} paq.</strong>
    <span>Unidad {p.vehiculoId ?? '—'} · Cliente {p.clienteId} · Destino ({p.x}, {p.y})</span>
    <span>{momento}{vista === 'transito' ? ` · Llegada estimada: ${p.eta ? hora(p.eta) : 'sin estimación'}` : ''}</span></article>
}

/**
 * Lista de almacenes enlazada con el mapa: elegir uno aquí lo resalta en el mapa y elegir uno en el mapa lo
 * marca aquí. Para el almacén elegido muestra los envíos y paquetes entregados de las últimas horas simuladas.
 */
export function AlmacenesPanel({ snapshot, selectedId, onSelect }: { snapshot: Snapshot | null; selectedId: string | null; onSelect: (id: string | null) => void }) {
  const selected = snapshot?.almacenes.find((a) => a.id === selectedId)
  const [vista, setVista] = useState<Vista>('despachados')
  const { datos, error, cargando } = useActividadAlmacen(snapshot?.runId, selected?.id ?? null)
  const fila = useRef<HTMLButtonElement | null>(null)
  // Si la selección vino del mapa, la fila elegida se trae a la vista.
  useEffect(() => { fila.current?.scrollIntoView({ block: 'nearest' }) }, [selectedId])
  const horas = datos?.horas ?? snapshot?.almacenes[0]?.actividad?.horas ?? 4
  const lista = datos ? datos[vista] : []
  const nombre = (id: string) => ALMACENES.find((item) => item.id === id)?.nombre ?? id
  return <div role="tabpanel" id="warehouses-list" aria-labelledby="warehouses-tab">
    <div className="entity-list warehouse-list">
      {snapshot?.almacenes.map((a) => {
        const act = a.actividad
        return <button type="button" key={a.id} ref={selectedId === a.id ? fila : undefined} className={`entity-row ${selectedId === a.id ? 'selected' : ''}`} aria-pressed={selectedId === a.id} onClick={() => onSelect(selectedId === a.id ? null : a.id)}>
          <span className="entity-main"><strong>{nombre(a.id)}</strong>
            <span>{a.stock === null ? 'Stock ilimitado' : `${a.stock.toLocaleString('es-PE')} / ${capacidadAlmacen(a).toLocaleString('es-PE')} paquetes · ${porcentaje(a.stock, capacidadAlmacen(a))}`}</span>
            {a.stock !== null && <NivelCarga carga={a.stock} capacidad={capacidadAlmacen(a)} />}
            {act && <span className="warehouse-activity-line" title={`Últimas ${act.horas} h simuladas`}>↑ {act.enviosDespachados} envíos · {act.paquetesDespachados} paq. &nbsp;|&nbsp; ✓ {act.enviosEntregados} entregas · {act.paquetesEntregados} paq. <small>({act.horas} h)</small></span>}
          </span>
        </button>
      })}
      {!snapshot && <p className="no-results">Esperando almacenes…</p>}
      {selected ? <section className="warehouse-transit" aria-label={`Actividad de ${nombre(selected.id)}`}>
        <h3>{nombre(selected.id)} · últimas {horas} h</h3>
        {datos && <p className="warehouse-window">Del {hora(datos.desde)} al {hora(datos.hasta)} (hora simulada)</p>}
        <div className="warehouse-kpis">
          <div><b>{datos?.enviosDespachados ?? '—'}</b><span>envíos despachados<br />{datos ? `${datos.paquetesDespachados} paquetes` : ''}</span></div>
          <div><b>{datos?.enviosEntregados ?? '—'}</b><span>entregados<br />{datos ? `${datos.paquetesEntregados} paquetes` : ''}</span></div>
          <div><b>{datos?.enTransito ?? '—'}</b><span>en tránsito<br />{datos ? `${datos.paquetesEnTransito} paquetes` : ''}</span></div>
        </div>
        <div className="warehouse-views" role="group" aria-label="Detalle de envíos">
          {([['despachados', 'Despachados'], ['entregados', 'Entregados'], ['transito', 'En tránsito']] as const).map(([clave, texto]) => <button type="button" key={clave} aria-pressed={vista === clave} onClick={() => setVista(clave)}>{texto}</button>)}
        </div>
        {error && !datos && <p className="no-results" role="alert">{error}</p>}
        {cargando && !datos && !error && <p className="no-results" role="status">Consultando…</p>}
        {datos && lista.map((p) => <Envio key={`${vista}-${p.id}`} p={p} vista={vista} />)}
        {datos && !lista.length && <p className="no-results">{vista === 'despachados' ? `Sin envíos despachados en las últimas ${horas} h.` : vista === 'entregados' ? `Sin entregas en las últimas ${horas} h.` : 'Sin envíos en tránsito desde este almacén.'}</p>}
      </section> : <p className="no-results">Selecciona un almacén (aquí o en el mapa) para ver sus envíos y entregas de las últimas {horas} h.</p>}
    </div>
  </div>
}
