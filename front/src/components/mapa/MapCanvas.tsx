import { useCallback, useEffect, useImperativeHandle, useRef, type Ref } from 'react'
import { ALMACENES, GRID_ALTO, GRID_ANCHO, GRILLA_INTERVALO } from '../../config/defaults'
import { COLORES_SEMAFORO, COLORES_TIPO, MAP_THEME, NOMBRES_ESTADO_PEDIDO, NOMBRES_ESTADO_VEHICULO, NOMBRES_TIPO } from '../../config/theme'
import type { AlmacenSnap, BloqueoSnap, PedidoSnap, Snapshot, TipoVehiculo, VehiculoSnap } from '../../types/snapshot'
import { readableSimTime } from '../../services/simulationTime'
import { distanceToSegment, movementPath, pointOnPath, project, remainingPath, unproject, type Point, type Viewport } from './geometry'

export interface MapCanvasHandle { center: (x: number, y: number) => void; zoom: (factor: number) => void; reset: () => void }
/** Capas que el usuario puede ocultar desde la leyenda. */
export interface CapasMapa { rutas: boolean; pedidos: boolean; entregados: boolean; bloqueos: boolean }
interface Motion { path: Point[]; start: number; duration: number; vehicle: VehiculoSnap }
type Hit = { kind: 'vehiculo'; item: VehiculoSnap } | { kind: 'pedido'; item: PedidoSnap } | { kind: 'almacen'; item: AlmacenSnap; parked: VehiculoSnap[] } | { kind: 'bloqueo'; item: BloqueoSnap }
interface Props {
  snapshot: Snapshot; selectedId: string | null; onSelect: (id: string | null) => void; ref?: Ref<MapCanvasHandle>
  capas: CapasMapa
  seleccionandoPunto?: boolean
  onElegirPunto?: (x: number, y: number) => void
  onCancelarSeleccion?: () => void
  puntoElegido?: { x: number; y: number } | null
}
const TIPOS: TipoVehiculo[] = ['AUTO', 'MOTO', 'BICICLETA']
const FONT = 'Segoe UI, sans-serif'

function isParked(vehicle: VehiculoSnap, almacenes: AlmacenSnap[]) {
  return vehicle.estado === 'LIBRE' && almacenes.some((a) => Math.abs(a.x - vehicle.x) < 0.01 && Math.abs(a.y - vehicle.y) < 0.01)
}

export function MapCanvas({ snapshot, selectedId, onSelect, ref, capas, seleccionandoPunto, onElegirPunto, onCancelarSeleccion, puntoElegido }: Props) {
  const canvas = useRef<HTMLCanvasElement>(null)
  const scene = useRef({ snapshot, selectedId, onSelect, capas, seleccionandoPunto, onElegirPunto, onCancelarSeleccion, puntoElegido })
  const motions = useRef(new Map<string, Motion>())
  const lastSnapshotAt = useRef(0)
  const view = useRef<Viewport>({ width: 0, height: 0, scale: 1, offsetX: 0, offsetY: 0 })
  const zoom = useRef(1)
  const focus = useRef<Point>([GRID_ANCHO / 2, GRID_ALTO / 2])
  const fit = useRef(1)
  const hover = useRef<Point | null>(null)

  useEffect(() => { if (seleccionandoPunto) canvas.current?.focus() }, [seleccionandoPunto])

  const placeView = useCallback(() => {
    const v = view.current
    v.scale = fit.current * zoom.current
    v.offsetX = v.width / 2 - focus.current[0] * v.scale
    v.offsetY = v.height / 2 + focus.current[1] * v.scale
  }, [])
  const zoomAt = useCallback((factor: number, anchor?: Point) => {
    const v = view.current
    const screen: Point = anchor ?? [v.width / 2, v.height / 2]
    const before = unproject(screen, v)
    zoom.current = Math.max(0.6, Math.min(10, zoom.current * factor))
    placeView()
    const after = unproject(screen, v)
    focus.current = [focus.current[0] + before[0] - after[0], focus.current[1] + before[1] - after[1]]
    placeView()
  }, [placeView])
  useImperativeHandle(ref, () => ({
    center: (x, y) => { focus.current = [x, y]; placeView() },
    zoom: (factor) => zoomAt(factor),
    reset: () => { zoom.current = 1; focus.current = [GRID_ANCHO / 2, GRID_ALTO / 2]; placeView() },
  }))

  // Cada snapshot nuevo anima a cada unidad desde donde se ve ahora hasta su nueva posición, siguiendo su ruta.
  useEffect(() => {
    const now = performance.now()
    const interval = lastSnapshotAt.current ? Math.max(250, Math.min(2000, now - lastSnapshotAt.current)) : 0
    lastSnapshotAt.current = now
    const next = new Map<string, Motion>()
    const newRun = scene.current.snapshot.runId !== snapshot.runId
    for (const vehicle of snapshot.vehiculos) {
      const previous = newRun ? undefined : motions.current.get(vehicle.id)
      const to: Point = [vehicle.x, vehicle.y]
      if (!previous) { next.set(vehicle.id, { path: [to], start: now, duration: 0, vehicle }); continue }
      const progress = previous.duration ? (now - previous.start) / previous.duration : 1
      const tail = remainingPath(previous.path, progress)
      const prevTarget: Point = [previous.vehicle.x, previous.vehicle.y]
      const trace = movementPath(prevTarget, to, previous.vehicle.ruta)
      const moved = trace.length > 1 || Math.abs(prevTarget[0] - to[0]) > 1e-3 || Math.abs(prevTarget[1] - to[1]) > 1e-3
      const path = !moved ? tail : trace.length === 1 ? [to] : [...tail, ...trace.slice(1)]
      next.set(vehicle.id, { path, start: now, duration: path.length > 1 ? interval : 0, vehicle })
    }
    motions.current = next
    scene.current = { snapshot, selectedId, onSelect, capas, seleccionandoPunto, onElegirPunto, onCancelarSeleccion, puntoElegido }
  }, [snapshot]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    scene.current = { ...scene.current, selectedId, onSelect, capas, seleccionandoPunto, onElegirPunto, onCancelarSeleccion, puntoElegido }
  }, [selectedId, onSelect, capas, seleccionandoPunto, onElegirPunto, onCancelarSeleccion, puntoElegido])

  useEffect(() => {
    const element = canvas.current!
    const ctx = element.getContext('2d')!
    let frame = 0
    let pointer: { x: number; y: number; focus: Point; dragged: boolean } | null = null
    let hits: { hit: Hit; screen: Point; radius: number }[] = []
    let lines: { hit: Hit; a: Point; b: Point }[] = []

    function resize() {
      const rect = element.getBoundingClientRect()
      const dpr = window.devicePixelRatio || 1
      element.width = Math.round(rect.width * dpr); element.height = Math.round(rect.height * dpr)
      view.current.width = rect.width; view.current.height = rect.height
      fit.current = Math.max(1, Math.min((rect.width - 110) / GRID_ANCHO, (rect.height - 120) / GRID_ALTO))
      placeView()
    }
    const observer = new ResizeObserver(resize)
    observer.observe(element)
    resize()

    function label(text: string, x: number, y: number, background = 'white', color = MAP_THEME.texto, bold = false) {
      ctx.font = `${bold ? '600 ' : ''}10px ${FONT}`; ctx.textAlign = 'center'; ctx.textBaseline = 'alphabetic'
      const width = ctx.measureText(text).width + 12
      ctx.fillStyle = background; ctx.beginPath(); ctx.roundRect(x - width / 2, y - 11, width, 16, 4); ctx.fill()
      ctx.strokeStyle = MAP_THEME.borde; ctx.lineWidth = 0.6; ctx.stroke()
      ctx.fillStyle = color; ctx.fillText(text, x, y + 1)
    }
    function polyline(points: Point[], v: Viewport) {
      ctx.beginPath()
      points.forEach((p, i) => { const [x, y] = project(p, v); if (i) ctx.lineTo(x, y); else ctx.moveTo(x, y) })
    }
    function vehicleIcon(vehicle: VehiculoSnap, x: number, y: number, selected: boolean) {
      const broken = vehicle.estado === 'AVERIADO'
      ctx.save(); ctx.translate(x, y)
      if (selected) { ctx.beginPath(); ctx.arc(0, 0, 21, 0, Math.PI * 2); ctx.fillStyle = `${COLORES_TIPO[vehicle.tipo]}22`; ctx.fill() }
      ctx.beginPath(); ctx.arc(0, 0, selected ? 15 : 12.5, 0, Math.PI * 2)
      ctx.fillStyle = 'white'; ctx.fill(); ctx.strokeStyle = broken ? MAP_THEME.averiado : COLORES_SEMAFORO[vehicle.semaforo]; ctx.lineWidth = selected ? 3.2 : 2.4; ctx.stroke()
      ctx.beginPath(); ctx.arc(0, 0, 9.5, 0, Math.PI * 2); ctx.fillStyle = broken ? MAP_THEME.averiado : COLORES_TIPO[vehicle.tipo]; ctx.fill()
      ctx.strokeStyle = 'white'; ctx.lineWidth = 1.2; ctx.lineJoin = 'round'; ctx.beginPath()
      if (vehicle.tipo === 'AUTO') {
        ctx.moveTo(-6, 3); ctx.lineTo(-6, -2); ctx.lineTo(-4, -5); ctx.lineTo(4, -5); ctx.lineTo(6, -2); ctx.lineTo(6, 3); ctx.closePath()
        ctx.moveTo(-5, -1); ctx.lineTo(5, -1); ctx.moveTo(-4, 3); ctx.lineTo(-4, 5); ctx.moveTo(4, 3); ctx.lineTo(4, 5)
      } else {
        ctx.arc(-5, 3, 2.6, 0, Math.PI * 2); ctx.moveTo(7.6, 3); ctx.arc(5, 3, 2.6, 0, Math.PI * 2)
        ctx.moveTo(-5, 3); ctx.lineTo(-1, -3); ctx.lineTo(3, 3); ctx.lineTo(-5, 3)
        ctx.moveTo(5, 3); ctx.lineTo(2, -5); ctx.lineTo(5, -5)
        if (vehicle.tipo === 'MOTO') { ctx.moveTo(-5, -2); ctx.lineTo(0, -2) }
      }
      ctx.stroke()
      if (broken) {
        ctx.beginPath(); ctx.arc(9, -9, 6, 0, Math.PI * 2); ctx.fillStyle = 'white'; ctx.fill(); ctx.strokeStyle = MAP_THEME.averiado; ctx.lineWidth = 1.5; ctx.stroke()
        ctx.fillStyle = MAP_THEME.averiado; ctx.font = `700 9px ${FONT}`; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'; ctx.fillText('!', 9, -8.5)
      } else if (vehicle.estado === 'ATENDIENDO') {
        ctx.beginPath(); ctx.arc(9, -9, 5, 0, Math.PI * 2); ctx.fillStyle = COLORES_SEMAFORO.VERDE; ctx.fill()
        ctx.strokeStyle = 'white'; ctx.lineWidth = 1.4; ctx.beginPath(); ctx.moveTo(6.8, -9); ctx.lineTo(8.6, -7.2); ctx.lineTo(11.3, -10.6); ctx.stroke()
      }
      ctx.restore()
    }
    function orderMarker(pedido: PedidoSnap, x: number, y: number, size: number) {
      const color = COLORES_SEMAFORO[pedido.semaforo]
      if (pedido.estado === 'ENTREGADO') {
        ctx.beginPath(); ctx.arc(x, y, size * 0.75, 0, Math.PI * 2); ctx.fillStyle = '#e8edf3'; ctx.fill(); ctx.strokeStyle = MAP_THEME.entregado; ctx.lineWidth = 1; ctx.stroke()
        ctx.beginPath(); ctx.moveTo(x - size * 0.35, y); ctx.lineTo(x - size * 0.05, y + size * 0.3); ctx.lineTo(x + size * 0.4, y - size * 0.3); ctx.strokeStyle = MAP_THEME.entregado; ctx.lineWidth = 1.2; ctx.stroke()
        return
      }
      // Pin: pendiente = hueco (aún sin unidad); planificado/en ruta = relleno.
      ctx.beginPath(); ctx.moveTo(x, y); ctx.arc(x, y - size * 1.6, size, Math.PI * 0.78, Math.PI * 2.22); ctx.closePath()
      ctx.fillStyle = pedido.estado === 'PENDIENTE' ? 'white' : color; ctx.fill()
      ctx.strokeStyle = pedido.estado === 'PENDIENTE' ? color : 'white'; ctx.lineWidth = pedido.estado === 'PENDIENTE' ? 2 : 1.2; ctx.stroke()
      ctx.beginPath(); ctx.arc(x, y - size * 1.6, size * 0.38, 0, Math.PI * 2); ctx.fillStyle = pedido.estado === 'PENDIENTE' ? color : 'white'; ctx.fill()
      if (pedido.atrasado) { ctx.beginPath(); ctx.arc(x, y - size * 1.6, size + 3, 0, Math.PI * 2); ctx.strokeStyle = COLORES_SEMAFORO.ROJO; ctx.lineWidth = 1.5; ctx.setLineDash([2, 2]); ctx.stroke(); ctx.setLineDash([]) }
    }
    function warehouseIcon(almacen: AlmacenSnap, x: number, y: number, parked: VehiculoSnap[]) {
      const config = ALMACENES.find((a) => a.id === almacen.id)
      ctx.fillStyle = MAP_THEME.almacen; ctx.beginPath(); ctx.roundRect(x - 17, y - 17, 34, 34, 8); ctx.fill()
      ctx.strokeStyle = 'white'; ctx.lineWidth = 1.5; ctx.beginPath()
      ctx.moveTo(x - 10, y + 9); ctx.lineTo(x - 10, y - 4); ctx.lineTo(x, y - 10); ctx.lineTo(x + 10, y - 4); ctx.lineTo(x + 10, y + 9); ctx.closePath()
      ctx.moveTo(x - 4, y + 9); ctx.lineTo(x - 4, y); ctx.lineTo(x + 4, y); ctx.lineTo(x + 4, y + 9); ctx.stroke()
      label(config?.nombre ?? almacen.id, x, y + 31, 'white', MAP_THEME.texto, true)
      // Stock como barra de llenado (∞ en el central).
      if (almacen.stock === null) label('Stock ∞', x, y + 49, '#e9eff6')
      else {
        const capacidad = almacen.capacidad ?? config?.capacidad ?? 1000
        const ratio = Math.max(0, Math.min(1, almacen.stock / (Number.isFinite(capacidad) ? capacidad : 1)))
        ctx.fillStyle = '#e9eff6'; ctx.beginPath(); ctx.roundRect(x - 34, y + 39, 68, 15, 4); ctx.fill(); ctx.strokeStyle = MAP_THEME.borde; ctx.lineWidth = 0.6; ctx.stroke()
        ctx.fillStyle = ratio > 0.3 ? '#9fd8c3' : ratio > 0.1 ? '#f7d58a' : '#f5a3ab'; ctx.beginPath(); ctx.roundRect(x - 33, y + 40, 66 * ratio, 13, 3); ctx.fill()
        ctx.fillStyle = MAP_THEME.texto; ctx.font = `10px ${FONT}`; ctx.textAlign = 'center'; ctx.textBaseline = 'alphabetic'; ctx.fillText(`${almacen.stock.toLocaleString('es-PE')}/${capacidad.toLocaleString('es-PE')}`, x, y + 50)
      }
      // Unidades estacionadas: una ficha por tipo en lugar de 37 íconos superpuestos.
      const counts = TIPOS.map((tipo) => [tipo, parked.filter((p) => p.tipo === tipo).length] as const).filter(([, n]) => n > 0)
      if (counts.length) {
        const width = counts.length * 30 + 6
        ctx.fillStyle = 'white'; ctx.beginPath(); ctx.roundRect(x - width / 2, y - 39, width, 17, 9); ctx.fill(); ctx.strokeStyle = MAP_THEME.borde; ctx.lineWidth = 0.8; ctx.stroke()
        counts.forEach(([tipo, n], i) => {
          const cx = x - width / 2 + 11 + i * 30
          ctx.beginPath(); ctx.arc(cx, y - 30.5, 5, 0, Math.PI * 2); ctx.fillStyle = COLORES_TIPO[tipo]; ctx.fill()
          ctx.fillStyle = MAP_THEME.texto; ctx.font = `600 10px ${FONT}`; ctx.textAlign = 'left'; ctx.textBaseline = 'middle'; ctx.fillText(String(n), cx + 8, y - 30)
        })
      }
    }
    function grid(v: Viewport) {
      const [x0, y0] = project([0, GRID_ALTO], v), [x1, y1] = project([GRID_ANCHO, 0], v)
      ctx.fillStyle = MAP_THEME.lienzo; ctx.fillRect(x0, y0, x1 - x0, y1 - y0)
      // Calles cada 1 km cuando el zoom permite distinguirlas.
      if (v.scale >= 7) {
        ctx.strokeStyle = MAP_THEME.grilla; ctx.lineWidth = 0.6; ctx.globalAlpha = Math.min(1, (v.scale - 7) / 8 + 0.35); ctx.beginPath()
        for (let x = 0; x <= GRID_ANCHO; x++) if (x % GRILLA_INTERVALO) { const a = project([x, 0], v), b = project([x, GRID_ALTO], v); ctx.moveTo(...a); ctx.lineTo(...b) }
        for (let y = 0; y <= GRID_ALTO; y++) if (y % GRILLA_INTERVALO) { const a = project([0, y], v), b = project([GRID_ANCHO, y], v); ctx.moveTo(...a); ctx.lineTo(...b) }
        ctx.stroke(); ctx.globalAlpha = 1
      }
      ctx.strokeStyle = MAP_THEME.grillaMayor; ctx.lineWidth = 1; ctx.beginPath()
      for (let x = 0; x <= GRID_ANCHO; x += GRILLA_INTERVALO) { const a = project([x, 0], v), b = project([x, GRID_ALTO], v); ctx.moveTo(...a); ctx.lineTo(...b) }
      for (let y = 0; y <= GRID_ALTO; y += GRILLA_INTERVALO) { const a = project([0, y], v), b = project([GRID_ANCHO, y], v); ctx.moveTo(...a); ctx.lineTo(...b) }
      ctx.stroke()
      ctx.strokeStyle = MAP_THEME.borde; ctx.lineWidth = 1.2; ctx.strokeRect(x0, y0, x1 - x0, y1 - y0)
      ctx.fillStyle = '#7b8ca3'; ctx.font = `9px Consolas, monospace`
      ctx.textAlign = 'center'; ctx.textBaseline = 'top'
      for (let x = 0; x <= GRID_ANCHO; x += GRILLA_INTERVALO) { const [px, py] = project([x, 0], v); ctx.fillText(String(x), px, py + 4) }
      ctx.textAlign = 'right'; ctx.textBaseline = 'middle'
      for (let y = 0; y <= GRID_ALTO; y += GRILLA_INTERVALO) { const [px, py] = project([0, y], v); ctx.fillText(String(y), px - 5, py) }
    }
    function tooltip(hit: Hit, at: Point) {
      const rows: string[] = []
      if (hit.kind === 'vehiculo') {
        const item = hit.item
        rows.push(`${item.id} · ${NOMBRES_TIPO[item.tipo]}`, `${NOMBRES_ESTADO_VEHICULO[item.estado]} · ${item.carga}/${item.capacidad} paq.`)
        if (item.averia) rows.push(`Avería tipo ${item.averia.tipo} hasta ${readableSimTime(item.averia.hasta)}`)
        else if (item.proximaParada) rows.push(`→ ${item.proximaParada.pedidoId} · ETA ${item.proximaParada.eta ? readableSimTime(item.proximaParada.eta) : '—'}`)
        else if (item.estado === 'REGRESANDO') rows.push(`→ ${ALMACENES.find((a) => a.id === item.almacenBase)?.nombre ?? item.almacenBase}`)
      } else if (hit.kind === 'pedido') {
        const item = hit.item
        rows.push(`${item.id} · ${item.cantidad} paq. · ${item.clienteId}`, `${NOMBRES_ESTADO_PEDIDO[item.estado]}${item.vehiculoId ? ` · ${item.vehiculoId}` : ''}`, `Plazo: ${readableSimTime(item.deadline)}`)
        if (item.estado === 'ENTREGADO' && item.entregadoEn) rows.push(`Entregado: ${readableSimTime(item.entregadoEn)}`)
        else if (item.eta) rows.push(`ETA: ${readableSimTime(item.eta)}`)
      } else if (hit.kind === 'almacen') {
        const config = ALMACENES.find((a) => a.id === hit.item.id)
        rows.push(config?.nombre ?? hit.item.id, hit.item.stock === null ? 'Stock infinito' : `Stock: ${hit.item.stock} unidades`, `Unidades estacionadas: ${hit.parked.length}`)
      } else {
        rows.push(`Bloqueo ${hit.item.id}`, `${readableSimTime(hit.item.inicio)} → ${readableSimTime(hit.item.fin)}`)
      }
      ctx.font = `11px ${FONT}`
      const width = Math.max(...rows.map((r) => ctx.measureText(r).width)) + 18, height = rows.length * 15 + 10
      const x = Math.min(at[0] + 14, view.current.width - width - 6), y = Math.min(at[1] + 14, view.current.height - height - 6)
      ctx.fillStyle = 'rgba(16, 36, 62, 0.94)'; ctx.beginPath(); ctx.roundRect(x, y, width, height, 6); ctx.fill()
      ctx.textAlign = 'left'; ctx.textBaseline = 'top'
      rows.forEach((row, i) => { ctx.fillStyle = i ? '#d5e2f3' : 'white'; ctx.font = `${i ? '' : '600 '}11px ${FONT}`; ctx.fillText(row, x + 9, y + 6 + i * 15) })
    }

    function draw(now: number) {
      const v = view.current, current = scene.current, s = current.snapshot, capasActivas = current.capas
      const dpr = window.devicePixelRatio || 1
      if (element.width !== Math.round(v.width * dpr)) resize()
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
      ctx.fillStyle = MAP_THEME.fondo; ctx.fillRect(0, 0, v.width, v.height)
      grid(v)
      hits = []; lines = []
      const positions = new Map<string, Point>()
      for (const [id, motion] of motions.current) positions.set(id, pointOnPath(motion.path, motion.duration ? (now - motion.start) / motion.duration : 1))
      const tailOf = (vehicle: VehiculoSnap): Point[] => {
        const motion = motions.current.get(vehicle.id)
        const pending = motion ? remainingPath(motion.path, motion.duration ? (now - motion.start) / motion.duration : 1) : [[vehicle.x, vehicle.y] as Point]
        return [...pending, ...vehicle.ruta.slice(1)]
      }

      // 1. Bloqueos vigentes.
      if (capasActivas.bloqueos) for (const bloqueo of s.bloqueos ?? []) {
        polyline(bloqueo.puntos, v)
        ctx.lineCap = 'round'; ctx.lineJoin = 'round'
        ctx.strokeStyle = MAP_THEME.bloqueo; ctx.lineWidth = Math.max(4, Math.min(9, v.scale * 0.45)); ctx.stroke()
        ctx.strokeStyle = 'white'; ctx.lineWidth = 1.2; ctx.setLineDash([4, 4]); ctx.stroke(); ctx.setLineDash([])
        for (const end of [bloqueo.puntos[0], bloqueo.puntos[bloqueo.puntos.length - 1]]) {
          const [x, y] = project(end, v); ctx.beginPath(); ctx.arc(x, y, 4.5, 0, Math.PI * 2); ctx.fillStyle = MAP_THEME.bloqueo; ctx.fill(); ctx.strokeStyle = 'white'; ctx.lineWidth = 1.5; ctx.stroke()
        }
        for (let i = 1; i < bloqueo.puntos.length; i++) lines.push({ hit: { kind: 'bloqueo', item: bloqueo }, a: project(bloqueo.puntos[i - 1], v), b: project(bloqueo.puntos[i], v) })
      }
      ctx.lineCap = 'butt'

      // 2. Rutas de la flota (tenues) y la de la unidad seleccionada (resaltada, con paradas numeradas).
      const selected = s.vehiculos.find((vehicle) => vehicle.id === current.selectedId)
      if (capasActivas.rutas) for (const vehicle of s.vehiculos) {
        if (vehicle === selected || vehicle.ruta.length < 2) continue
        polyline(tailOf(vehicle), v)
        ctx.strokeStyle = COLORES_TIPO[vehicle.tipo]; ctx.globalAlpha = 0.32; ctx.lineWidth = 1.6; ctx.lineJoin = 'round'; ctx.stroke(); ctx.globalAlpha = 1
      }
      if (selected && selected.ruta.length > 1) {
        polyline(tailOf(selected), v)
        ctx.strokeStyle = 'white'; ctx.lineWidth = 6; ctx.lineJoin = 'round'; ctx.stroke()
        ctx.strokeStyle = COLORES_TIPO[selected.tipo]; ctx.lineWidth = 3; ctx.setLineDash([8, 5]); ctx.stroke(); ctx.setLineDash([])
        const last = selected.ruta[selected.ruta.length - 1]
        if (selected.estado === 'REGRESANDO' && !(selected.paradas?.length)) { const [x, y] = project(last, v); ctx.beginPath(); ctx.arc(x, y, 5, 0, Math.PI * 2); ctx.fillStyle = COLORES_TIPO[selected.tipo]; ctx.fill() }
      }

      // 3. Pedidos (destinos), coloreados por semáforo.
      const pinSize = Math.max(4, Math.min(7, v.scale * 0.45))
      if (capasActivas.pedidos) for (const pedido of s.pedidos) {
        if (pedido.estado === 'ENTREGADO' && !capasActivas.entregados) continue
        const [x, y] = project([pedido.x, pedido.y], v)
        orderMarker(pedido, x, y, pinSize)
        hits.push({ hit: { kind: 'pedido', item: pedido }, screen: pedido.estado === 'ENTREGADO' ? [x, y] : [x, y - pinSize * 1.6], radius: pinSize + 3 })
      }
      if (selected?.paradas?.length) selected.paradas.forEach((parada, index) => {
        const [x, y] = project([parada.x, parada.y], v)
        ctx.beginPath(); ctx.arc(x, y, 9, 0, Math.PI * 2); ctx.fillStyle = COLORES_TIPO[selected.tipo]; ctx.fill(); ctx.strokeStyle = 'white'; ctx.lineWidth = 2; ctx.stroke()
        ctx.fillStyle = 'white'; ctx.font = `700 10px ${FONT}`; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'; ctx.fillText(String(index + 1), x, y + 0.5)
        if (parada.eta) label(`ETA ${readableSimTime(parada.eta).replace(/^Día \d+ · /, '')}`, x, y - 15, '#f2f6fc')
      })

      // 4. Almacenes con stock y unidades estacionadas.
      const parked = new Map<string, VehiculoSnap[]>()
      for (const vehicle of s.vehiculos) if (isParked(vehicle, s.almacenes)) {
        const almacen = s.almacenes.find((a) => Math.abs(a.x - vehicle.x) < 0.01 && Math.abs(a.y - vehicle.y) < 0.01)!
        parked.set(almacen.id, [...(parked.get(almacen.id) ?? []), vehicle])
      }
      for (const almacen of s.almacenes) {
        const [x, y] = project([almacen.x, almacen.y], v)
        warehouseIcon(almacen, x, y, parked.get(almacen.id) ?? [])
        hits.push({ hit: { kind: 'almacen', item: almacen, parked: parked.get(almacen.id) ?? [] }, screen: [x, y], radius: 19 })
      }

      // 5. Unidades en operación; las detenidas sobre un almacén (averiada remolcada, seleccionada) se reparten alrededor.
      const drawn = s.vehiculos.filter((vehicle) => !isParked(vehicle, s.almacenes) || vehicle.id === current.selectedId)
      drawn.sort((a, b) => Number(a.id === current.selectedId) - Number(b.id === current.selectedId))
      const crowd = new Map<string, number>()
      for (const vehicle of drawn) {
        const position = positions.get(vehicle.id) ?? [vehicle.x, vehicle.y]
        let screen = project(position, v)
        const atWarehouse = s.almacenes.find((a) => Math.abs(a.x - position[0]) < 0.05 && Math.abs(a.y - position[1]) < 0.05)
        if (atWarehouse) {
          const n = crowd.get(atWarehouse.id) ?? 0
          crowd.set(atWarehouse.id, n + 1)
          const angle = -Math.PI / 4 + n * 0.75, center = project([atWarehouse.x, atWarehouse.y], v)
          screen = [center[0] + Math.cos(angle) * 34, center[1] + Math.sin(angle) * 34]
        }
        vehicleIcon(vehicle, screen[0], screen[1], vehicle.id === current.selectedId)
        hits.push({ hit: { kind: 'vehiculo', item: vehicle }, screen, radius: 15 })
      }

      const picked = current.puntoElegido
      if (picked) {
        const [x, y] = project([picked.x, picked.y], v)
        ctx.strokeStyle = '#2563eb'; ctx.lineWidth = 2; ctx.beginPath(); ctx.arc(x, y, 9, 0, Math.PI * 2); ctx.stroke()
        ctx.beginPath(); ctx.moveTo(x - 14, y); ctx.lineTo(x + 14, y); ctx.moveTo(x, y - 14); ctx.lineTo(x, y + 14); ctx.stroke()
        label(`Nuevo pedido (${picked.x}, ${picked.y})`, x, y - 22, '#eff6ff')
      }

      const hovered = hover.current && !pointer && !current.seleccionandoPunto ? hitAt(hover.current) : null
      if (hovered && hover.current) tooltip(hovered, hover.current)
      element.style.cursor = current.seleccionandoPunto ? 'crosshair' : pointer?.dragged ? 'grabbing' : hovered && (hovered.kind === 'vehiculo' || (hovered.kind === 'pedido' && hovered.item.vehiculoId)) ? 'pointer' : 'grab'
      frame = requestAnimationFrame(draw)
    }

    function hitAt(point: Point): Hit | null {
      const order: Hit['kind'][] = ['vehiculo', 'pedido', 'almacen']
      for (const kind of order) {
        const found = [...hits].reverse().find((h) => h.hit.kind === kind && Math.hypot(h.screen[0] - point[0], h.screen[1] - point[1]) <= h.radius)
        if (found) return found.hit
      }
      const line = lines.find((l) => distanceToSegment(point, l.a, l.b) <= 6)
      return line?.hit ?? null
    }
    function point(event: MouseEvent): Point { const rect = element.getBoundingClientRect(); return [event.clientX - rect.left, event.clientY - rect.top] }
    function wheel(event: WheelEvent) { event.preventDefault(); zoomAt(Math.exp(-event.deltaY * 0.0015), point(event)) }
    function down(event: PointerEvent) {
      if (event.button !== 0) return
      element.setPointerCapture(event.pointerId)
      pointer = { x: event.clientX, y: event.clientY, focus: [...focus.current], dragged: false }
    }
    function move(event: PointerEvent) {
      hover.current = point(event)
      if (!pointer) return
      const dx = event.clientX - pointer.x, dy = event.clientY - pointer.y
      if (Math.hypot(dx, dy) > 4) pointer.dragged = true
      if (pointer.dragged) { focus.current = [pointer.focus[0] - dx / view.current.scale, pointer.focus[1] + dy / view.current.scale]; placeView() }
    }
    function up(event: PointerEvent) {
      if (!pointer) return
      if (!pointer.dragged) {
        const at = point(event)
        if (scene.current.seleccionandoPunto) choosePoint(at)
        else {
          const hit = hitAt(at)
          scene.current.onSelect(hit?.kind === 'vehiculo' ? hit.item.id : hit?.kind === 'pedido' && hit.item.vehiculoId ? hit.item.vehiculoId : null)
        }
      }
      pointer = null
      if (element.hasPointerCapture(event.pointerId)) element.releasePointerCapture(event.pointerId)
    }
    function cancel() { pointer = null }
    function leave() { hover.current = null }
    function choosePoint(screen: Point) {
      const [x, y] = unproject(screen, view.current)
      scene.current.onElegirPunto?.(Math.max(0, Math.min(GRID_ANCHO, Math.round(x))), Math.max(0, Math.min(GRID_ALTO, Math.round(y))))
    }
    function key(event: KeyboardEvent) {
      if (event.key === 'Escape') { scene.current.onSelect(null); scene.current.onCancelarSeleccion?.() }
      else if (event.key === 'Enter' && scene.current.seleccionandoPunto) { event.preventDefault(); choosePoint([view.current.width / 2, view.current.height / 2]) }
      else if (event.key === '+' || event.key === '=') zoomAt(1.25)
      else if (event.key === '-') zoomAt(0.8)
      else if (['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'].includes(event.key)) {
        event.preventDefault()
        focus.current = [focus.current[0] + (event.key === 'ArrowRight' ? 3 : event.key === 'ArrowLeft' ? -3 : 0), focus.current[1] + (event.key === 'ArrowUp' ? 3 : event.key === 'ArrowDown' ? -3 : 0)]; placeView()
      }
    }
    element.addEventListener('wheel', wheel, { passive: false }); element.addEventListener('pointerdown', down); element.addEventListener('pointermove', move); element.addEventListener('pointerup', up); element.addEventListener('pointercancel', cancel); element.addEventListener('pointerleave', leave); element.addEventListener('keydown', key)
    frame = requestAnimationFrame(draw)
    return () => { cancelAnimationFrame(frame); observer.disconnect(); element.removeEventListener('wheel', wheel); element.removeEventListener('pointerdown', down); element.removeEventListener('pointermove', move); element.removeEventListener('pointerup', up); element.removeEventListener('pointercancel', cancel); element.removeEventListener('pointerleave', leave); element.removeEventListener('keydown', key) }
  }, [placeView, zoomAt])
  return <canvas ref={canvas} className={`logistics-canvas ${seleccionandoPunto ? 'picking-point' : ''}`} tabIndex={0} aria-label={seleccionandoPunto ? 'Elegir coordenadas: haz clic en el mapa o pulsa Enter para elegir el centro; Escape para cancelar.' : 'Mapa logístico interactivo. Pasa el cursor sobre unidades, pedidos, almacenes o bloqueos para ver su detalle; haz clic en una unidad para seguir su ruta. Arrastra para desplazar; usa la rueda o las teclas más y menos para ampliar.'} />
}
