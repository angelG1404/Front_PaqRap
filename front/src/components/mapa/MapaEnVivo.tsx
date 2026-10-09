import { useRef, useState, type ReactNode } from 'react'
import { ALMACENES, GRID_ALTO, GRID_ANCHO } from '../../config/defaults'
import { COLORES_SEMAFORO, COLORES_TIPO, ICONOS_TIPO, NOMBRES_ESTADO_PEDIDO, NOMBRES_ESTADO_VEHICULO, NOMBRES_TIPO } from '../../config/theme'
import type { EstadoConexion, EstadoPedido, Semaforo, Snapshot, TipoVehiculo } from '../../types/snapshot'
import { readableSimTime } from '../../services/simulationTime'
import { Icon } from '../Icon'
import { MapCanvas, type MapCanvasHandle } from './MapCanvas'
import './mapa.css'

export interface MapaEnVivoProps {
  snapshot: Snapshot | null
  estadoConexion: EstadoConexion
  escenario: '5D' | 'COLAPSO' | 'DIARIO'
  panelLateral?: ReactNode
  panelRegistrar?: ReactNode
  seleccionandoPunto?: boolean
  onElegirPunto?: (x: number, y: number) => void
  onCancelarSeleccion?: () => void
  puntoElegido?: { x: number; y: number } | null
}
function Signal({ semaforo }: { semaforo: Semaforo }) {
  return <span className="signal" style={{ color: COLORES_SEMAFORO[semaforo] }}><i style={{ background: COLORES_SEMAFORO[semaforo] }} />{semaforo === 'AMBAR' ? 'Ámbar' : semaforo === 'VERDE' ? 'Verde' : 'Rojo'}</span>
}

export function MapaEnVivo({ snapshot, estadoConexion, escenario, panelLateral, panelRegistrar, seleccionandoPunto, onElegirPunto, onCancelarSeleccion, puntoElegido }: MapaEnVivoProps) {
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [collapsed, setCollapsed] = useState(false)
  const [tab, setTab] = useState<'registro' | 'vehiculos' | 'pedidos'>(panelRegistrar ? 'registro' : 'vehiculos')
  const [vehicleQuery, setVehicleQuery] = useState('')
  const [orderQuery, setOrderQuery] = useState('')
  const [orderState, setOrderState] = useState<EstadoPedido | ''>('')
  const map = useRef<MapCanvasHandle>(null)
  const vehicle = snapshot?.vehiculos.find((item) => item.id === selectedId)
  const vehicles = snapshot?.vehiculos.filter((item) => item.id.toLowerCase().includes(vehicleQuery.toLowerCase().trim())) ?? []
  const orders = snapshot?.pedidos.filter((item) => item.id.toLowerCase().includes(orderQuery.toLowerCase().trim()) && (!orderState || item.estado === orderState)) ?? []
  const assigned = snapshot?.pedidos.filter((pedido) => pedido.vehiculoId === selectedId) ?? []
  function select(id: string) {
    const item = snapshot?.vehiculos.find((v) => v.id === id)
    setSelectedId(id)
    if (item) map.current?.center(item.x, item.y)
  }
  return <section className={`live-map ${collapsed ? 'panel-collapsed' : ''}`} aria-label={`Mapa en vivo ${escenario}`}>
    <div className="map-stage">
      {snapshot ? <MapCanvas ref={map} snapshot={snapshot} selectedId={seleccionandoPunto ? null : selectedId} onSelect={setSelectedId} seleccionandoPunto={seleccionandoPunto} onElegirPunto={onElegirPunto} onCancelarSeleccion={onCancelarSeleccion} puntoElegido={puntoElegido} /> : <div className="map-empty" role="status">{estadoConexion === 'ERROR' ? 'No se pudo iniciar la fuente de datos.' : 'Esperando datos del mapa…'}</div>}
      {seleccionandoPunto && <div className="map-pick-notice" role="status">Haz clic para elegir la celda más cercana.<button type="button" onClick={onCancelarSeleccion}>Cancelar</button></div>}
      <span className="axis axis-y">Eje Y: {GRID_ALTO} km</span><span className="axis axis-origin">(0, 0) km</span><span className="axis axis-x">Eje X: {GRID_ANCHO} km</span>
      <span className={`connection ${estadoConexion.toLowerCase()}`} role="status"><i />{estadoConexion === 'CONECTADO' ? 'Fuente conectada' : estadoConexion === 'CONECTANDO' ? 'Conectando…' : estadoConexion === 'ERROR' ? 'Error de conexión' : 'Desconectado'}</span>
      <div className="map-tools" aria-label="Controles de vista"><button type="button" aria-label="Acercar mapa" onClick={() => map.current?.zoom(1.25)}>+</button><button type="button" aria-label="Alejar mapa" onClick={() => map.current?.zoom(0.8)}>−</button><button type="button" aria-label="Centrar mapa completo" title="Centrar mapa completo" onClick={() => map.current?.reset()}><Icon name="target" /></button></div>
      <span className="map-help">Arrastra para mover · Rueda para ampliar</span>
      {vehicle && !seleccionandoPunto && <section className="unit-panel" aria-label={`Unidad ${vehicle.id}`}>
        <div className="unit-heading"><span className="unit-icon" style={{ background: COLORES_TIPO[vehicle.tipo] }}><Icon name={ICONOS_TIPO[vehicle.tipo]} /></span><h2>{vehicle.id} <span>({NOMBRES_TIPO[vehicle.tipo]})</span></h2><button type="button" className="icon-button" aria-label="Cerrar panel de unidad" onClick={() => setSelectedId(null)}><Icon name="close" /></button></div>
        <div className="unit-state"><span>{NOMBRES_ESTADO_VEHICULO[vehicle.estado]}</span><Signal semaforo={vehicle.semaforo} /></div>
        <dl><div><dt>ETA</dt><dd>{vehicle.proximaParada ? readableSimTime(vehicle.proximaParada.eta) : 'Sin próxima parada'}</dd></div><div><dt>Destino</dt><dd>{vehicle.proximaParada ? `${vehicle.proximaParada.pedidoId} (${vehicle.proximaParada.x}, ${vehicle.proximaParada.y})` : vehicle.estado === 'REGRESANDO' ? (() => { const base = ALMACENES.find((a) => a.id === vehicle.almacenBase)!; return `${base.nombre} (${base.x}, ${base.y})` })() : 'Sin destino asignado'}</dd></div><div><dt>Carga</dt><dd>{vehicle.carga} / {vehicle.capacidad} paquetes</dd></div><div><dt>Almacén base</dt><dd>{ALMACENES.find((a) => a.id === vehicle.almacenBase)?.nombre}</dd></div></dl>
        <div className="assigned-orders"><h3>PEDIDOS ASIGNADOS ({assigned.length})</h3>{assigned.length ? assigned.map((pedido) => <p key={pedido.id}><span>{pedido.id}</span><span>{pedido.cantidad} paq. · {NOMBRES_ESTADO_PEDIDO[pedido.estado]}</span></p>) : <p>Sin pedidos asignados</p>}</div>
      </section>}
    </div>
    <button type="button" className="collapse-panel" aria-label={collapsed ? 'Mostrar panel lateral' : 'Ocultar panel lateral'} aria-expanded={!collapsed} aria-controls="map-side-panel" onClick={() => setCollapsed(!collapsed)}>{collapsed ? '‹' : '›'}</button>
    <aside id="map-side-panel" className="map-side-panel" hidden={collapsed}>
      <section className="map-legend"><div className="legend-title"><h2><Icon name="map" /> LEYENDA</h2><span>{GRID_ANCHO}×{GRID_ALTO} km</span></div>
        <div className="legend-grid"><div className="legend-item"><span className="legend-symbol warehouse"><Icon name="warehouse" /></span>Almacén</div>{(Object.keys(COLORES_TIPO) as TipoVehiculo[]).map((tipo) => <div className="legend-item" key={tipo}><span className="legend-symbol" style={{ background: COLORES_TIPO[tipo] }}><Icon name={ICONOS_TIPO[tipo]} /></span>{NOMBRES_TIPO[tipo]}</div>)}</div>
        <div className="traffic-legend">{(Object.keys(COLORES_SEMAFORO) as Semaforo[]).map((signal) => <Signal key={signal} semaforo={signal} />)}</div><p>El anillo indica la holgura del plazo.</p>
      </section>
      <section className="map-operations"><h2><Icon name="route" /> OPERACIONES ACTIVAS</h2>
        <div className={`map-tabs ${panelRegistrar ? 'three-tabs' : ''}`} role="tablist" aria-label="Operaciones">{panelRegistrar && <button type="button" role="tab" id="register-tab" aria-selected={tab === 'registro'} aria-controls="register-panel" onClick={() => setTab('registro')}>Registrar pedido</button>}<button type="button" role="tab" id="vehicles-tab" aria-selected={tab === 'vehiculos'} aria-controls="vehicles-list" onClick={() => { setTab('vehiculos'); onCancelarSeleccion?.() }}>Vehículos ({snapshot?.vehiculos.length ?? 0})</button><button type="button" role="tab" id="orders-tab" aria-selected={tab === 'pedidos'} aria-controls="orders-list" onClick={() => { setTab('pedidos'); onCancelarSeleccion?.() }}>Pedidos ({snapshot?.pedidos.length ?? 0})</button></div>
        {panelRegistrar && <div role="tabpanel" id="register-panel" aria-labelledby="register-tab" hidden={tab !== 'registro'} className="registration-scroll">{panelRegistrar}</div>}
        {tab === 'vehiculos' ? <div role="tabpanel" id="vehicles-list" aria-labelledby="vehicles-tab"><label className="map-search"><Icon name="search" /><input type="search" aria-label="Buscar vehículo por ID" placeholder="Buscar ID de vehículo…" value={vehicleQuery} onChange={(e) => setVehicleQuery(e.target.value)} /></label><div className="entity-list">{vehicles.map((item) => <button type="button" key={item.id} className={`entity-row ${item.id === selectedId ? 'selected' : ''}`} aria-label={`Seleccionar ${item.id}`} aria-pressed={item.id === selectedId} onClick={() => select(item.id)}><span className="list-vehicle-icon" style={{ background: COLORES_TIPO[item.tipo] }}><Icon name={ICONOS_TIPO[item.tipo]} /></span><span className="entity-main"><strong>{item.id} <small>{NOMBRES_TIPO[item.tipo]}</small></strong><span>{NOMBRES_ESTADO_VEHICULO[item.estado]}</span></span><Signal semaforo={item.semaforo} /></button>)}{!vehicles.length && <p className="no-results">No se encontraron vehículos.</p>}</div></div>
          : tab === 'pedidos' ? <div role="tabpanel" id="orders-list" aria-labelledby="orders-tab"><label className="map-search"><Icon name="search" /><input type="search" aria-label="Buscar pedido por ID" placeholder="Buscar ID de pedido…" value={orderQuery} onChange={(e) => setOrderQuery(e.target.value)} /></label><label className="order-filter">Estado<select aria-label="Filtrar pedidos por estado" value={orderState} onChange={(e) => setOrderState(e.target.value as EstadoPedido | '')}><option value="">Todos los estados</option>{(Object.entries(NOMBRES_ESTADO_PEDIDO) as [EstadoPedido, string][]).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label><div className="entity-list">{orders.map((item) => <button type="button" key={item.id} className="entity-row order-row" aria-label={`Centrar pedido ${item.id}`} onClick={() => { setSelectedId(null); map.current?.center(item.x, item.y) }}><span className="entity-main"><strong>{item.id} <small>{item.cantidad} paq.</small></strong><span>{NOMBRES_ESTADO_PEDIDO[item.estado]} · ({item.x}, {item.y})</span><span>Plazo: {readableSimTime(item.deadline)}</span></span><Signal semaforo={item.semaforo} /></button>)}{!orders.length && <p className="no-results">No se encontraron pedidos.</p>}</div></div> : null}
      </section>
      {panelLateral && <section className="map-panel-extra">{panelLateral}</section>}
    </aside>
  </section>
}
