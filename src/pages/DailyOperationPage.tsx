import { useState } from 'react'
import { MapaEnVivo } from '../components/mapa/MapaEnVivo'
import { useSnapshots } from '../hooks/useSnapshots'
import { dailySource } from '../mock/dailySource'
import { continuousDailySource } from '../services/continuousDailySource'
import { formatFechaHora } from '../services/simulationTime'
import { RegistroPedidoForm, type CoordenadasPedido } from '../components/RegistroPedidoForm'
import { FLOTA, GRID_ALTO, GRID_ANCHO } from '../config/defaults'
import { ICONOS_TIPO, NOMBRES_TIPO } from '../config/theme'
import type { TipoVehiculo } from '../types/snapshot'
import { Icon } from '../components/Icon'
import './operacion-diaria.css'

const source = continuousDailySource(dailySource)

export function DailyOperationPage() {
  const { snapshot, estadoConexion } = useSnapshots(source)
  const [coordenadas, setCoordenadas] = useState<CoordenadasPedido>({ x: '', y: '' })
  const [eligiendo, setEligiendo] = useState(false)
  const x = Number(coordenadas.x), y = Number(coordenadas.y)
  const puntoElegido = coordenadas.x !== '' && coordenadas.y !== '' && Number.isInteger(x) && Number.isInteger(y) && x >= 0 && x <= GRID_ANCHO && y >= 0 && y <= GRID_ALTO ? { x, y } : null
  return <div className="map-page daily-page">
    <header className="daily-header"><div className="daily-heading"><div><h1>Operación en Tiempo Real</h1><span className="live-badge"><i />En vivo — <time dateTime={snapshot?.simTimeIso}>{formatFechaHora(snapshot?.simTimeIso)}</time></span></div><p>Supervisión y despacho continuo de flota activa</p><span className={`daily-connection ${estadoConexion.toLowerCase()}`} role="status">● {estadoConexion === 'CONECTADO' ? 'Conectado' : estadoConexion === 'ERROR' ? 'Error de conexión' : estadoConexion === 'DESCONECTADO' ? 'Desconectado' : 'Conectando…'}</span></div>
      <div className="fleet-counters" aria-label="Flota configurada">{FLOTA.map((item) => { const type = item.id.toUpperCase() as TipoVehiculo; return <span key={item.id} title={NOMBRES_TIPO[type]}><Icon name={ICONOS_TIPO[type]} /><strong>{item.cantidad}</strong><span className="sr-only">{NOMBRES_TIPO[type]}</span></span> })}</div>
      <div className="package-counter"><span>PAQUETES</span><strong>{snapshot?.metricas.entregados ?? '—'} <i>/</i> {snapshot?.metricas.pedidosTotal ?? '—'}</strong><small>entregados / total</small></div>
    </header>
    <MapaEnVivo snapshot={snapshot} estadoConexion={estadoConexion} escenario="DIARIO" seleccionandoPunto={eligiendo} puntoElegido={puntoElegido} onElegirPunto={(pickedX, pickedY) => { setCoordenadas({ x: String(pickedX), y: String(pickedY) }); setEligiendo(false) }} onCancelarSeleccion={() => setEligiendo(false)} panelRegistrar={<RegistroPedidoForm coordenadas={coordenadas} setCoordenadas={setCoordenadas} eligiendo={eligiendo} onElegir={() => setEligiendo(true)} onCancelar={() => setEligiendo(false)} disponible={estadoConexion === 'CONECTADO' && !!snapshot} />} />
  </div>
}
