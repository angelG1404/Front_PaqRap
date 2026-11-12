import assert from 'node:assert/strict'
import { createServer } from 'vite'
import { createElement } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'

const server = await createServer({ server: { middlewareMode: true }, appType: 'custom' })
try {
  const { Ocupacion } = await server.ssrLoadModule('/src/components/mapa/Ocupacion.tsx')
  const { AlmacenesPanel } = await server.ssrLoadModule('/src/components/mapa/AlmacenesPanel.tsx')
  const { estadoCarga } = await server.ssrLoadModule('/src/config/theme.ts')
  const { compararOcupacion } = await server.ssrLoadModule('/src/services/ocupacion.ts')
  const unidades = [{ id: 'AUTO', carga: 12, capacidad: 24 }, { id: 'MOTO', carga: 6, capacidad: 8 }, { id: 'BICI', carga: 0, capacidad: 4 }]
  assert.deepEqual([...unidades].sort((a, b) => compararOcupacion(a, b, 'desc')).map((v) => v.id), ['MOTO', 'AUTO', 'BICI'])
  assert.deepEqual([...unidades].sort((a, b) => compararOcupacion(a, b, 'asc')).map((v) => v.id), ['BICI', 'AUTO', 'MOTO'])
  const snapshot = {
    vehiculos: [{ carga: 12, capacidad: 24 }, { carga: 4, capacidad: 8 }, { carga: 0, capacidad: 4 }],
    almacenes: [
      { id: 'CENTRAL', stock: null, capacidad: null },
      { id: 'INTERMEDIO_NOROESTE', stock: 800, capacidad: 1000 },
      { id: 'INTERMEDIO_ESTE', stock: 200, capacidad: 1000 },
    ],
    pedidos: [
      { id: 'TRANSITO', estado: 'EN_RUTA', almacenDespacho: 'INTERMEDIO_ESTE', cantidad: 7, vehiculoId: 'AUTO-1', clienteId: 'c1', x: 3, y: 4 },
      { id: 'FINALIZADO', estado: 'ENTREGADO', almacenDespacho: 'INTERMEDIO_ESTE', cantidad: 10 },
      { id: 'FUTURO', estado: 'PLANIFICADO', almacenDespacho: 'INTERMEDIO_ESTE', cantidad: 10 },
      { id: 'OTRO', estado: 'EN_RUTA', almacenDespacho: 'CENTRAL', cantidad: 10 },
    ],
  }
  const render = (Component, props) => renderToStaticMarkup(createElement(Component, props))
  const cards = render(Ocupacion, { snapshot })
  assert.ok(cards.includes((16 / 36 * 100).toLocaleString('es-PE', { maximumFractionDigits: 1 }) + ' %'), 'Ocupación ponderada por capacidad')
  assert.ok(cards.includes('50 %'), 'El central ilimitado no altera la ocupación de intermedios')
  assert.ok(cards.includes('Central: ilimitado'))
  const panel = render(AlmacenesPanel, { snapshot, selectedId: 'INTERMEDIO_ESTE', onSelect() {} })
  assert.ok(panel.includes('1 envíos · 7 paquetes'))
  assert.ok(panel.includes('TRANSITO'))
  for (const excluded of ['FINALIZADO', 'FUTURO', 'OTRO']) assert.ok(!panel.includes(excluded))
  assert.ok(!render(AlmacenesPanel, { snapshot, selectedId: null, onSelect() {} }).includes('TRANSITO'))
  const empty = render(Ocupacion, { snapshot: { vehiculos: [], almacenes: [] } })
  assert.ok(!empty.includes('NaN') && !empty.includes('Infinity'))
  assert.ok(empty.includes('Sin capacidad disponible'))
  assert.ok(render(Ocupacion, { snapshot: null }).includes('Esperando datos'))
  for (const [carga, expected] of [[0, 'VACIO'], [24, 'ROJO'], [25, 'AMBAR'], [50, 'AMBAR'], [51, 'VERDE']]) {
    assert.equal(estadoCarga({ carga, capacidad: 100 }), expected)
  }
  console.log('OK: ocupación ponderada, central ilimitado, envíos en tránsito, ausencia de datos y umbrales.')
} finally {
  await server.close()
}
