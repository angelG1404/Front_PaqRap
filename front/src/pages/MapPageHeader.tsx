import { type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { FLOTA } from '../config/defaults'
import { ICONOS_TIPO, NOMBRES_TIPO } from '../config/theme'
import type { Snapshot, TipoVehiculo } from '../types/snapshot'
import { formatDuration, formatFechaHoraMinuto, formatTranscurrido, readableSimTime } from '../services/simulationTime'
import { useHoraActual } from '../hooks/useHoraActual'
import { USE_MOCK } from '../services/api'
import { Icon } from '../components/Icon'
import { Ocupacion } from '../components/mapa/Ocupacion'

interface Props { snapshot: Snapshot | null; escenario: Snapshot['escenario']; onDetener?: () => void; deteniendo?: boolean; acciones?: ReactNode }

export function MapPageHeader({ snapshot, escenario, onDetener, deteniendo, acciones }: Props) {
  const ahora = useHoraActual()
  const daily = escenario === 'DIARIO'
  const estado = snapshot?.estado
  const flota = snapshot?.metricas.flota
  return <div className="workspace-header">
    <header className="workspace-navigation">
      <p>{daily ? 'Operación diaria' : 'Módulo de simulación'} <span>/</span> <strong>Mapa en vivo</strong></p>
      <div>{USE_MOCK && <span className="demo-badge">Demostración</span>}{!daily && <Link to="/simulacion/parametros" className="back-parameters">← Parámetros</Link>}{acciones}</div>
    </header>
    <div className="workspace-overview">
      <div className="workspace-scenario"><h1>{daily ? 'Operación diaria' : escenario === '5D' ? 'Simulación 5D' : 'Colapso logístico'}</h1>
        <span>Inicio <time dateTime={snapshot?.inicioIso}>{snapshot?.inicioIso ? formatFechaHoraMinuto(snapshot.inicioIso) : '—'}</time></span>
        <span className={`workspace-status status-${estado ?? 'CONECTANDO'}`}>● {estado === 'COLAPSADO' ? 'Colapsada / congelada' : estado === 'FINALIZADO' ? 'Finalizada' : estado === 'DETENIDA' ? 'Detenida' : estado === 'EN_CURSO' ? 'En curso' : 'Conectando'}</span>
      </div>
      <Ocupacion snapshot={snapshot} />
      {!daily && onDetener && estado === 'EN_CURSO' && <button type="button" className="stop-button" onClick={onDetener} disabled={deteniendo}>■ {deteniendo ? 'Deteniendo…' : 'Detener'}</button>}
    </div>
    <div className="workspace-telemetry">
      <div className="workspace-main-clock"><span>{daily ? 'Fecha y hora actual' : 'Momento simulado'}</span>
        <time dateTime={daily ? ahora : snapshot?.simTimeIso}>{daily ? formatFechaHoraMinuto(ahora) : snapshot ? formatFechaHoraMinuto(snapshot.simTimeIso) : '—'}{!daily && snapshot && <small> | {readableSimTime(snapshot.simTime).replace(/ · .*/, '')}</small>}</time>
      </div>
      <div className="workspace-time-group">
        {!daily && <div><span>Transcurrido simulado</span><strong>{snapshot ? formatTranscurrido(snapshot.inicioIso, snapshot.simTimeIso) : '—'}</strong></div>}
        <div><span>{daily ? 'Duración de operación' : 'Duración real'}</span><strong>{snapshot?.duracionSeg !== undefined ? formatDuration(snapshot.duracionSeg) : '—'}</strong></div>
        {!daily && <div><span>Velocidad</span><strong>{snapshot?.factorVelocidad ? `${snapshot.factorVelocidad}x` : '—'}</strong></div>}
      </div>
      <div className="workspace-deliveries"><span>Pedidos entregados</span><strong>{snapshot?.metricas.entregados ?? '—'} / {snapshot?.metricas.pedidosTotal ?? '—'}</strong></div>
      <div className="workspace-fleet"><span>Flota</span><div>{FLOTA.map((item) => { const tipo = item.id.toUpperCase() as TipoVehiculo; return <span key={item.id} title={NOMBRES_TIPO[tipo]}><Icon name={ICONOS_TIPO[tipo]} />{flota?.[item.id] ?? '—'}<span className="sr-only">{NOMBRES_TIPO[tipo]}</span></span> })}</div></div>
    </div>
  </div>
}
