import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { FLOTA } from '../config/defaults'
import { ICONOS_TIPO, NOMBRES_TIPO } from '../config/theme'
import type { Snapshot, TipoVehiculo } from '../types/snapshot'
import { formatDuration, readableSimTime } from '../services/simulationTime'
import { Icon } from '../components/Icon'

/** Cabeceras de página: el componente MapaEnVivo no conoce estos contadores. */
export function MapPageHeader({ snapshot, escenario }: { snapshot: Snapshot | null; escenario: Snapshot['escenario'] }) {
  const [openedAt] = useState(() => Date.now())
  const [elapsed, setElapsed] = useState(0)
  useEffect(() => { const timer = setInterval(() => setElapsed(Math.floor((Date.now() - openedAt) / 1000)), 1000); return () => clearInterval(timer) }, [openedAt])
  const daily = escenario === 'DIARIO'
  const estado = snapshot?.estado
  return <>
    <header className="map-page-header"><div><p className="breadcrumb">{daily ? 'OPERACIÓN DIARIA' : 'MÓDULO DE SIMULACIÓN'} <span>/</span> <strong>Mapa en Vivo</strong></p></div>{!daily && <div className="map-page-actions"><Link to="/simulacion/parametros" className="back-parameters">← Volver a Parámetros</Link><ol className="steps" aria-label="Pasos de la corrida"><li><span>✓</span>1. Parámetros</li><li className="step-line" aria-hidden="true" /><li className="current" aria-current="step"><span>2</span>2. Mapa <Icon name="map" /></li></ol></div>}</header>
    <div className="map-summary-bar"><div className="scenario-label"><i /><div><strong>{daily ? 'Operación Diaria' : escenario === '5D' ? 'Simulación 5D' : 'Colapso Logístico'}</strong><span>{daily ? 'Seguimiento continuo' : 'Modo de simulación · Solo lectura'}</span></div></div><span className="demo-badge">DATOS DE DEMOSTRACIÓN</span><div className="fleet-counters" aria-label="Flota configurada">{FLOTA.map((item) => { const type = item.id.toUpperCase() as TipoVehiculo; return <span key={item.id} title={NOMBRES_TIPO[type]}><Icon name={ICONOS_TIPO[type]} /><strong>{item.cantidad}</strong><span className="sr-only">{NOMBRES_TIPO[type]}</span></span> })}</div><div className="package-counter"><span>PAQUETES</span><strong>{snapshot?.metricas.entregados ?? '—'} <i>/</i> {snapshot?.metricas.pedidosTotal ?? '—'}</strong><small>entregados / total</small></div></div>
    <div className="map-clock-bar"><span className="clock-pill"><Icon name="clock" /><strong>{daily ? 'HORA LOCAL:' : 'SIMULACIÓN:'}</strong><time>{daily ? snapshot?.simTimeIso?.replace('T', ' ') ?? '—' : snapshot ? readableSimTime(snapshot.simTime) : '—'}</time></span><span className="clock-pill duration"><Icon name="clock" /><strong>DURACIÓN:</strong><time>{formatDuration(elapsed)}</time></span><span className={`run-status ${estado === 'COLAPSADO' ? 'collapsed' : estado === 'FINALIZADO' ? 'finished' : ''}`}>● ESTADO: {estado === 'COLAPSADO' ? 'DETENIDA / CONGELADA' : estado === 'FINALIZADO' ? 'FINALIZADA' : estado === 'EN_CURSO' ? 'EN CURSO' : 'CONECTANDO'}</span></div>
  </>
}
