import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ALGORITMOS, ALMACENES, FLOTA, GRID_ALTO, GRID_ANCHO, HORIZONTE_5D, MODOS, PORCENTAJE_MAXIMO } from '../config/defaults'
import { useSimulation } from '../context/SimulationContext'
import { Card } from '../components/Card'
import { Icon } from '../components/Icon'
import { OrdersUpload } from '../components/OrdersUpload'
import type { ParametrosCorrida } from '../types'

export function ParametersPage() {
  const { parametros, setParametros, archivoPedidos, infoArchivoPedidos } = useSimulation()
  const [draft, setDraft] = useState<ParametrosCorrida>(() => structuredClone(parametros))
  const navigate = useNavigate()
  const { verdeMinPct, ambarMinPct } = draft.umbralesSemaforo
  const total = Object.values(draft.flota).reduce((sum, count) => sum + count, 0)
  const thresholdsValid = Number.isFinite(verdeMinPct) && Number.isFinite(ambarMinPct) && ambarMinPct >= 0 && verdeMinPct <= PORCENTAJE_MAXIMO && ambarMinPct < verdeMinPct
  const fleetValid = Object.values(draft.flota).every((n) => Number.isSafeInteger(n) && n >= 0) && total > 0
  const canStart = !!archivoPedidos && !!infoArchivoPedidos?.valido && thresholdsValid && fleetValid && !!draft.fechaInicio
  function threshold(key: 'verdeMinPct' | 'ambarMinPct', value: number) {
    setDraft((prev) => ({ ...prev, umbralesSemaforo: { ...prev.umbralesSemaforo, [key]: value } }))
  }
  function start(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!canStart) return
    setParametros(draft)
    navigate('/simulacion/mapa')
  }
  return <form onSubmit={start} className="parameters">
    <div className="top-grid"><OrdersUpload />
      <Card title="Configuración de la Corrida" subtitle="Parámetros técnicos y asignación" icon="settings" tone="orange">
        <fieldset><legend className="field-label">MODO DE SIMULACIÓN</legend><div className="mode-options">{MODOS.map((mode) => <label key={mode.id} className={`mode-option ${draft.escenario === mode.id ? 'selected' : ''}`}><div><strong>{mode.nombre}</strong><input type="radio" name="escenario" value={mode.id} checked={draft.escenario === mode.id} onChange={() => setDraft({ ...draft, escenario: mode.id })} /></div><p>{mode.descripcion}</p></label>)}</div></fieldset>

        <label className="field-label mt-3" htmlFor="algoritmo">ALGORITMO</label><select id="algoritmo" value={draft.algoritmo} onChange={(e) => setDraft({ ...draft, algoritmo: e.target.value as ParametrosCorrida['algoritmo'] })}>{ALGORITMOS.map((algoritmo) => <option key={algoritmo}>{algoritmo}</option>)}</select>
        <label className="field-label mt-3" htmlFor="fecha">FECHA Y HORA DE INICIO</label><input id="fecha" type="datetime-local" required value={draft.fechaInicio} onChange={(e) => setDraft({ ...draft, fechaInicio: e.target.value })} />
        <div className="horizon"><span>HORIZONTE ESTIMADO</span><strong>{draft.escenario === '5D' ? `${HORIZONTE_5D.dias} días simulados (${HORIZONTE_5D.horas} h)` : 'Hasta el colapso'}</strong></div>
      </Card>
    </div>
    <div className="bottom-grid">
      <Card title="Umbrales de Semaforización" subtitle="Configuración editable de holgura SLA (%)" icon="light" tone="green">
        <div className="threshold green"><span className="status-dot" /><div><strong>Verde — A tiempo</strong><small>Holgura holgada</small></div><label>≥ <input aria-label="Umbral mínimo verde" type="number" min={0} max={PORCENTAJE_MAXIMO} required value={Number.isNaN(verdeMinPct) ? '' : verdeMinPct} onChange={(e) => threshold('verdeMinPct', e.target.valueAsNumber)} /> %</label></div>
        <div className="threshold amber"><span className="status-dot" /><div><strong>Ámbar — En riesgo</strong><small>Atención preventiva</small></div><label><input aria-label="Umbral mínimo ámbar" type="number" min={0} max={PORCENTAJE_MAXIMO} required value={Number.isNaN(ambarMinPct) ? '' : ambarMinPct} onChange={(e) => threshold('ambarMinPct', e.target.valueAsNumber)} /> – &lt; {Number.isNaN(verdeMinPct) ? '—' : verdeMinPct} %</label></div>
        <div className="threshold red"><span className="status-dot" /><div><strong>Rojo — Vencido/Crítico</strong><small>Plazo superado</small></div><label>&lt; <input aria-label="Límite superior rojo" type="number" min={0} max={PORCENTAJE_MAXIMO} required value={Number.isNaN(ambarMinPct) ? '' : ambarMinPct} onChange={(e) => threshold('ambarMinPct', e.target.valueAsNumber)} /> %</label></div>
        {!thresholdsValid && <p className="validation-error">Usa porcentajes entre 0 y {PORCENTAJE_MAXIMO}, con ámbar menor que verde.</p>}
      </Card>
      <Card title="Flota Disponible" subtitle="Parámetros técnicos y capacidades" icon="truck" tone="purple" badge={`${FLOTA.length} Tipos`}>
        <div className="table-scroll"><table className="fleet-table"><thead><tr><th>TIPO</th><th>CANT.</th><th>CAPACIDAD</th><th>COSTO/KM</th><th>VEL.</th></tr></thead><tbody>{FLOTA.map((vehicle) => <tr key={vehicle.id}><td><span className="vehicle-dot" style={{ background: vehicle.color }} />{vehicle.nombre}</td><td><input type="number" aria-label={`Cantidad de ${vehicle.nombre}`} min={0} step={1} required value={Number.isNaN(draft.flota[vehicle.id]) ? '' : draft.flota[vehicle.id]} onChange={(event) => setDraft({ ...draft, flota: { ...draft.flota, [vehicle.id]: event.target.valueAsNumber } })} /></td><td>{vehicle.capacidad} paq.</td><td>S/ {vehicle.costoKm.toFixed(2)}</td><td>{vehicle.velocidad} km/h</td></tr>)}</tbody></table></div>
        <p className="note">ⓘ El tamaño de la flota solo puede modificarse al inicio de la corrida.</p><p className="fleet-total"><span>Flota total configurada</span><strong>{Number.isNaN(total) ? '—' : total} unidades</strong></p>
        {!fleetValid && <p className="validation-error">Ingresa cantidades enteras no negativas y al menos una unidad.</p>}
      </Card>
      <Card title="Red de Almacenes" subtitle="Centros logísticos (solo lectura)" icon="warehouse" tone="teal" badge={`${ALMACENES.length} Nodos`}>
        <div className="table-scroll"><table className="warehouse-table"><thead><tr><th>ALMACÉN (X, Y)</th><th>CAPACIDAD</th><th>RECARGA</th></tr></thead><tbody>{ALMACENES.map((warehouse) => <tr key={warehouse.id}><td><strong>{warehouse.nombre}</strong><span className="coordinates">({warehouse.x}, {warehouse.y})</span></td><td>{Number.isFinite(warehouse.capacidad) ? warehouse.capacidad.toLocaleString('es-PE') : '∞'}</td><td>{warehouse.recarga}</td></tr>)}</tbody></table></div><p className="fleet-total"><span>Cuadrícula táctica</span><strong>Red {GRID_ANCHO} × {GRID_ALTO} km</strong></p>
      </Card>
    </div>
    <div className="start-panel"><button type="submit" className="start-button" disabled={!canStart}><Icon name="play" />Iniciar Corrida<Icon name="arrow" /></button><p className={canStart ? 'ready' : ''}>{canStart ? '✓ Con el archivo validado, puedes iniciar la corrida.' : !infoArchivoPedidos?.valido ? 'Carga un archivo de pedidos válido para iniciar la corrida.' : 'Revisa la configuración para iniciar la corrida.'}</p></div>
  </form>
}
