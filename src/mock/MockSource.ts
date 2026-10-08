import { ALMACENES, FLOTA, GRID_ALTO, GRID_ANCHO, HORIZONTE_5D, HORIZONTES_VALIDOS, MOCK_CONFIG, PORCENTAJE_MAXIMO, UMBRALES_SEMAFORO } from '../config/defaults'
import type { SnapshotSource } from '../services/snapshotSource'
import { formatSimTime, localIso, parseSimTime } from '../services/simulationTime'
import type { ParametrosCorrida } from '../types'
import type { PedidoSnap, Semaforo, Snapshot, TipoVehiculo, VehiculoSnap } from '../types/snapshot'

export function rutaOrtogonal(x: number, y: number, toX: number, toY: number): [number, number][] {
  const ruta: [number, number][] = [[x, y]]
  while (x !== toX) { x += Math.sign(toX - x); ruta.push([x, y]) }
  while (y !== toY) { y += Math.sign(toY - y); ruta.push([x, y]) }
  return ruta
}
export function semaforoPedido(pedido: PedidoSnap, minutes: number, thresholds = UMBRALES_SEMAFORO): Semaforo {
  const deadline = parseSimTime(pedido.deadline)
  const window = deadline - parseSimTime(pedido.registradoEn)
  const remaining = window > 0 ? (deadline - minutes) / window * PORCENTAJE_MAXIMO : 0
  return remaining >= thresholds.verdeMinPct ? 'VERDE' : remaining >= thresholds.ambarMinPct ? 'AMBAR' : 'ROJO'
}

/** Fuente local de demostración. No representa la velocidad física de la flota. */
export class MockSource implements SnapshotSource {
  private timer: ReturnType<typeof setInterval> | null = null
  private ticks = 0
  private startedAt = 0
  private minutes = 0
  private nextOrder = 0
  private costo = 0
  private lastMovement = 0
  private lastArrival = 0
  private deliveredHistory = 0
  private lateHistory = 0
  private snapshot!: Snapshot
  private readonly escenario: Snapshot['escenario']
  private readonly options: Pick<ParametrosCorrida, 'fechaInicio' | 'umbralesSemaforo'> | undefined

  constructor(escenario: Snapshot['escenario'], options?: Pick<ParametrosCorrida, 'fechaInicio' | 'umbralesSemaforo'>) {
    this.escenario = escenario
    this.options = options
  }

  start(onSnapshot: (snapshot: Snapshot) => void): void {
    this.stop()
    this.reset()
    onSnapshot(structuredClone(this.snapshot))
    this.timer = setInterval(() => {
      this.advance()
      onSnapshot(structuredClone(this.snapshot))
      if (this.snapshot.estado !== 'EN_CURSO') this.stop()
    }, MOCK_CONFIG.tickMs)
  }

  stop(): void { if (this.timer !== null) clearInterval(this.timer); this.timer = null }

  private reset(): void {
    this.ticks = 0; this.minutes = this.escenario === 'DIARIO' ? 0 : MOCK_CONFIG.inicioSimuladoMinutos; this.nextOrder = 0; this.costo = 0
    this.lastMovement = 0; this.lastArrival = 0; this.deliveredHistory = 0; this.lateHistory = 0
    this.startedAt = Date.now()
    const vehiculos: VehiculoSnap[] = FLOTA.flatMap((config) => Array.from({ length: config.cantidad }, (_, index) => {
      const base = ALMACENES[index % ALMACENES.length]
      // Separación inicial para que las 37 unidades puedan inspeccionarse.
      const group = FLOTA.indexOf(config)
      const x = Math.min(GRID_ANCHO, Math.max(0, base.x + (index % 5 - 2) * 3 + group))
      const y = Math.min(GRID_ALTO, Math.max(0, base.y + (Math.floor(index / 5) - 1) * 4 + group * 2))
      return { id: `${config.id === 'auto' ? 'A' : config.id === 'moto' ? 'M' : 'B'}-${String(index + 1).padStart(2, '0')}`, tipo: config.id.toUpperCase() as TipoVehiculo, x, y, estado: 'LIBRE', almacenBase: base.id, carga: 0, capacidad: config.capacidad, ruta: [[x, y]], proximaParada: null, semaforo: 'VERDE' }
    }))
    this.snapshot = {
      runId: `mock-${this.escenario.toLowerCase()}-${this.startedAt}`, escenario: this.escenario, estado: 'EN_CURSO',
      simTime: formatSimTime(0), simTimeIso: localIso(new Date(this.startedAt)), vehiculos, pedidos: [],
      almacenes: ALMACENES.map((almacen) => ({ id: almacen.id, x: almacen.x, y: almacen.y, stock: Number.isFinite(almacen.capacidad) ? MOCK_CONFIG.stockIntermedioInicial : null })),
      metricas: { pedidosTotal: 0, entregados: 0, enRuta: 0, pendientes: 0, atrasados: 0 },
    }
    const count = this.escenario === 'DIARIO' ? MOCK_CONFIG.pedidosDiariosIniciales : MOCK_CONFIG.pedidosIniciales
    for (let i = 0; i < count; i++) {
      const pedido = this.createOrder()
      if (this.escenario !== 'DIARIO' && i < MOCK_CONFIG.pedidosEntregadosIniciales) pedido.estado = 'ENTREGADO'
      this.snapshot.pedidos.push(pedido)
    }
    this.assignOrders(true)
    this.updateMetrics()
  }

  private createOrder(): PedidoSnap {
    const n = ++this.nextOrder
    const horizon = HORIZONTES_VALIDOS[(n - 1) % HORIZONTES_VALIDOS.length] * 60
    // Registro anterior dentro de la ventana SLA: colores variados desde el inicio.
    const age = this.escenario === 'DIARIO' ? 0 : Math.min(this.minutes, horizon * MOCK_CONFIG.fraccionesAntiguedad[n % MOCK_CONFIG.fraccionesAntiguedad.length])
    return {
      id: `ORD-${String(n).padStart(3, '0')}`, clienteId: `c${9100 + n}`,
      x: (n * 13 + 5) % (GRID_ANCHO + 1), y: (n * 7 + 9) % (GRID_ALTO + 1),
      cantidad: MOCK_CONFIG.cantidadMin + n % (MOCK_CONFIG.cantidadMax - MOCK_CONFIG.cantidadMin + 1),
      registradoEn: formatSimTime(this.minutes - age), deadline: formatSimTime(this.minutes + horizon - age),
      estado: 'PENDIENTE', vehiculoId: null, semaforo: 'VERDE',
    }
  }

  private assignOrders(initial = false): void {
    const pending = this.snapshot.pedidos.filter((pedido) => pedido.estado === 'PENDIENTE')
    for (const vehicle of this.snapshot.vehiculos) {
      if (vehicle.estado !== 'LIBRE') continue
      const pedido = pending.shift()
      if (!pedido) break
      const stock = this.snapshot.almacenes.find((a) => a.id === vehicle.almacenBase)!
      if (stock.stock !== null && stock.stock < pedido.cantidad) continue
      if (stock.stock !== null) stock.stock -= pedido.cantidad
      pedido.vehiculoId = vehicle.id
      pedido.estado = initial && pending.length < MOCK_CONFIG.pedidosPlanificadosIniciales ? 'PLANIFICADO' : 'EN_RUTA'
      vehicle.estado = pedido.estado === 'PLANIFICADO' ? 'LIBRE' : 'EN_RUTA'
      vehicle.carga = pedido.cantidad
      vehicle.ruta = rutaOrtogonal(vehicle.x, vehicle.y, pedido.x, pedido.y)
      vehicle.proximaParada = { pedidoId: pedido.id, x: pedido.x, y: pedido.y, eta: this.eta(vehicle.ruta.length - 1) }
    }
  }

  private eta(cells: number): string {
    const minutesPerCell = this.escenario === 'DIARIO' ? MOCK_CONFIG.movimientoMs / 60000 : MOCK_CONFIG.movimientoMs / MOCK_CONFIG.tickMs * MOCK_CONFIG.minutosPorTick
    return formatSimTime(this.minutes + cells * minutesPerCell)
  }

  private advance(): void {
    this.ticks++
    const elapsed = Date.now() - this.startedAt
    this.minutes = this.escenario === 'DIARIO' ? Math.floor(elapsed / 1000) / 60 : MOCK_CONFIG.inicioSimuladoMinutos + this.ticks * MOCK_CONFIG.minutosPorTick
    const movement = Math.floor(elapsed / MOCK_CONFIG.movimientoMs)
    const shouldMove = movement > this.lastMovement
    if (shouldMove) this.lastMovement = movement
    for (const vehicle of this.snapshot.vehiculos) {
      const pedido = this.snapshot.pedidos.find((p) => p.id === vehicle.proximaParada?.pedidoId)
      if (vehicle.estado === 'ATENDIENDO') {
        if (pedido) pedido.estado = 'ENTREGADO'
        vehicle.carga = 0; vehicle.proximaParada = null; vehicle.estado = 'REGRESANDO'
        const base = ALMACENES.find((a) => a.id === vehicle.almacenBase)!
        vehicle.ruta = rutaOrtogonal(vehicle.x, vehicle.y, base.x, base.y)
      } else if (pedido?.estado === 'PLANIFICADO') {
        pedido.estado = 'EN_RUTA'; vehicle.estado = 'EN_RUTA'
      } else if (shouldMove && (vehicle.estado === 'EN_RUTA' || vehicle.estado === 'REGRESANDO')) {
        if (vehicle.ruta.length > 1) {
          vehicle.ruta.shift()
          ;[vehicle.x, vehicle.y] = vehicle.ruta[0]
          this.costo += FLOTA.find((f) => f.id.toUpperCase() === vehicle.tipo)!.costoKm
        }
        if (vehicle.ruta.length <= 1) vehicle.estado = vehicle.estado === 'EN_RUTA' ? 'ATENDIENDO' : 'LIBRE'
      }
      if (vehicle.proximaParada) vehicle.proximaParada.eta = this.eta(vehicle.ruta.length - 1)
    }
    if (this.escenario === 'DIARIO') {
      const arrival = Math.floor(elapsed / (MOCK_CONFIG.llegadaDiariaSegundos * 1000))
      if (arrival > this.lastArrival) { this.snapshot.pedidos.push(this.createOrder()); this.lastArrival = arrival }
      // Mantener acotado el historial de una demo que puede permanecer abierta indefinidamente.
      while (this.snapshot.pedidos.length > MOCK_CONFIG.maxHistorialDiario) {
        const index = this.snapshot.pedidos.findIndex((p) => p.estado === 'ENTREGADO')
        if (index < 0) break
        const [removed] = this.snapshot.pedidos.splice(index, 1)
        this.deliveredHistory++
        if (removed.semaforo === 'ROJO') this.lateHistory++
      }
    }
    if (this.escenario === 'COLAPSO' && this.ticks % MOCK_CONFIG.llegadasColapsoTicks === 0) {
      for (let i = 0; i < MOCK_CONFIG.pedidosPorLlegadaColapso; i++) this.snapshot.pedidos.push(this.createOrder())
    }
    this.assignOrders()
    this.updateMetrics()
    if (this.escenario === '5D' && this.minutes >= MOCK_CONFIG.inicioSimuladoMinutos + HORIZONTE_5D.horas * 60) this.finish('FINALIZADO')
    if (this.escenario === 'COLAPSO' && this.ticks >= MOCK_CONFIG.colapsoMinTicks && this.snapshot.metricas.atrasados / this.snapshot.metricas.pedidosTotal >= MOCK_CONFIG.colapsoFraccionAtrasados) {
      this.snapshot.causa = `Saturación logística: ${this.snapshot.metricas.atrasados} pedidos fuera de plazo.`
      this.finish('COLAPSADO')
    }
  }

  private updateMetrics(): void {
    const thresholds = this.options?.umbralesSemaforo ?? UMBRALES_SEMAFORO
    for (const pedido of this.snapshot.pedidos) if (pedido.estado !== 'ENTREGADO') pedido.semaforo = semaforoPedido(pedido, this.minutes, thresholds)
    for (const vehicle of this.snapshot.vehiculos) {
      const pedido = this.snapshot.pedidos.find((p) => p.id === vehicle.proximaParada?.pedidoId)
      vehicle.semaforo = pedido?.semaforo ?? 'VERDE'
    }
    const pedidos = this.snapshot.pedidos
    this.snapshot.simTime = formatSimTime(this.minutes)
    const start = this.escenario === 'DIARIO' ? this.startedAt : this.options?.fechaInicio ? new Date(this.options.fechaInicio).getTime() : this.startedAt
    const elapsedMinutes = this.escenario === 'DIARIO' ? this.minutes : this.minutes - MOCK_CONFIG.inicioSimuladoMinutos
    this.snapshot.simTimeIso = localIso(new Date(start + elapsedMinutes * 60000))
    this.snapshot.metricas = {
      pedidosTotal: pedidos.length + this.deliveredHistory,
      entregados: pedidos.filter((p) => p.estado === 'ENTREGADO').length + this.deliveredHistory,
      enRuta: pedidos.filter((p) => p.estado === 'EN_RUTA').length,
      pendientes: pedidos.filter((p) => p.estado === 'PENDIENTE' || p.estado === 'PLANIFICADO').length,
      atrasados: pedidos.filter((p) => p.estado !== 'ENTREGADO' && parseSimTime(p.deadline) < this.minutes).length,
    }
  }

  private finish(estado: 'FINALIZADO' | 'COLAPSADO'): void {
    this.snapshot.estado = estado
    this.snapshot.resumen = {
      pedidosTotal: this.snapshot.metricas.pedidosTotal, entregados: this.snapshot.metricas.entregados,
      atrasados: this.snapshot.metricas.atrasados + this.lateHistory,
      noAsignados: this.snapshot.pedidos.filter((p) => p.vehiculoId === null && p.estado !== 'ENTREGADO').length,
      costoTotal: this.costo,
    }
  }
}
