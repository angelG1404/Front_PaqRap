import { useMemo, useRef, useState, type ReactNode } from 'react'
import { ALMACENES, GRID_ALTO, GRID_ANCHO } from '../../config/defaults'
import { COLORES_AVERIA, COLORES_CARGA, NOMBRES_CARGA, estadoCarga, type EstadoCarga, COLORES_SEMAFORO, COLORES_TIPO, ICONOS_TIPO, NOMBRES_ESTADO_PEDIDO, NOMBRES_ESTADO_VEHICULO, NOMBRES_TIPO } from '../../config/theme'
import type { EstadoConexion, EstadoPedido, EstadoVehiculo, Semaforo, Snapshot, TipoVehiculo, VehiculoSnap } from '../../types/snapshot'
import { readableSimTime } from '../../services/simulationTime'
import { Icon } from '../Icon'
import { MapCanvas, type CapasMapa, type MapCanvasHandle } from './MapCanvas'
import { compararOcupacion, porcentaje } from '../../services/ocupacion'
import { AlmacenesPanel } from './AlmacenesPanel'
import './mapa.css'
import './workspace.css'

export interface MapaEnVivoProps {
  snapshot: Snapshot | null
  estadoConexion: EstadoConexion
  escenario: '5D' | 'COLAPSO' | 'DIARIO'
  panelLateral?: ReactNode
  panelRegistrar?: ReactNode
  panelAverias?: ReactNode
  etiquetaRegistro?: string
  seleccionandoPunto?: boolean
  onElegirPunto?: (x: number, y: number) => void
  onCancelarSeleccion?: () => void
  puntoElegido?: { x: number; y: number } | null
}
function Signal({ semaforo }: { semaforo: Semaforo }) {
  return <span className="signal" style={{ color: COLORES_SEMAFORO[semaforo] }}><i style={{ background: COLORES_SEMAFORO[semaforo] }} />{semaforo === 'AMBAR' ? 'Ámbar' : semaforo === 'VERDE' ? 'Verde' : 'Rojo'}</span>
}
const CAPAS_VISIBLES: CapasMapa = { rutas: true, pedidos: true, entregados: true, bloqueos: true }
const hora = (simTime: string | null | undefined) => simTime ? readableSimTime(simTime) : '—'

function destino(vehicle: VehiculoSnap): string {
  if (vehicle.averia) return vehicle.averia.remolcado ? 'En Almacén Central (remolcada)' : 'Detenida en el lugar de la avería'
  if (vehicle.proximaParada) return `${vehicle.proximaParada.pedidoId} (${vehicle.proximaParada.x}, ${vehicle.proximaParada.y})`
  const base = ALMACENES.find((a) => a.id === vehicle.almacenBase)
  if (vehicle.estado === 'REGRESANDO' && base) return `${base.nombre} (${base.x}, ${base.y})`
  if (vehicle.estado === 'ATENDIENDO') return 'Entregando al cliente'
  return 'Sin destino asignado'
}

export function MapaEnVivo({ snapshot, estadoConexion, escenario, panelLateral, panelRegistrar, panelAverias, etiquetaRegistro = 'Registrar pedido', seleccionandoPunto, onElegirPunto, onCancelarSeleccion, puntoElegido }: MapaEnVivoProps) {
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [collapsed, setCollapsed] = useState(() => window.matchMedia('(max-width: 700px)').matches)
  const [tab, setTab] = useState<'registro' | 'averias' | 'vehiculos' | 'pedidos' | 'almacenes' | 'incidencias'>('vehiculos')
  const [ordenOcupacion, setOrdenOcupacion] = useState<'desc' | 'asc'>('desc')
  const [vehicleQuery, setVehicleQuery] = useState('')
  const [vehicleState, setVehicleState] = useState<EstadoVehiculo | ''>('')
  const [orderQuery, setOrderQuery] = useState('')
  const [orderState, setOrderState] = useState<EstadoPedido | ''>('')
  const [tipoFiltro, setTipoFiltro] = useState<TipoVehiculo | ''>('')
  const [cargaFiltro, setCargaFiltro] = useState<EstadoCarga | ''>('')
  const visibleSnapshot = useMemo(() => snapshot ? { ...snapshot, vehiculos: snapshot.vehiculos.filter((v) => (!tipoFiltro || v.tipo === tipoFiltro) && (!cargaFiltro || estadoCarga(v) === cargaFiltro)) } : null, [snapshot, tipoFiltro, cargaFiltro])
  const map = useRef<MapCanvasHandle>(null)
  const vehicle = visibleSnapshot?.vehiculos.find((item) => item.id === selectedId)
  const vehicles = visibleSnapshot?.vehiculos.filter((item) => item.id.toLowerCase().includes(vehicleQuery.toLowerCase().trim()) && (!vehicleState || item.estado === vehicleState)) ?? []
  vehicles.sort((a, b) => compararOcupacion(a, b, ordenOcupacion))
  const orders = snapshot?.pedidos.filter((item) => (item.id.toLowerCase().includes(orderQuery.toLowerCase().trim()) || item.clienteId.toLowerCase().includes(orderQuery.toLowerCase().trim())) && (!orderState || item.estado === orderState)) ?? []
  const assigned = snapshot?.pedidos.filter((pedido) => pedido.vehiculoId === selectedId && pedido.estado !== 'ENTREGADO') ?? []
  const paradas = vehicle?.paradas ?? []
  function select(id: string) {
    const item = snapshot?.vehiculos.find((v) => v.id === id) ?? snapshot?.almacenes.find((a) => a.id === id)
    setSelectedId(id)
    if (item) map.current?.center(item.x, item.y)
  }
  function selectOnMap(id: string | null) {
    setSelectedId(id)
    if (snapshot?.almacenes.some((a) => a.id === id)) {
      setTab('almacenes')
      setCollapsed(false)
    }
  }
  return <section className={`live-map workspace-map ${collapsed ? 'panel-collapsed' : ''} ${seleccionandoPunto ? 'picking-location' : ''}`} aria-label={`Mapa en vivo ${escenario}`}>
    <div className="map-stage">
      {visibleSnapshot ? <MapCanvas ref={map} snapshot={visibleSnapshot} capas={CAPAS_VISIBLES} selectedId={seleccionandoPunto ? null : selectedId} onSelect={selectOnMap} seleccionandoPunto={seleccionandoPunto} onElegirPunto={onElegirPunto} onCancelarSeleccion={onCancelarSeleccion} puntoElegido={puntoElegido} /> : <div className="map-empty" role="status">{estadoConexion === 'ERROR' ? 'No se pudo conectar con el servidor. Revisa que el backend esté levantado.' : 'Esperando datos del mapa…'}</div>}
      {seleccionandoPunto && <div className="map-pick-notice" role="status">Haz clic para elegir la celda más cercana.<button type="button" onClick={onCancelarSeleccion}>Cancelar</button></div>}
      <span className="axis axis-y">Eje Y: {GRID_ALTO} km</span><span className="axis axis-origin">(0, 0) km</span><span className="axis axis-x">Eje X: {GRID_ANCHO} km</span>
      <span className={`connection ${estadoConexion.toLowerCase()}`} role="status"><i />{estadoConexion === 'CONECTADO' ? 'Fuente conectada' : estadoConexion === 'CONECTANDO' ? 'Conectando…' : estadoConexion === 'ERROR' ? 'Error de conexión' : 'Desconectado'}</span>
      <div className="map-tools" aria-label="Controles de vista"><button type="button" aria-label="Acercar mapa" onClick={() => map.current?.zoom(1.25)}>+</button><button type="button" aria-label="Alejar mapa" onClick={() => map.current?.zoom(0.8)}>−</button><button type="button" aria-label="Centrar mapa completo" title="Centrar mapa completo" onClick={() => map.current?.reset()}><Icon name="target" /></button></div>
      <details className="map-legend map-legend-overlay"><summary>Leyenda <span>{GRID_ANCHO}×{GRID_ALTO} km</span></summary>
        <div className="legend-grid">{(Object.keys(COLORES_CARGA) as EstadoCarga[]).map((carga) => <div className="legend-item" key={carga}><i className="legend-load" style={{ background: COLORES_CARGA[carga] }} />{NOMBRES_CARGA[carga]}</div>)}
          <div className="legend-item"><span className="legend-symbol warehouse"><Icon name="warehouse" /></span>Almacén</div>
          <div className="legend-item"><span className="legend-symbol blocked" /> Bloqueo</div>
          {(Object.keys(COLORES_TIPO) as TipoVehiculo[]).map((tipo) => <div className="legend-item" key={tipo}><span className="legend-symbol" style={{ background: COLORES_TIPO[tipo] }}><Icon name={ICONOS_TIPO[tipo]} /></span>{NOMBRES_TIPO[tipo]}</div>)}
          {([1, 2, 3] as const).map((tipo) => <div className="legend-item" key={`averia-${tipo}`}><span className="legend-symbol" style={{ background: COLORES_AVERIA[tipo] }}>{tipo}</span>Avería {tipo}</div>)}
          <div className="legend-item"><span className="legend-pin pending" />Pedido sin unidad</div>
          <div className="legend-item"><span className="legend-pin" />Pedido asignado</div>
        </div>
      </details>
      <span className="map-help">Arrastra para mover · Rueda para ampliar · Pasa el cursor para ver detalles</span>
      {vehicle && !seleccionandoPunto && <section className="unit-panel" aria-label={`Unidad ${vehicle.id}`}>
        <div className="unit-heading"><span className="unit-icon" style={{ background: COLORES_CARGA[estadoCarga(vehicle)] }}><Icon name={ICONOS_TIPO[vehicle.tipo]} /></span><h2>{vehicle.id} <span>({NOMBRES_TIPO[vehicle.tipo]})</span></h2>{vehicle.proximaParada?.eta && <span className="unit-eta">ETA {hora(vehicle.proximaParada.eta).replace(/^Día \d+ · /, '')}</span>}<button type="button" className="icon-button" aria-label="Cerrar panel de unidad" onClick={() => setSelectedId(null)}><Icon name="close" /></button></div>
        <div className="unit-state"><span className={vehicle.estado === 'AVERIADO' ? 'broken' : ''}>{NOMBRES_ESTADO_VEHICULO[vehicle.estado]}</span><span style={{ color: COLORES_CARGA[estadoCarga(vehicle)] }}>Carga: {NOMBRES_CARGA[estadoCarga(vehicle)]}</span></div>
        {vehicle.averia && <p className="unit-breakdown">Avería tipo {vehicle.averia.tipo} desde {hora(vehicle.averia.desde)} · disponible {hora(vehicle.averia.hasta)}</p>}
        <dl><div><dt>Destino</dt><dd>{destino(vehicle)}</dd></div><div><dt>Carga</dt><dd>{vehicle.carga} / {vehicle.capacidad} paquetes</dd></div><div><dt>Almacén base</dt><dd>{ALMACENES.find((a) => a.id === vehicle.almacenBase)?.nombre}</dd></div>{vehicle.kmRecorridos !== undefined && <div><dt>Recorrido</dt><dd>{vehicle.kmRecorridos.toLocaleString('es-PE', { maximumFractionDigits: 1 })} km</dd></div>}</dl>
        <div className="assigned-orders"><h3>{paradas.length ? `RUTA PLANIFICADA (${paradas.length} paradas)` : `PEDIDOS ASIGNADOS (${assigned.length})`}</h3>
          {paradas.length ? paradas.map((parada, index) => <p key={parada.pedidoId}><span>{index + 1}. {parada.pedidoId}</span><span>{parada.cantidad ?? '—'} paq. · ETA {hora(parada.eta).replace(/^Día \d+ · /, '')}</span></p>)
            : assigned.length ? assigned.map((pedido) => <p key={pedido.id}><span>{pedido.id}</span><span>{pedido.cantidad} paq. · {NOMBRES_ESTADO_PEDIDO[pedido.estado]}</span></p>) : <p>Sin pedidos asignados</p>}
        </div>
      </section>}
    </div>
    <button type="button" className="collapse-panel" aria-label={collapsed ? 'Mostrar panel lateral' : 'Ocultar panel lateral'} aria-expanded={!collapsed} aria-controls="map-side-panel" onClick={() => setCollapsed(!collapsed)}>{collapsed ? '‹' : '›'}</button>
    <aside id="map-side-panel" className="map-side-panel workspace-panel" hidden={collapsed}>
      <section className="map-operations">
        <div className="map-tabs workspace-tabs" role="tablist" aria-label="Operaciones">
          <button type="button" role="tab" id="vehicles-tab" aria-selected={tab === 'vehiculos'} aria-controls="vehicles-list" onClick={() => { setTab('vehiculos'); onCancelarSeleccion?.() }}>Vehículos <small>{snapshot?.vehiculos.length ?? 0}</small></button>
          <button type="button" role="tab" id="orders-tab" aria-selected={tab === 'pedidos'} aria-controls="orders-list" onClick={() => { setTab('pedidos'); onCancelarSeleccion?.() }}>Pedidos <small>{snapshot?.pedidos.filter((p) => p.estado !== 'ENTREGADO').length ?? 0}</small></button>
          <button type="button" role="tab" id="warehouses-tab" aria-selected={tab === 'almacenes'} aria-controls="warehouses-list" onClick={() => { setTab('almacenes'); onCancelarSeleccion?.() }}>Almacenes <small>{snapshot?.almacenes.length ?? 0}</small></button>
          <button type="button" role="tab" id="incidents-tab" aria-selected={tab === 'incidencias'} aria-controls="incidents-list" onClick={() => { setTab('incidencias'); onCancelarSeleccion?.() }}>Incidencias <small>{snapshot?.vehiculos.filter((v) => v.averia).length ?? 0}</small></button>
        </div>
        {(tab === 'registro' || tab === 'averias') && <div className="panel-form-heading"><strong>{tab === 'registro' ? etiquetaRegistro : 'Registrar avería'}</strong><button type="button" aria-label="Cerrar formulario" onClick={() => { setTab('vehiculos'); onCancelarSeleccion?.() }}><Icon name="close" /></button></div>}
        {panelRegistrar && <div id="register-panel" aria-label={etiquetaRegistro} hidden={tab !== 'registro'} className="registration-scroll">{panelRegistrar}</div>}
        {panelAverias && <div id="breakdowns-panel" aria-label="Registro de avería" hidden={tab !== 'averias'} className="registration-scroll">{panelAverias}</div>}
        {tab === 'incidencias' && <div role="tabpanel" id="incidents-list" aria-labelledby="incidents-tab"><div className="entity-list">{snapshot?.vehiculos.filter((v) => v.averia).map((v) => <button type="button" className="entity-row" key={v.id} onClick={() => select(v.id)}><span className="entity-main"><strong>{v.id} · Avería tipo {v.averia!.tipo}</strong><span>Desde {hora(v.averia!.desde)}</span><span>Disponible {hora(v.averia!.hasta)}</span></span></button>)}{!snapshot?.vehiculos.some((v) => v.averia) && <p className="no-results">Sin averías activas.</p>}</div></div>}
        {tab === 'almacenes' && <AlmacenesPanel snapshot={snapshot} selectedId={selectedId} onSelect={select} />}
        {tab === 'vehiculos' ? <div role="tabpanel" id="vehicles-list" aria-labelledby="vehicles-tab"><details className="panel-filters"><summary>Filtros <span>{[tipoFiltro, cargaFiltro, vehicleQuery, vehicleState].filter(Boolean).length}</span></summary>      <section className="vehicle-filters" aria-label="Filtros globales de unidades">
        
        <p className="filter-label">Tipo de transporte</p>
        <div role="group" aria-label="Tipo de transporte"><button type="button" aria-pressed={!tipoFiltro} onClick={() => setTipoFiltro('')}>Todos</button>{(Object.keys(NOMBRES_TIPO) as TipoVehiculo[]).map((tipo) => <button type="button" key={tipo} aria-pressed={tipoFiltro === tipo} onClick={() => setTipoFiltro(tipo)}>{NOMBRES_TIPO[tipo]}</button>)}</div>
        <p className="filter-label">Carga disponible</p>
        <div role="group" aria-label="Carga disponible"><button type="button" aria-pressed={!cargaFiltro} onClick={() => setCargaFiltro('')}>Todos</button>{(Object.keys(COLORES_CARGA) as EstadoCarga[]).map((carga) => <button type="button" key={carga} aria-pressed={cargaFiltro === carga} onClick={() => setCargaFiltro(carga)}><i style={{ background: COLORES_CARGA[carga] }} />{NOMBRES_CARGA[carga]}</button>)}</div>
      </section>
<label className="map-search"><Icon name="search" /><input type="search" aria-label="Buscar vehículo por ID" placeholder="Buscar ID de vehículo…" value={vehicleQuery} onChange={(e) => setVehicleQuery(e.target.value)} /></label><label className="order-filter">Estado<select aria-label="Filtrar vehículos por estado" value={vehicleState} onChange={(e) => setVehicleState(e.target.value as EstadoVehiculo | '')}><option value="">Todos los estados</option>{(Object.entries(NOMBRES_ESTADO_VEHICULO) as [EstadoVehiculo, string][]).map(([value, label]) => <option key={value} value={value}>{label} ({snapshot?.vehiculos.filter((v) => v.estado === value).length ?? 0})</option>)}</select></label><label className="order-filter">Ocupación<select aria-label="Ordenar vehículos por ocupación" value={ordenOcupacion} onChange={(e) => setOrdenOcupacion(e.target.value as 'asc' | 'desc')}><option value="desc">Mayor ocupación primero</option><option value="asc">Menor ocupación primero</option></select></label></details><div className="entity-list">{vehicles.map((item) => <button type="button" key={item.id} className={`entity-row ${item.id === selectedId ? 'selected' : ''} ${item.estado === 'AVERIADO' ? 'broken' : ''}`} aria-label={`Seleccionar ${item.id}`} aria-pressed={item.id === selectedId} onClick={() => select(item.id)}><span className="list-vehicle-icon" style={{ background: COLORES_CARGA[estadoCarga(item)] }}><Icon name={ICONOS_TIPO[item.tipo]} /></span><span className="entity-main"><strong>{item.id} <small>{NOMBRES_TIPO[item.tipo]}</small></strong><span>{NOMBRES_ESTADO_VEHICULO[item.estado]}{` · ${item.carga}/${item.capacidad} paq. · ${porcentaje(item.carga, item.capacidad)}`}{item.proximaParada ? ` · → ${item.proximaParada.pedidoId}` : ''}</span></span><span style={{ color: COLORES_CARGA[estadoCarga(item)] }}>{NOMBRES_CARGA[estadoCarga(item)]}</span></button>)}{!vehicles.length && <p className="no-results">No se encontraron vehículos.</p>}</div></div>
          : tab === 'pedidos' ? <div role="tabpanel" id="orders-list" aria-labelledby="orders-tab"><details className="panel-filters"><summary>Filtros <span>{[orderQuery, orderState].filter(Boolean).length}</span></summary><label className="map-search"><Icon name="search" /><input type="search" aria-label="Buscar pedido por ID o cliente" placeholder="Buscar pedido o cliente…" value={orderQuery} onChange={(e) => setOrderQuery(e.target.value)} /></label><label className="order-filter">Estado<select aria-label="Filtrar pedidos por estado" value={orderState} onChange={(e) => setOrderState(e.target.value as EstadoPedido | '')}><option value="">Todos los estados</option>{(Object.entries(NOMBRES_ESTADO_PEDIDO) as [EstadoPedido, string][]).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label></details><div className="entity-list">{orders.map((item) => <button type="button" key={item.id} className="entity-row order-row" aria-label={`Centrar pedido ${item.id}`} onClick={() => { setSelectedId(item.vehiculoId && item.estado !== 'ENTREGADO' ? item.vehiculoId : null); map.current?.center(item.x, item.y) }}><span className="entity-main"><strong>{item.id} <small>{item.cantidad} paq. · {item.clienteId}</small></strong><span>{NOMBRES_ESTADO_PEDIDO[item.estado]}{item.vehiculoId ? ` · ${item.vehiculoId}` : ''} · ({item.x}, {item.y})</span><span>Plazo: {hora(item.deadline)}{item.estado === 'ENTREGADO' ? ` · Entregado ${hora(item.entregadoEn).replace(/^Día \d+ · /, '')}` : item.eta ? ` · ETA ${hora(item.eta).replace(/^Día \d+ · /, '')}` : ''}</span></span><Signal semaforo={item.semaforo} /></button>)}{!orders.length && <p className="no-results">No se encontraron pedidos.</p>}</div></div> : null}
      </section>
      {panelLateral && <section className="map-panel-extra">{panelLateral}</section>}
      {(panelRegistrar || panelAverias) && <footer className="panel-actions">
        {panelRegistrar && <button type="button" className="panel-primary-action" aria-expanded={tab === 'registro'} aria-controls="register-panel" onClick={() => { setTab('registro'); onCancelarSeleccion?.() }}>{etiquetaRegistro}</button>}
        {panelAverias && <button type="button" className="panel-incident-action" aria-expanded={tab === 'averias'} aria-controls="breakdowns-panel" onClick={() => { setTab('averias'); onCancelarSeleccion?.() }}>Registrar avería</button>}
      </footer>}
    </aside>
  </section>
}
