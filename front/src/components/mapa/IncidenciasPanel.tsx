import { useState } from 'react'
import { COLORES_AVERIA, NOMBRES_AVERIA } from '../../config/theme'
import { readableSimTime } from '../../services/simulationTime'
import type { AveriaDetalle, Snapshot } from '../../types/snapshot'

type Filtro = 'ACTIVA' | 'RESUELTA' | 'TODAS'
const hora = (simTime?: string | null) => simTime ? readableSimTime(simTime) : '—'
const horas = (valor: number) => `${valor.toLocaleString('es-PE', { maximumFractionDigits: 1 })} h`

function ubicacion(a: AveriaDetalle): string {
  if (a.estado === 'RESUELTA') return 'Unidad de vuelta en operación'
  return a.remolcada ? `Almacén Central (${a.x}, ${a.y}) · remolcada` : `En el lugar de la avería (${a.x}, ${a.y})`
}

/**
 * Averías de la corrida con todos sus atributos. El operador solo elige unidad y tipo al registrarla; la ocurrencia,
 * el fin, la ubicación y el estado los fija el motor y se actualizan aquí (remolque al central, resolución).
 */
export function IncidenciasPanel({ snapshot, onSelect }: { snapshot: Snapshot | null; onSelect: (vehiculoId: string) => void }) {
  const [filtro, setFiltro] = useState<Filtro>('ACTIVA')
  const todas = snapshot?.averias ?? []
  const activas = todas.filter((a) => a.estado === 'ACTIVA').length
  const lista = todas.filter((a) => filtro === 'TODAS' || a.estado === filtro)
  return <div role="tabpanel" id="incidents-list" aria-labelledby="incidents-tab">
    <div className="incident-filters" role="group" aria-label="Estado de la avería">
      {([['ACTIVA', `Activas ${activas}`], ['RESUELTA', `Resueltas ${todas.length - activas}`], ['TODAS', `Todas ${todas.length}`]] as const).map(([clave, texto]) => <button type="button" key={clave} aria-pressed={filtro === clave} onClick={() => setFiltro(clave)}>{texto}</button>)}
    </div>
    <div className="entity-list">
      {lista.map((a) => <button type="button" className={`entity-row incident-row ${a.estado === 'RESUELTA' ? 'resolved' : ''}`} key={a.id} onClick={() => onSelect(a.vehiculoId)}>
        <span className="entity-main">
          <strong>{a.id} · {a.vehiculoId} <small style={{ color: COLORES_AVERIA[a.tipo] }}>Tipo {a.tipo}</small> <small className={`incident-state ${a.estado === 'ACTIVA' ? 'active' : 'done'}`}>{a.estado === 'ACTIVA' ? 'Activa' : 'Resuelta'}</small></strong>
          <span>Ocurrió: {hora(a.ocurrencia)}</span>
          <span>Fin estimado: {hora(a.fin)} ({horas(a.duracionHoras)})</span>
          <span>Ubicación: {ubicacion(a)}</span>
          {a.tipo !== 1 && <span>Remolque al central: {a.remolcada ? `realizado (${hora(a.remolqueEn)})` : `programado ${hora(a.remolqueEn)}`}</span>}
          {a.resueltaEn && <span>Resuelta: {hora(a.resueltaEn)}</span>}
          <span>Pedidos reasignados: {a.pedidosReasignados}</span>
          <span className="incident-rule">{NOMBRES_AVERIA[a.tipo]}</span>
        </span>
      </button>)}
      {!lista.length && <p className="no-results">{filtro === 'ACTIVA' ? 'Sin averías activas.' : filtro === 'RESUELTA' ? 'Aún no hay averías resueltas.' : 'Sin averías en esta corrida.'}</p>}
    </div>
  </div>
}
