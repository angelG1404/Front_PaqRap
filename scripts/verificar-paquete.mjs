import { readFile } from 'node:fs/promises'
import assert from 'node:assert/strict'

const base = process.argv[2] || 'http://127.0.0.1:18081'
const paths = process.argv.includes('--backend-only')
  ? ['/api/mapa/base']
  : ['/', '/operacion-diaria', '/simulacion/parametros', '/simulacion/mapa', '/api/mapa/base']
for (const path of paths) {
  const response = await fetch(base + path)
  assert.equal(response.status, 200, path)
  console.log('OK', path)
}
for (const [name, type] of [['ventas.txt', 'pedidos'], ['ventas_small.txt', 'pedidos'], ['pedidos_colapso.txt', 'pedidos'], ['bloqueo.txt', 'bloqueos'], ['bloqueo_small.txt', 'bloqueos']]) {
  const form = new FormData()
  form.append('file', new Blob([await readFile(new URL('../data/' + name, import.meta.url))]), name)
  const response = await fetch(base + '/api/archivos/' + type, { method: 'POST', body: form })
  assert.equal(response.status, 200)
  const result = await response.json()
  assert.deepEqual(result.errores, [], name)
  assert.ok(result.totalPedidos > 0, name)
  console.log('OK archivo', name, result.totalPedidos)
}
const active = await (await fetch(base + '/api/escenarios/activos')).json()
const daily = active.find(item => item.escenario === 'DIARIO')
assert.ok(daily, 'Operación diaria iniciada')
const order = await fetch(base + '/api/pedidos', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ x: 34, y: 15, clienteId: 'demo-release', cantidad: 4, horizonteHoras: 12 }) })
assert.equal(order.status, 201)
console.log('OK registro diario')
await new Promise((resolve, reject) => {
  const ws = new WebSocket(base.replace(/^http/, 'ws') + '/ws')
  const timer = setTimeout(() => { ws.close(); reject(new Error('Sin snapshot WebSocket en 15 segundos')) }, 15000)
  ws.onopen = () => ws.send('CONNECT\naccept-version:1.2\nhost:localhost\n\n\0')
  ws.onerror = () => { clearTimeout(timer); reject(new Error('Error WebSocket')) }
  ws.onmessage = async event => {
    const frame = typeof event.data === 'string' ? event.data : await event.data.text()
    if (frame.startsWith('CONNECTED')) ws.send(`SUBSCRIBE\nid:smoke\ndestination:/topic/sim/${daily.runId}\nack:auto\n\n\0`)
    if (frame.startsWith('MESSAGE')) {
      clearTimeout(timer)
      ws.close()
      console.log('OK snapshot WebSocket')
      resolve()
    }
  }
})
