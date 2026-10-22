import { useState, type FormEvent } from 'react'
import { NOMBRES_AVERIA, NOMBRES_TIPO } from '../config/theme'
import { registrarAveria, type AveriaRegistrada } from '../services/api'
import { readableSimTime } from '../services/simulationTime'
import type { VehiculoSnap } from '../types/snapshot'
import { Icon } from './Icon'

interface Props { runId: string | null; vehiculos: VehiculoSnap[]; disponible: boolean }

/** Registra una avería en caliente: la unidad queda detenida y sus pedidos se replanifican. */
export function RegistroAveriaForm({ runId, vehiculos, disponible }: Props) {
  const [vehiculoId, setVehiculoId] = useState('')
  const [tipo, setTipo] = useState<1 | 2 | 3>(1)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState('')
  const [confirmacion, setConfirmacion] = useState<AveriaRegistrada | null>(null)
  const operativos = vehiculos.filter((v) => v.estado !== 'AVERIADO')
  const averiados = vehiculos.filter((v) => v.estado === 'AVERIADO')

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!runId || !vehiculoId) { setError('Selecciona la unidad averiada.'); return }
    setEnviando(true); setError(''); setConfirmacion(null)
    try {
      setConfirmacion(await registrarAveria(runId, vehiculoId, tipo))
      setVehiculoId('')
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo registrar la avería.') }
    finally { setEnviando(false) }
  }

  return <>
    <form className="register-order-form breakdown-form" onSubmit={submit} aria-label="Registrar avería">
      <div className="register-order-heading"><Icon name="truck" /><h3>Registrar avería</h3><span className="incident">INCIDENCIA</span></div>
      <fieldset disabled={enviando || !disponible}>
        <label className="register-field">Unidad<select value={vehiculoId} onChange={(e) => setVehiculoId(e.target.value)} required>
          <option value="">Selecciona la unidad…</option>
          {operativos.map((v) => <option key={v.id} value={v.id}>{v.id} — {NOMBRES_TIPO[v.tipo]} · {v.estado === 'LIBRE' ? 'en almacén' : v.estado.toLowerCase().replace('_', ' ')}</option>)}
        </select></label>
        <label className="register-field">Tipo de avería<select value={tipo} onChange={(e) => setTipo(Number(e.target.value) as 1 | 2 | 3)}>{([1, 2, 3] as const).map((t) => <option key={t} value={t}>{NOMBRES_AVERIA[t]}</option>)}</select></label>
        <p className="breakdown-note">La unidad se detiene donde está y sus pedidos pendientes se reasignan de inmediato. En tipo 2 y 3 se remolca al Almacén Central tras 4 h.</p>
        <button className="register-submit danger" type="submit" disabled={!disponible || enviando || !vehiculoId}><Icon name="check" />{enviando ? 'Registrando…' : 'Registrar avería'}</button>
      </fieldset>
      {error && <p className="register-error" role="alert">{error}</p>}
      {confirmacion && <div className="register-success" role="status"><strong>✓ Avería de {confirmacion.vehiculoId} registrada</strong><span>{confirmacion.pedidosReasignados} pedido(s) en reasignación · disponible {readableSimTime(confirmacion.hasta)}</span></div>}
    </form>
    <section className="recent-orders" aria-label="Averías activas"><div><h3>Averías activas</h3><span>Total: {averiados.length}</span></div>
      {averiados.length ? <ol>{averiados.map((v) => <li key={v.id} className="breakdown-item"><div><strong>{v.id}</strong><span>Tipo {v.averia?.tipo}</span></div><p>{v.averia?.remolcado ? 'Remolcada al Almacén Central' : 'Detenida en (' + Math.round(v.x) + ', ' + Math.round(v.y) + ')'} · disponible {v.averia ? readableSimTime(v.averia.hasta) : '—'}</p></li>)}</ol> : <p className="recent-empty">Sin averías activas.</p>}
    </section>
  </>
}
