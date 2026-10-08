import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ALGORITMOS, ALMACENES, FLOTA, GRID_ALTO, GRID_ANCHO, HORIZONTE_5D, MODOS, PORCENTAJE_MAXIMO } from '../config/defaults'
import { useSimulation } from '../context/SimulationContext'
import { Card } from '../components/Card'
import { Icon } from '../components/Icon'
import { OrdersUpload } from '../components/OrdersUpload'
import { iniciarEscenario, subirArchivoPedidos, ApiError } from '../services/api'
import type { EscenarioActivoInfo, ParametrosCorrida } from '../types'

export function ParametersPage() {
  const { parametros, setParametros, archivoPedidos, infoArchivoPedidos, runIds, setRunId } = useSimulation()
  const [draft, setDraft] = useState<ParametrosCorrida>(() => structuredClone(parametros))
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  const [conflictRun, setConflictRun] = useState<EscenarioActivoInfo | null>(null)

  const { verdeMinPct, ambarMinPct } = draft.umbralesSemaforo
  const total = Object.values(draft.flota).reduce((sum, count) => sum + count, 0)
  const thresholdsValid = Number.isFinite(verdeMinPct) && Number.isFinite(ambarMinPct) && ambarMinPct >= 0 && verdeMinPct <= PORCENTAJE_MAXIMO && ambarMinPct < verdeMinPct
  const fleetValid = Object.values(draft.flota).every((n) => Number.isSafeInteger(n) && n >= 0) && total > 0
  const canStart = (draft.escenario === 'DIARIO' || !!archivoPedidos) && (draft.escenario === 'DIARIO' || !!infoArchivoPedidos?.valido) && thresholdsValid && fleetValid && !!draft.fechaInicio

  function threshold(key: 'verdeMinPct' | 'ambarMinPct', value: number) {
    setDraft((prev) => ({ ...prev, umbralesSemaforo: { ...prev.umbralesSemaforo, [key]: value } }))
  }

  async function start(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!canStart || loading) return
    setLoading(true)
    setErrorMsg(null)
    setConflictRun(null)

    try {
      let archivoPedidosId: string | undefined = undefined
      if (draft.escenario !== 'DIARIO' && archivoPedidos) {
        const subida = await subirArchivoPedidos(archivoPedidos)
        if (subida.errores && subida.errores.length > 0) {
          setErrorMsg(`El archivo contiene errores reportados por el backend en las líneas: ${subida.errores.map(e => e.linea).join(', ')}`)
          setLoading(false)
          return
        }
        archivoPedidosId = subida.archivoId
      }

      const res = await iniciarEscenario(draft.escenario, {
        ...draft,
        archivoPedidosId,
      })

      setRunId(draft.escenario, res.runId)
      setParametros(draft)
      navigate('/simulacion/mapa')
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setConflictRun({
          runId: runIds[draft.escenario] || 'active-run',
          escenario: draft.escenario,
          estado: 'EN_CURSO',
          topic: '',
        })
        setErrorMsg(`Ya hay una simulación ${draft.escenario} en curso.`)
      } else {
        setErrorMsg(err instanceof Error ? err.message : 'Error al iniciar corrida.')
      }
    } finally {
      setLoading(false)
    }
  }

  return <form onSubmit={start} className="parameters">
    {errorMsg && <div className="run-alert mb-4 p-3 bg-red-100 text-red-800 rounded flex items-center justify-between">
      <span>{errorMsg}</span>
      {conflictRun && <button type="button" className="secondary-button ml-4" onClick={() => {
        setRunId(conflictRun.escenario, conflictRun.runId)
        navigate('/simulacion/mapa')
      }}>Ver corrida activa</button>}
    </div>}

    <div className="top-grid"><OrdersUpload />
      <Card title="Configuración de la Corrida" subtitle="Parámetros técnicos y asignación" icon="settings" tone="orange">
        <fieldset><legend className="field-label">MODO DE SIMULACIÓN</legend><div className="mode-options">{MODOS.map((mode) => <label key={mode.id} className={`mode-option ${draft.escenario === mode.id ? 'selected' : ''}`}><div><strong>{mode.nombre}</strong><input type="radio" name="escenario" value={mode.id} checked={draft.escenario === mode.id} onChange={() => setDraft({ ...draft, escenario: mode.id as any })} /></div><p>{mode.descripcion}</p></label>)}</div></fieldset>

        <label className="field-label mt-3" htmlFor="algoritmo">ALGORITMO</label><select id="algoritmo" value={draft.algoritmo} onChange={(e) => setDraft({ ...draft, algoritmo: e.target.value as ParametrosCorrida['algoritmo'] })}>{ALGORITMOS.map((algoritmo) => <option key={algoritmo}>{algoritmo}</option>)}</select>
        <label className="field-label mt-3" htmlFor="fecha">FECHA Y HORA DE INICIO</label><input id="fecha" type="datetime-local" required value={draft.fechaInicio} onChange={(e) => setDraft({ ...draft, fechaInicio: e.target.value })} />
        <div className="horizon"><span>HORIZONTE ESTIMADO</span><strong>{draft.escenario === '5D' ? `${HORIZONTE_5D.dias} días simulados (${HORIZONTE_5D.horas} h)` : draft.escenario === 'COLAPSO' ? 'Hasta el colapso' : 'Continuo'}</strong></div>
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
    <div className="start-panel"><button type="submit" className="start-button" disabled={!canStart || loading}><Icon name="play" />{loading ? 'Iniciando...' : 'Iniciar Corrida'}<Icon name="arrow" /></button><p className={canStart ? 'ready' : ''}>{canStart ? '✓ Con la configuración validada, puedes iniciar la corrida.' : 'Revisa la configuración y el archivo de pedidos para iniciar la corrida.'}</p></div>
  </form>
}
