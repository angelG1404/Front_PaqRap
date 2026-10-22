import { useRef, useState, type ReactNode } from 'react'
import { ALMACENES, GRID_ALTO, GRID_ANCHO } from '../../config/defaults'
import { COLORES_SEMAFORO, COLORES_TIPO, ICONOS_TIPO, MAP_THEME, NOMBRES_ESTADO_PEDIDO, NOMBRES_ESTADO_VEHICULO, NOMBRES_TIPO } from '../../config/theme'
import type { EstadoConexion, EstadoPedido, EstadoVehiculo, Semaforo, Snapshot, TipoVehiculo, VehiculoSnap } from '../../types/snapshot'
import { readableSimTime } from '../../services/simulationTime'
import { Icon } from '../Icon'
import { MapCanvas, type CapasMapa, type MapCanvasHandle } from './MapCanvas'
import './mapa.css'

export interface MapaEnVivoProps {
  snapshot: Snapshot | null
  estadoConexion: EstadoConexion
  escenario: '5D' | 'COLAPSO' | 'DIARIO'
  panelLateral?: ReactNode
  panelRegistrar?: ReactNode
  etiquetaRegistro?: string
  seleccionandoPunto?: boolean
  onElegirPunto?: (x: number, y: number) => void
  onCancelarSeleccion?: () => void
  puntoElegido?: { x: number; y: number } | null
}
function Signal({ semaforo }: { semaforo: Semaforo }) {
  return <span className="signal" style={{ color: COLORES_SEMAFORO[semaforo] }}><i style={{ background: COLORES_SEMAFORO[semaforo] }} />{semaforo === 'AMBAR' ? 'Ámbar' : semaforo === 'VERDE' ? 'Verde' : 'Rojo'}</span>
}
const CAPAS: { id: keyof CapasMapa; nombre: string }[] = [
  { id: 'rutas', nombre: 'Rutas' }, { id: 'pedidos', nombre: 'Pedidos' }, { id: 'entregados', nombre: 'Entregados' }, { id: 'bloqueos', nombre: 'Bloqueos' },
]
const hora = (simTime: string | null | undefined) => simTime ? readableSimTime(simTime) : '—'

function destino(vehicle: VehiculoSnap): string {
  if (vehicle.averia) return vehicle.averia.remolcado ? 'En Almacén Central (remolcada)' : 'Detenida en el lugar de la avería'
  if (vehicle.proximaParada) return `${vehicle.proximaParada.pedidoId} (${vehicle.proximaParada.x}, ${vehicle.proximaParada.y})`
  const base = ALMACENES.find((a) => a.id === vehicle.almacenBase)
  if (vehicle.estado === 'REGRESANDO' && base) return `${base.nombre} (${base.x}, ${base.y})`
  if (vehicle.estado === 'ATENDIENDO') return 'Entregando al cliente'
  return 'Sin destino asignado'
}

export function MapaEnVivo({ snapshot, estadoConexion, escenario, panelLateral, panelRegistrar, etiquetaRegistro = 'Registrar pedido', seleccionandoPunto, onElegirPunto, onCancelarSeleccion, puntoElegido }: MapaEnVivoProps) {
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [collapsed, setCollapsed] = useState(false)
  const [tab, setTab] = useState<'registro' | 'vehiculos' | 'pedidos'>(panelRegistrar ? 'registro' : 'vehiculos')
  const [vehicleQuery, setVehicleQuery] = useState('')
  const [vehicleState, setVehicleState] = useState<EstadoVehiculo | ''>('')
  const [orderQuery, setOrderQuery] = useState('')
  const [orderState, setOrderState] = useState<EstadoPedido | ''>('')
  const [capas, setCapas] = useState<CapasMapa>({ rutas: true, pedidos: true, entregados: true, bloqueos: true })
  const map = useRef<MapCanvasHandle>(null)
  const vehicle = snapshot?.vehiculos.find((item) => item.id === selectedId)
  const vehicles = snapshot?.vehiculos.filter((item) => item.id.toLowerCase().includes(vehicleQuery.toLowerCase().trim()) && (!vehicleState || item.estado === vehicleState)) ?? []
  const orders = snapshot?.pedidos.filter((item) => (item.id.toLowerCase().includes(orderQuery.toLowerCase().trim()) || item.clienteId.toLowerCase().includes(orderQuery.toLowerCase().trim())) && (!orderState || item.estado === orderState)) ?? []
  const assigned = snapshot?.pedidos.filter((pedido) => pedido.vehiculoId === selectedId && pedido.estado !== 'ENTREGADO') ?? []
  const paradas = vehicle?.paradas ?? []
  function select(id: string) {
    const item = snapshot?.vehiculos.find((v) => v.id === id)
    setSelectedId(id)
    if (item) map.current?.center(item.x, item.y)
  }
  return <section className={`live-map ${collapsed ? 'panel-collapsed' : ''}`} aria-label={`Mapa en vivo ${escenario}`}>
    <div className="map-stage">
      {snapshot ? <MapCanvas ref={map} snapshot={snapshot} capas={capas} selectedId={seleccionandoPunto ? null : selectedId} onSelect={setSelectedId} seleccionandoPunto={seleccionandoPunto} onElegirPunto={onElegirPunto} onCancelarSeleccion={onCancelarSeleccion} puntoElegido={puntoElegido} /> : <div className="map-empty" role="status">{estadoConexion === 'ERROR' ? 'No se pudo conectar con el servidor. Revisa que el backend esté levantado.' : 'Esperando datos del mapa…'}</div>}
      {seleccionandoPunto && <div className="map-pick-notice" role="status">Haz clic para elegir la celda más cercana.<button type="button" onClick={onCancelarSeleccion}>Cancelar</button></div>}
      <span className="axis axis-y">Eje Y: {GRID_ALTO} km</span><span className="axis axis-origin">(0, 0) km</span><span className="axis axis-x">Eje X: {GRID_ANCHO} km</span>
      <span className={`connection ${estadoConexion.toLowerCase()}`} role="status"><i />{estadoConexion === 'CONECTADO' ? 'Fuente conectada' : estadoConexion === 'CONECTANDO' ? 'Conectando…' : estadoConexion === 'ERROR' ? 'Error de conexión' : 'Desconectado'}</span>
      <div className="map-tools" aria-label="Controles de vista"><button type="button" aria-label="Acercar mapa" onClick={() => map.current?.zoom(1.25)}>+</button><button type="button" aria-label="Alejar mapa" onClick={() => map.current?.zoom(0.8)}>−</button><button type="button" aria-label="Centrar mapa completo" title="Centrar mapa completo" onClick={() => map.current?.reset()}><Icon name="target" /></button></div>
      <span className="map-help">Arrastra para mover · Rueda para ampliar · Pasa el cursor para ver detalles</span>
      {vehicle && !seleccionandoPunto && <section className="unit-panel" aria-label={`Unidad ${vehicle.id}`}>
        <div className="unit-heading"><span className="unit-icon" style={{ background: vehicle.estado === 'AVERIADO' ? MAP_THEME.averiado : COLORES_TIPO[vehicle.tipo] }}><Icon name={ICONOS_TIPO[vehicle.tipo]} /></span><h2>{vehicle.id} <span>({NOMBRES_TIPO[vehicle.tipo]})</span></h2>{vehicle.proximaParada?.eta && <span className="unit-eta">ETA {hora(vehicle.proximaParada.eta).replace(/^Día \d+ · /, '')}</span>}<button type="button" className="icon-button" aria-label="Cerrar panel de unidad" onClick={() => setSelectedId(null)}><Icon name="close" /></button></div>
        <div className="unit-state"><span className={vehicle.estado === 'AVERIADO' ? 'broken' : ''}>{NOMBRES_ESTADO_VEHICULO[vehicle.estado]}</span><Signal semaforo={vehicle.semaforo} /></div>
        {vehicle.averia && <p className="unit-breakdown">Avería tipo {vehicle.averia.tipo} desde {hora(vehicle.averia.desde)} · disponible {hora(vehicle.averia.hasta)}</p>}
        <dl><div><dt>Destino</dt><dd>{destino(vehicle)}</dd></div><div><dt>Carga</dt><dd>{vehicle.carga} / {vehicle.capacidad} paquetes</dd></div><div><dt>Almacén base</dt><dd>{ALMACENES.find((a) => a.id === vehicle.almacenBase)?.nombre}</dd></div>{vehicle.kmRecorridos !== undefined && <div><dt>Recorrido</dt><dd>{vehicle.kmRecorridos.toLocaleString('es-PE', { maximumFractionDigits: 1 })} km</dd></div>}</dl>
        <div className="assigned-orders"><h3>{paradas.length ? `RUTA PLANIFICADA (${paradas.length} paradas)` : `PEDIDOS ASIGNADOS (${assigned.length})`}</h3>
          {paradas.length ? paradas.map((parada, index) => <p key={parada.pedidoId}><span>{index + 1}. {parada.pedidoId}</span><span>{parada.cantidad ?? '—'} paq. · ETA {hora(parada.eta).replace(/^Día \d+ · /, '')}</span></p>)
            : assigned.length ? assigned.map((pedido) => <p key={pedido.id}><span>{pedido.id}</span><span>{pedido.cantidad} paq. · {NOMBRES_ESTADO_PEDIDO[pedido.estado]}</span></p>) : <p>Sin pedidos asignados</p>}
        </div>
      </section>}
    </div>
    <button type="button" className="collapse-panel" aria-label={collapsed ? 'Mostrar panel lateral' : 'Ocultar panel lateral'} aria-expanded={!collapsed} aria-controls="map-side-panel" onClick={() => setCollapsed(!collapsed)}>{collapsed ? '‹' : '›'}</button>
    <aside id="map-side-panel" className="map-side-panel" hidden={collapsed}>
      <section className="map-legend"><div className="legend-title"><h2><Icon name="map" /> LEYENDA</h2><span>{GRID_ANCHO}×{GRID_ALTO} km</span></div>
        <div className="legend-grid">
          <div className="legend-item"><span className="legend-symbol warehouse"><Icon name="warehouse" /></span>Almacén</div>
          <div className="legend-item"><span className="legend-symbol blocked" /> Bloqueo</div>
          {(Object.keys(COLORES_TIPO) as TipoVehiculo[]).map((tipo) => <div className="legend-item" key={tipo}><span className="legend-symbol" style={{ background: COLORES_TIPO[tipo] }}><Icon name={ICONOS_TIPO[tipo]} /></span>{NOMBRES_TIPO[tipo]}</div>)}
          <div className="legend-item"><span className="legend-symbol" style={{ background: MAP_THEME.averiado }}>!</span>Averiado</div>
          <div className="legend-item"><span className="legend-pin pending" />Pedido sin unidad</div>
          <div className="legend-item"><span className="legend-pin" />Pedido asignado</div>
        </div>
        <div className="traffic-legend">{(Object.keys(COLORES_SEMAFORO) as Semaforo[]).map((signal) => <Signal key={signal} semaforo={signal} />)}</div><p>El anillo y el pin indican la holgura del plazo{snapshot?.escenario !== 'DIARIO' ? ' (umbrales de la corrida)' : ''}.</p>
        <div className="layer-toggles" role="group" aria-label="Capas visibles">{CAPAS.map((capa) => <label key={capa.id} className={capas[capa.id] ? 'on' : ''}><input type="checkbox" checked={capas[capa.id]} onChange={(e) => setCapas({ ...capas, [capa.id]: e.target.checked })} />{capa.nombre}</label>)}</div>
      </section>
      <section className="map-operations"><h2><Icon name="route" /> OPERACIONES ACTIVAS</h2>
        <div className={`map-tabs ${panelRegistrar ? 'three-tabs' : ''}`} role="tablist" aria-label="Operaciones">{panelRegistrar && <button type="button" role="tab" id="register-tab" aria-selected={tab === 'registro'} aria-controls="register-panel" onClick={() => setTab('registro')}>{etiquetaRegistro}</button>}<button type="button" role="tab" id="vehicles-tab" aria-selected={tab === 'vehiculos'} aria-controls="vehicles-list" onClick={() => { setTab('vehiculos'); onCancelarSeleccion?.() }}>Vehículos ({snapshot?.vehiculos.length ?? 0})</button><button type="button" role="tab" id="orders-tab" aria-selected={tab === 'pedidos'} aria-controls="orders-list" onClick={() => { setTab('pedidos'); onCancelarSeleccion?.() }}>Pedidos ({snapshot?.pedidos.filter((p) => p.estado !== 'ENTREGADO').length ?? 0})</button></div>
        {panelRegistrar && <div role="tabpanel" id="register-panel" aria-labelledby="register-tab" hidden={tab !== 'registro'} className="registration-scroll">{panelRegistrar}</div>}
        {tab === 'vehiculos' ? <div role="tabpanel" id="vehicles-list" aria-labelledby="vehicles-tab"><label className="map-search"><Icon name="search" /><input type="search" aria-label="Buscar vehículo por ID" placeholder="Buscar ID de vehículo…" value={vehicleQuery} onChange={(e) => setVehicleQuery(e.target.value)} /></label><label className="order-filter">Estado<select aria-label="Filtrar vehículos por estado" value={vehicleState} onChange={(e) => setVehicleState(e.target.value as EstadoVehiculo | '')}><option value="">Todos los estados</option>{(Object.entries(NOMBRES_ESTADO_VEHICULO) as [EstadoVehiculo, string][]).map(([value, label]) => <option key={value} value={value}>{label} ({snapshot?.vehiculos.filter((v) => v.estado === value).length ?? 0})</option>)}</select></label><div className="entity-list">{vehicles.map((item) => <button type="button" key={item.id} className={`entity-row ${item.id === selectedId ? 'selected' : ''} ${item.estado === 'AVERIADO' ? 'broken' : ''}`} aria-label={`Seleccionar ${item.id}`} aria-pressed={item.id === selectedId} onClick={() => select(item.id)}><span className="list-vehicle-icon" style={{ background: item.estado === 'AVERIADO' ? MAP_THEME.averiado : COLORES_TIPO[item.tipo] }}><Icon name={ICONOS_TIPO[item.tipo]} /></span><span className="entity-main"><strong>{item.id} <small>{NOMBRES_TIPO[item.tipo]}</small></strong><span>{NOMBRES_ESTADO_VEHICULO[item.estado]}{item.carga ? ` · ${item.carga}/${item.capacidad} paq.` : ''}{item.proximaParada ? ` · → ${item.proximaParada.pedidoId}` : ''}</span></span><Signal semaforo={item.semaforo} /></button>)}{!vehicles.length && <p className="no-results">No se encontraron vehículos.</p>}</div></div>
          : tab === 'pedidos' ? <div role="tabpanel" id="orders-list" aria-labelledby="orders-tab"><label className="map-search"><Icon name="search" /><input type="search" aria-label="Buscar pedido por ID o cliente" placeholder="Buscar pedido o cliente…" value={orderQuery} onChange={(e) => setOrderQuery(e.target.value)} /></label><label className="order-filter">Estado<select aria-label="Filtrar pedidos por estado" value={orderState} onChange={(e) => setOrderState(e.target.value as EstadoPedido | '')}><option value="">Todos los estados</option>{(Object.entries(NOMBRES_ESTADO_PEDIDO) as [EstadoPedido, string][]).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label><div className="entity-list">{orders.map((item) => <button type="button" key={item.id} className="entity-row order-row" aria-label={`Centrar pedido ${item.id}`} onClick={() => { setSelectedId(item.vehiculoId && item.estado !== 'ENTREGADO' ? item.vehiculoId : null); map.current?.center(item.x, item.y) }}><span className="entity-main"><strong>{item.id} <small>{item.cantidad} paq. · {item.clienteId}</small></strong><span>{NOMBRES_ESTADO_PEDIDO[item.estado]}{item.vehiculoId ? ` · ${item.vehiculoId}` : ''} · ({item.x}, {item.y})</span><span>Plazo: {hora(item.deadline)}{item.estado === 'ENTREGADO' ? ` · Entregado ${hora(item.entregadoEn).replace(/^Día \d+ · /, '')}` : item.eta ? ` · ETA ${hora(item.eta).replace(/^Día \d+ · /, '')}` : ''}</span></span><Signal semaforo={item.semaforo} /></button>)}{!orders.length && <p className="no-results">No se encontraron pedidos.</p>}</div></div> : null}
      </section>
      {panelLateral && <section className="map-panel-extra">{panelLateral}</section>}
    </aside>
  </section>
}
