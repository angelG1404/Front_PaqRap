import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { FLOTA } from '../config/defaults'
import { ICONOS_TIPO, NOMBRES_TIPO } from '../config/theme'
import type { Snapshot, TipoVehiculo } from '../types/snapshot'
import { formatDuration, formatFechaHora, readableSimTime } from '../services/simulationTime'
import { USE_MOCK } from '../services/api'
import { Icon } from '../components/Icon'
import { PlanificacionPill } from '../components/PlanificacionPill'

interface Props { snapshot: Snapshot | null; escenario: Snapshot['escenario']; onDetener?: () => void; deteniendo?: boolean }

/** Cabeceras de página: el componente MapaEnVivo no conoce estos contadores. */
export function MapPageHeader({ snapshot, escenario, onDetener, deteniendo }: Props) {
  const [openedAt] = useState(() => Date.now())
  const [elapsed, setElapsed] = useState(0)
  useEffect(() => { const timer = setInterval(() => setElapsed(Math.floor((Date.now() - openedAt) / 1000)), 1000); return () => clearInterval(timer) }, [openedAt])
  const daily = escenario === 'DIARIO'
  const estado = snapshot?.estado
  const enCurso = estado === 'EN_CURSO'
  // La duración real la mide el servidor: es la misma en todos los dispositivos conectados.
  const duracion = snapshot?.duracionSeg ?? elapsed
  const flota = snapshot?.metricas.flota
  return <>
    <header className="map-page-header"><div><p className="breadcrumb">{daily ? 'OPERACIÓN DIARIA' : 'MÓDULO DE SIMULACIÓN'} <span>/</span> <strong>Mapa en Vivo</strong></p></div>{!daily && <div className="map-page-actions"><Link to="/simulacion/parametros" className="back-parameters">← Volver a Parámetros</Link><ol className="steps" aria-label="Pasos de la corrida"><li><span>✓</span>1. Parámetros</li><li className="step-line" aria-hidden="true" /><li className="current" aria-current="step"><span>2</span>2. Mapa <Icon name="map" /></li></ol></div>}</header>
    <div className="map-summary-bar"><div className="scenario-label"><i /><div><strong>{daily ? 'Operación Diaria' : escenario === '5D' ? 'Simulación 5D' : 'Colapso Logístico'}</strong><span>{daily ? 'Seguimiento continuo' : `Algoritmo ${snapshot?.algoritmo ?? '—'} · Solo lectura`}</span></div></div>
      {USE_MOCK && <span className="demo-badge">DATOS DE DEMOSTRACIÓN</span>}
      {!daily && onDetener && enCurso && <button type="button" className="stop-button" onClick={onDetener} disabled={deteniendo}><span aria-hidden="true">■</span>{deteniendo ? 'Deteniendo…' : 'Detener'}</button>}
      <div className="fleet-counters" aria-label="Flota configurada">{FLOTA.map((item) => { const type = item.id.toUpperCase() as TipoVehiculo; return <span key={item.id} title={NOMBRES_TIPO[type]}><Icon name={ICONOS_TIPO[type]} /><strong>{flota?.[item.id] ?? item.cantidad}</strong><span className="sr-only">{NOMBRES_TIPO[type]}</span></span> })}</div>
      <div className="package-counter"><span>PEDIDOS</span><strong>{snapshot?.metricas.entregados ?? '—'} <i>/</i> {snapshot?.metricas.pedidosTotal ?? '—'}</strong><small>entregados / total</small></div></div>
    <div className="map-clock-bar"><span className="clock-pill"><Icon name="clock" /><strong>{daily ? 'HORA LOCAL:' : 'SIMULACIÓN:'}</strong><time>{snapshot ? `${formatFechaHora(snapshot.simTimeIso)}${daily ? '' : ` | ${readableSimTime(snapshot.simTime).replace(/ · .*/, '')}`}` : '—'}</time></span>
      <span className="clock-pill duration"><Icon name="clock" /><strong>DURACIÓN:</strong><time>{formatDuration(duracion)}</time></span>
      {!daily && snapshot?.factorVelocidad && <span className="clock-pill speed"><strong>VELOCIDAD:</strong><time>{snapshot.factorVelocidad}x</time></span>}
      {!daily && snapshot && <span className="clock-pill metrics"><strong>ATRASADOS:</strong><time className={snapshot.metricas.atrasados ? 'late' : ''}>{snapshot.metricas.atrasados}</time><strong>COSTO:</strong><time>S/ {(snapshot.metricas.costoTotal ?? 0).toLocaleString('es-PE', { maximumFractionDigits: 0 })}</time></span>}
      <PlanificacionPill snapshot={snapshot} />
      <span className={`run-status ${estado === 'COLAPSADO' ? 'collapsed' : estado === 'FINALIZADO' || estado === 'DETENIDA' ? 'finished' : ''}`}>● ESTADO: {estado === 'COLAPSADO' ? 'DETENIDA / CONGELADA' : estado === 'FINALIZADO' ? 'FINALIZADA' : estado === 'DETENIDA' ? 'DETENIDA POR EL OPERADOR' : estado === 'EN_CURSO' ? 'EN CURSO' : 'CONECTANDO'}</span></div>
  </>
}
