import assert from 'node:assert/strict'
import { moduleUrl } from './load-ts-module.mjs'

const { dailySource } = await import(moduleUrl('src/mock/dailySource.ts'))
const { registrarPedido } = await import(moduleUrl('src/services/api.ts'))
const { registrarPedido: registrarReal } = await import(moduleUrl('src/services/api.ts', false))
const { continuousDailySource } = await import(moduleUrl('src/services/continuousDailySource.ts'))
const { HORIZONTES_VALIDOS, GRID_ANCHO, GRID_ALTO } = await import(moduleUrl('src/config/defaults.ts'))
const originals = { now: Date.now, interval: globalThis.setInterval, clear: globalThis.clearInterval, fetch: globalThis.fetch }
let now = new Date('2026-10-08T14:32:15').getTime()
let tick
let snapshot
let requests = 0
Date.now = () => now
globalThis.setInterval = (fn) => { tick = fn; return 1 }
globalThis.clearInterval = () => { tick = null }
globalThis.fetch = async () => { requests++; throw new Error('No debe haber red en mock') }
const input = { x: 31, y: 14, clienteId: ' c9103 ', cantidad: 3, horizonteHoras: 4 }

try {
  await assert.rejects(registrarPedido(input), /no está activa/)
  dailySource.start((s) => { snapshot = s })
  const previous = structuredClone(snapshot)
  const registered = await registrarPedido(input)
  assert.equal(registered.clienteId, 'c9103')
  assert.equal(registered.estado, 'PENDIENTE')
  assert.equal(new Date(registered.deadline).getTime(), now + input.horizonteHoras * 3600000)
  assert.equal(snapshot.metricas.pedidosTotal, previous.metricas.pedidosTotal + 1)
  const added = snapshot.pedidos.find((p) => p.id === registered.id)
  assert.equal(added.estado, 'PENDIENTE')
  assert.equal(added.vehiculoId, null)
  assert.equal(added.x, input.x); assert.equal(added.y, input.y)
  assert.equal(snapshot.metricas.pendientes, previous.metricas.pendientes + 1)
  now += 1000; tick()
  const assigned = snapshot.pedidos.find((p) => p.id === registered.id)
  assert.equal(assigned.estado, 'EN_RUTA')
  assert.ok(assigned.vehiculoId)
  assert.ok(snapshot.vehiculos.find((v) => v.id === assigned.vehiculoId).capacidad >= input.cantidad)
  assert.equal(new Date(snapshot.simTimeIso).getTime(), now)
  assert.equal(registered.estado, 'PENDIENTE', 'La respuesta no se debe mutar al asignar')

  const ids = new Set([registered.id])
  for (const horizonteHoras of HORIZONTES_VALIDOS) {
    const result = await registrarPedido({ ...input, horizonteHoras, x: GRID_ANCHO, y: GRID_ALTO })
    assert.equal(new Date(result.deadline).getTime() - now, horizonteHoras * 3600000)
    assert.ok(!ids.has(result.id)); ids.add(result.id)
  }
  const total = snapshot.metricas.pedidosTotal
  for (const invalid of [{ x: -1 }, { x: GRID_ANCHO + 1 }, { y: GRID_ALTO + 1 }, { y: NaN }, { x: 1.5 }, { clienteId: '  ' }, { cantidad: 0 }, { cantidad: 1.5 }, { horizonteHoras: 5 }]) {
    await assert.rejects(registrarPedido({ ...input, ...invalid }))
  }
  assert.equal(snapshot.metricas.pedidosTotal, total)
  const large = await registrarPedido({ ...input, cantidad: 100 })
  now += 1000; tick()
  assert.equal(snapshot.pedidos.find((p) => p.id === large.id).estado, 'PENDIENTE')
  assert.ok(snapshot.vehiculos.every((v) => v.carga <= v.capacidad))
  assert.equal(requests, 0, 'Registrar en mock no usa fetch')
  dailySource.stop()

  let publish
  let stopped = false
  const continuous = continuousDailySource({ start: (callback) => { publish = callback }, stop: () => { stopped = true } })
  let observed
  continuous.start((s) => { observed = s })
  for (const estado of ['FINALIZADO', 'COLAPSADO', 'EN_CURSO']) {
    publish({ ...snapshot, estado, simTimeIso: '2026-10-08T14:33:00', causa: 'Ignorar', resumen: {} })
    assert.equal(observed.estado, 'EN_CURSO')
    assert.equal(observed.simTimeIso, '2026-10-08T14:33:00')
    assert.equal(observed.causa, undefined); assert.equal(observed.resumen, undefined)
  }
  continuous.stop(); assert.ok(stopped)

  globalThis.fetch = async (url, options) => {
    assert.equal(url, 'https://paqrap.example/api/pedidos')
    assert.equal(options.method, 'POST')
    assert.equal(options.headers['Content-Type'], 'application/json')
    assert.deepEqual(JSON.parse(options.body), { ...input, clienteId: 'c9103' })
    return new Response(JSON.stringify(registered), { status: 201 })
  }
  assert.deepEqual(await registrarReal(input), registered)
  globalThis.fetch = async () => new Response(JSON.stringify({ message: 'Cliente no encontrado' }), { status: 400 })
  await assert.rejects(registrarReal(input), /Cliente no encontrado/)
  globalThis.fetch = async () => new Response('Fallo', { status: 500 })
  await assert.rejects(registrarReal(input), /HTTP 500/)
  globalThis.fetch = async () => { throw new Error('offline') }
  await assert.rejects(registrarReal(input), /conectar con el servidor/)
  console.log('OK: registro mock sin red, publicación inmediata, asignación, capacidad, validación, deadlines ISO, IDs únicos y reloj real.')
  console.log('OK: Diario ignora estados finales; contrato HTTP 201, errores del servidor y fallo de red (fetch simulado).')
} finally {
  dailySource.stop()
  Date.now = originals.now; globalThis.setInterval = originals.interval; globalThis.clearInterval = originals.clear; globalThis.fetch = originals.fetch
}
