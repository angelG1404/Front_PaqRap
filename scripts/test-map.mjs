import assert from 'node:assert/strict'
import { moduleUrl } from './load-ts-module.mjs'
const { MockSource, rutaOrtogonal, semaforoPedido } = await import(moduleUrl('src/mock/MockSource.ts'))
const { pointOnPath, movementPath, project, unproject } = await import(moduleUrl('src/components/mapa/geometry.ts'))
const { MOCK_CONFIG, HORIZONTE_5D, FLOTA, GRID_ANCHO, GRID_ALTO } = await import(moduleUrl('src/config/defaults.ts'))
const { parseSimTime } = await import(moduleUrl('src/services/simulationTime.ts'))

const route = rutaOrtogonal(1, 1, 4, 3)
assert.deepEqual(route, [[1, 1], [2, 1], [3, 1], [4, 1], [4, 2], [4, 3]])
assert.deepEqual(pointOnPath([[0, 0], [2, 0], [2, 2]], 0.75), [2, 1])
assert.deepEqual(pointOnPath([[0, 0], [2, 2]], 0.25), [1, 0])
assert.deepEqual(movementPath([2, 0.5], [2, 2], [[0, 0], [2, 0], [2, 2]]), [[2, 0.5], [2, 2]])
const viewport = { width: 800, height: 600, scale: 8, offsetX: 30, offsetY: 550 }
assert.deepEqual(unproject(project([27, 14], viewport), viewport), [27, 14])
assert.ok(project([0, 1], viewport)[1] < project([0, 0], viewport)[1])
const order = { registradoEn: '01d00h00m', deadline: '01d10h00m' }
assert.equal(semaforoPedido(order, 240), 'VERDE')
assert.equal(semaforoPedido(order, 241), 'AMBAR')
assert.equal(semaforoPedido(order, 420), 'AMBAR')
assert.equal(semaforoPedido(order, 421), 'ROJO')

const originals = { now: Date.now, interval: globalThis.setInterval, clear: globalThis.clearInterval }
let now = new Date('2026-10-08T14:32:15').getTime()
let callback = null
let intervals = 0
Date.now = () => now
globalThis.setInterval = (fn) => { callback = fn; intervals++; return intervals }
globalThis.clearInterval = () => { callback = null }

try {
  for (const scenario of ['5D', 'COLAPSO', 'DIARIO']) {
    const source = new MockSource(scenario)
    let latest
    let previous
    let emissions = 0
    let movementCount = 0
    const states = new Set()
    const totalFleet = FLOTA.reduce((total, item) => total + item.cantidad, 0)
    source.start((snapshot) => {
      latest = snapshot
      emissions++
      assert.equal(snapshot.escenario, scenario)
      assert.equal(snapshot.vehiculos.length, totalFleet)
      assert.equal(new Set(snapshot.vehiculos.map(v => v.id)).size, totalFleet)
      const m = snapshot.metricas
      assert.equal(m.entregados + m.enRuta + m.pendientes, m.pedidosTotal)
      assert.equal(m.enRuta, snapshot.pedidos.filter(p => p.estado === 'EN_RUTA').length)
      assert.equal(m.atrasados, snapshot.pedidos.filter(p => p.estado !== 'ENTREGADO' && parseSimTime(p.deadline) < parseSimTime(snapshot.simTime)).length)
      for (const vehicle of snapshot.vehiculos) {
        states.add(vehicle.estado)
        assert.ok(vehicle.x >= 0 && vehicle.x <= GRID_ANCHO && vehicle.y >= 0 && vehicle.y <= GRID_ALTO)
        assert.deepEqual(vehicle.ruta[0], [vehicle.x, vehicle.y])
        assert.ok(vehicle.carga >= 0 && vehicle.carga <= vehicle.capacidad)
        for (let i = 1; i < vehicle.ruta.length; i++) {
          const a = vehicle.ruta[i - 1], b = vehicle.ruta[i]
          assert.equal(Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]), 1)
        }
        if (vehicle.proximaParada) assert.deepEqual(vehicle.ruta.at(-1), [vehicle.proximaParada.x, vehicle.proximaParada.y])
        if (previous) {
          const old = previous.vehiculos.find(v => v.id === vehicle.id)
          const distance = Math.abs(old.x - vehicle.x) + Math.abs(old.y - vehicle.y)
          assert.ok(distance <= 1, `${scenario}: salto de vehículo`)
          if (distance) { assert.equal((emissions - 1) % (MOCK_CONFIG.movimientoMs / MOCK_CONFIG.tickMs), 0); movementCount++ }
        }
      }
      if (scenario === 'DIARIO') {
        assert.equal(snapshot.estado, 'EN_CURSO')
        assert.equal(snapshot.resumen, undefined)
        if (previous) assert.equal(new Date(snapshot.simTimeIso).getTime() - new Date(previous.simTimeIso).getTime(), MOCK_CONFIG.tickMs)
      }
      previous = snapshot
    })
    assert.equal(latest.pedidos.length, scenario === 'DIARIO' ? MOCK_CONFIG.pedidosDiariosIniciales : MOCK_CONFIG.pedidosIniciales)
    const initial = structuredClone(latest)
    const first = latest
    const maxTicks = scenario === 'DIARIO' ? 600 : 500
    for (let i = 0; i < maxTicks && callback; i++) { now += MOCK_CONFIG.tickMs; callback() }
    assert.deepEqual(first, initial, 'Los snapshots emitidos no se deben mutar')
    assert.ok(movementCount > 0)
    assert.ok(states.has('ATENDIENDO') && states.has('REGRESANDO') && states.has('LIBRE'))
    if (scenario === '5D') {
      assert.equal(latest.estado, 'FINALIZADO')
      assert.equal(emissions - 1, HORIZONTE_5D.horas * 60 / MOCK_CONFIG.minutosPorTick)
      assert.equal(callback, null)
    } else if (scenario === 'COLAPSO') {
      assert.equal(latest.estado, 'COLAPSADO')
      assert.ok(latest.causa)
      assert.equal(callback, null)
    } else assert.equal(emissions, maxTicks + 1)
    source.stop()
    assert.equal(callback, null)
    source.start(() => {})
    source.start(() => {})
    source.stop()
    assert.equal(callback, null)
    console.log(`${scenario}: ${emissions} snapshots, ${movementCount} pasos ortogonales, métricas/estados/cierre correctos.`)
  }
} finally { Date.now = originals.now; globalThis.setInterval = originals.interval; globalThis.clearInterval = originals.clear }
console.log('OK: geometría, interpolación, semáforos, flota, cadencia, aislamiento de snapshots y ciclo de vida.')
