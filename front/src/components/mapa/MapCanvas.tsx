import { useCallback, useEffect, useImperativeHandle, useRef, type Ref } from 'react'
import { ALMACENES, GRID_ALTO, GRID_ANCHO, GRILLA_INTERVALO } from '../../config/defaults'
import { COLORES_SEMAFORO, COLORES_TIPO, MAP_THEME } from '../../config/theme'
import type { Snapshot, VehiculoSnap } from '../../types/snapshot'
import { movementPath, pointOnPath, project, unproject, type Point, type Viewport } from './geometry'

export interface MapCanvasHandle { center: (x: number, y: number) => void; zoom: (factor: number) => void; reset: () => void }
interface Motion { path: Point[]; start: number; duration: number; changedAt: number; vehicle: VehiculoSnap }
interface Props {
  snapshot: Snapshot; selectedId: string | null; onSelect: (id: string | null) => void; ref?: Ref<MapCanvasHandle>
  seleccionandoPunto?: boolean
  onElegirPunto?: (x: number, y: number) => void
  onCancelarSeleccion?: () => void
  puntoElegido?: { x: number; y: number } | null
}
export function MapCanvas({ snapshot, selectedId, onSelect, ref, seleccionandoPunto, onElegirPunto, onCancelarSeleccion, puntoElegido }: Props) {
  const canvas = useRef<HTMLCanvasElement>(null)
  const scene = useRef({ snapshot, selectedId, onSelect, seleccionandoPunto, onElegirPunto, onCancelarSeleccion, puntoElegido })
  const motions = useRef(new Map<string, Motion>())
  const view = useRef<Viewport>({ width: 0, height: 0, scale: 1, offsetX: 0, offsetY: 0 })
  const zoom = useRef(1)
  const focus = useRef<Point>([GRID_ANCHO / 2, GRID_ALTO / 2])
  const fit = useRef(1)
  const hits = useRef<{ id: string; screen: Point }[]>([])

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
    zoom.current = Math.max(0.6, Math.min(8, zoom.current * factor))
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

  useEffect(() => {
    const now = performance.now()
    const next = new Map<string, Motion>()
    const newRun = scene.current.snapshot.runId !== snapshot.runId
    for (const vehicle of snapshot.vehiculos) {
      const previous = newRun ? undefined : motions.current.get(vehicle.id)
      if (!previous) next.set(vehicle.id, { path: [[vehicle.x, vehicle.y]], start: now, duration: 0, changedAt: now, vehicle })
      else if (vehicle.x !== previous.vehicle.x || vehicle.y !== previous.vehicle.y) {
        const from = pointOnPath(previous.path, previous.duration ? (now - previous.start) / previous.duration : 1)
        const path = movementPath(from, [vehicle.x, vehicle.y], previous.vehicle.ruta)
        next.set(vehicle.id, { path, start: now, duration: Math.max(150, Math.min(5000, now - previous.changedAt)), changedAt: now, vehicle })
      } else next.set(vehicle.id, { ...previous, vehicle })
    }
    motions.current = next
    scene.current = { snapshot, selectedId, onSelect, seleccionandoPunto, onElegirPunto, onCancelarSeleccion, puntoElegido }
  }, [snapshot, selectedId, onSelect, seleccionandoPunto, onElegirPunto, onCancelarSeleccion, puntoElegido])

  useEffect(() => {
    const element = canvas.current!
    const ctx = element.getContext('2d')!
    let frame = 0
    let pointer: { x: number; y: number; focus: Point; dragged: boolean } | null = null
    function resize() {
      const rect = element.getBoundingClientRect()
      const dpr = window.devicePixelRatio || 1
      element.width = Math.round(rect.width * dpr); element.height = Math.round(rect.height * dpr)
      view.current.width = rect.width; view.current.height = rect.height
      fit.current = Math.max(1, Math.min((rect.width - 100) / GRID_ANCHO, (rect.height - 110) / GRID_ALTO))
      placeView()
    }
    const observer = new ResizeObserver(resize)
    observer.observe(element)
    window.addEventListener('resize', resize)
    resize()
    function label(text: string, x: number, y: number, background = 'white', color = MAP_THEME.texto) {
      ctx.font = '10px Segoe UI, sans-serif'; ctx.textAlign = 'center'
      const width = ctx.measureText(text).width + 12
      ctx.fillStyle = background; ctx.fillRect(x - width / 2, y - 10, width, 16)
      ctx.strokeStyle = MAP_THEME.borde; ctx.lineWidth = 0.6; ctx.strokeRect(x - width / 2, y - 10, width, 16)
      ctx.fillStyle = color; ctx.fillText(text, x, y + 1)
    }
    function vehicleIcon(vehicle: VehiculoSnap, x: number, y: number, selected: boolean) {
      ctx.save(); ctx.translate(x, y)
      ctx.beginPath(); ctx.arc(0, 0, selected ? 16 : 13, 0, Math.PI * 2)
      ctx.fillStyle = 'white'; ctx.fill(); ctx.strokeStyle = COLORES_SEMAFORO[vehicle.semaforo]; ctx.lineWidth = selected ? 3 : 2; ctx.stroke()
      ctx.beginPath(); ctx.arc(0, 0, 10, 0, Math.PI * 2); ctx.fillStyle = COLORES_TIPO[vehicle.tipo]; ctx.fill()
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
      ctx.stroke(); ctx.restore()
    }
    function draw(now: number) {
      const v = view.current, s = scene.current.snapshot
      const dpr = window.devicePixelRatio || 1
      if (element.width !== Math.round(v.width * dpr)) resize()
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
      ctx.clearRect(0, 0, v.width, v.height); ctx.fillStyle = MAP_THEME.fondo; ctx.fillRect(0, 0, v.width, v.height)
      ctx.strokeStyle = MAP_THEME.grilla; ctx.lineWidth = 1; ctx.beginPath()
      for (let x = 0; x <= GRID_ANCHO; x += GRILLA_INTERVALO) { const a = project([x, 0], v), b = project([x, GRID_ALTO], v); ctx.moveTo(...a); ctx.lineTo(...b) }
      for (let y = 0; y <= GRID_ALTO; y += GRILLA_INTERVALO) { const a = project([0, y], v), b = project([GRID_ANCHO, y], v); ctx.moveTo(...a); ctx.lineTo(...b) }
      ctx.stroke()
      const selected = s.vehiculos.find((vehicle) => vehicle.id === scene.current.selectedId)
      if (selected) {
        const motion = motions.current.get(selected.id)
        const pos = motion ? pointOnPath(motion.path, motion.duration ? (now - motion.start) / motion.duration : 1) : [selected.x, selected.y] as Point
        ctx.strokeStyle = COLORES_TIPO[selected.tipo]; ctx.lineWidth = 2; ctx.setLineDash([5, 3]); ctx.beginPath(); ctx.moveTo(...project(pos, v))
        // Connect along the motion path first, preserving orthogonal turns.
        const tail = motion ? movementPath(pos, [selected.x, selected.y], motion.path) : []
        for (const point of [...tail.slice(1), ...selected.ruta]) ctx.lineTo(...project(point, v))
        ctx.stroke(); ctx.setLineDash([])
      }
      for (const pedido of s.pedidos) {
        if (pedido.estado === 'ENTREGADO') continue
        const [x, y] = project([pedido.x, pedido.y], v)
        ctx.beginPath(); ctx.arc(x, y, 3.5, 0, Math.PI * 2); ctx.fillStyle = COLORES_SEMAFORO[pedido.semaforo]; ctx.fill(); ctx.strokeStyle = 'white'; ctx.lineWidth = 1; ctx.stroke()
      }
      for (const almacen of s.almacenes) {
        const [x, y] = project([almacen.x, almacen.y], v)
        const config = ALMACENES.find((a) => a.id === almacen.id)!
        ctx.fillStyle = MAP_THEME.almacen; ctx.beginPath(); ctx.roundRect(x - 18, y - 18, 36, 36, 8); ctx.fill()
        ctx.strokeStyle = 'white'; ctx.lineWidth = 1.5; ctx.beginPath()
        ctx.moveTo(x - 10, y + 9); ctx.lineTo(x - 10, y - 4); ctx.lineTo(x, y - 10); ctx.lineTo(x + 10, y - 4); ctx.lineTo(x + 10, y + 9); ctx.closePath()
        ctx.moveTo(x - 4, y + 9); ctx.lineTo(x - 4, y); ctx.lineTo(x + 4, y); ctx.lineTo(x + 4, y + 9); ctx.stroke()
        label(config.nombre, x, y + 30)
        label(almacen.stock === null ? '∞' : `${almacen.stock.toLocaleString('es-PE')}/${config.capacidad}`, x, y + 48, '#e9eff6')
      }
      hits.current = []
      // Selected unit last, so it stays clickable in a group of parked vehicles.
      const sorted = [...motions.current.values()].sort((a, b) => Number(a.vehicle.id === scene.current.selectedId) - Number(b.vehicle.id === scene.current.selectedId))
      for (const motion of sorted) {
        const pos = pointOnPath(motion.path, motion.duration ? (now - motion.start) / motion.duration : 1)
        const screen = project(pos, v)
        vehicleIcon(motion.vehicle, ...screen, motion.vehicle.id === scene.current.selectedId)
        hits.current.push({ id: motion.vehicle.id, screen })
      }
      const picked = scene.current.puntoElegido
      if (picked) {
        const [x, y] = project([picked.x, picked.y], v)
        ctx.strokeStyle = '#2563eb'; ctx.lineWidth = 2; ctx.beginPath(); ctx.arc(x, y, 9, 0, Math.PI * 2); ctx.stroke()
        ctx.beginPath(); ctx.moveTo(x - 14, y); ctx.lineTo(x + 14, y); ctx.moveTo(x, y - 14); ctx.lineTo(x, y + 14); ctx.stroke()
        label(`Nuevo pedido (${picked.x}, ${picked.y})`, x, y - 22, '#eff6ff')
      }
      frame = requestAnimationFrame(draw)
    }
    function point(event: MouseEvent): Point { const rect = element.getBoundingClientRect(); return [event.clientX - rect.left, event.clientY - rect.top] }
    function wheel(event: WheelEvent) { event.preventDefault(); zoomAt(Math.exp(-event.deltaY * 0.0015), point(event)) }
    function down(event: PointerEvent) {
      if (event.button !== 0) return
      element.setPointerCapture(event.pointerId)
      pointer = { x: event.clientX, y: event.clientY, focus: [...focus.current], dragged: false }
    }
    function move(event: PointerEvent) {
      if (!pointer) return
      const dx = event.clientX - pointer.x, dy = event.clientY - pointer.y
      if (Math.hypot(dx, dy) > 4) pointer.dragged = true
      if (pointer.dragged) { focus.current = [pointer.focus[0] - dx / view.current.scale, pointer.focus[1] + dy / view.current.scale]; placeView() }
    }
    function up(event: PointerEvent) {
      if (!pointer) return
      if (!pointer.dragged) {
        const [x, y] = point(event)
        if (scene.current.seleccionandoPunto) choosePoint([x, y])
        else {
          const hit = [...hits.current].reverse().find(({ screen }) => Math.hypot(screen[0] - x, screen[1] - y) <= 16)
          scene.current.onSelect(hit?.id ?? null)
        }
      }
      pointer = null
      if (element.hasPointerCapture(event.pointerId)) element.releasePointerCapture(event.pointerId)
    }
    function cancel() { pointer = null }
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
    element.addEventListener('wheel', wheel, { passive: false }); element.addEventListener('pointerdown', down); element.addEventListener('pointermove', move); element.addEventListener('pointerup', up); element.addEventListener('pointercancel', cancel); element.addEventListener('keydown', key)
    frame = requestAnimationFrame(draw)
    return () => { cancelAnimationFrame(frame); observer.disconnect(); window.removeEventListener('resize', resize); element.removeEventListener('wheel', wheel); element.removeEventListener('pointerdown', down); element.removeEventListener('pointermove', move); element.removeEventListener('pointerup', up); element.removeEventListener('pointercancel', cancel); element.removeEventListener('keydown', key) }
  }, [placeView, zoomAt])
  return <canvas ref={canvas} className={`logistics-canvas ${seleccionandoPunto ? 'picking-point' : ''}`} tabIndex={0} aria-label={seleccionandoPunto ? 'Elegir coordenadas: haz clic en el mapa o pulsa Enter para elegir el centro; Escape para cancelar.' : 'Mapa logístico interactivo. Selecciona unidades en el mapa o en la lista. Arrastra para desplazar; usa la rueda o las teclas más y menos para ampliar.'} />
}
