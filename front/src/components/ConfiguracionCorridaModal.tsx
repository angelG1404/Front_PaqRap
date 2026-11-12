import { useEffect, useState, type FormEvent } from 'react'
import { actualizarConfiguracion, getConfiguracion } from '../services/api'
import { validarConfiguracion } from '../services/configuracion'
import type { ConfiguracionCorrida } from '../types'
import { AlmacenesEditor, AveriasEditor, CapacidadesEditor } from './ConfiguracionEditores'
import { Icon } from './Icon'

/**
 * Configuración de una corrida en curso: reglas de avería, ubicación y capacidad de almacenes y capacidad por tipo de
 * unidad. Al guardar, el motor la aplica en caliente (ver notas en pantalla sobre qué afecta cada cambio).
 */
export function ConfiguracionCorridaModal({ runId, onCerrar }: { runId: string; onCerrar: () => void }) {
  const [valor, setValor] = useState<ConfiguracionCorrida | null>(null)
  const [error, setError] = useState('')
  const [guardado, setGuardado] = useState(false)
  const [guardando, setGuardando] = useState(false)

  useEffect(() => {
    let activo = true
    getConfiguracion(runId).then((c) => { if (activo) setValor(c) }).catch((e: unknown) => { if (activo) setError(e instanceof Error ? e.message : 'No se pudo cargar la configuración.') })
    return () => { activo = false }
  }, [runId])

  const problema = valor ? validarConfiguracion(valor) : null
  function cambiar(parche: Partial<ConfiguracionCorrida>) { setGuardado(false); setValor((previo) => previo ? { ...previo, ...parche } : previo) }

  async function guardar(event: FormEvent) {
    event.preventDefault()
    if (!valor || problema) return
    setGuardando(true); setError(''); setGuardado(false)
    try { setValor(await actualizarConfiguracion(runId, valor)); setGuardado(true) }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo guardar la configuración.') }
    finally { setGuardando(false) }
  }

  return <div className="cfg-overlay" role="dialog" aria-modal="true" aria-label="Configuración de la corrida">
    <form className="cfg-modal" onSubmit={guardar}>
      <header><h2>Configuración de la corrida</h2><button type="button" className="icon-button" aria-label="Cerrar" onClick={onCerrar}><Icon name="close" /></button></header>
      {!valor && !error && <p className="no-results" role="status">Cargando configuración…</p>}
      {valor && <div className="cfg-body">
        <section><h3>Reglas de avería</h3><p className="cfg-note">Rigen para las averías que se registren desde que se guarda; las ya registradas no cambian.</p>
          <AveriasEditor valor={valor.averias} onChange={(averias) => cambiar({ averias })} /></section>
        <section><h3>Almacenes</h3><p className="cfg-note">Al mover un almacén, las unidades estacionadas se mueven con él y las que regresan recalculan su camino. Si la capacidad baja, el stock se ajusta; si sube, se completa en la próxima recarga (23:59:59).</p>
          <AlmacenesEditor valor={valor.almacenes} onChange={(almacenes) => cambiar({ almacenes })} /></section>
        <section><h3>Capacidad de las unidades</h3><p className="cfg-note">Rige para las rutas que salgan desde que se guarda; la carga que ya va a bordo no cambia. Un pedido pendiente que deje de caber se fracciona.</p>
          <CapacidadesEditor valor={valor.vehiculos} onChange={(vehiculos) => cambiar({ vehiculos })} /></section>
      </div>}
      {(problema || error) && <p className="register-error" role="alert">{problema ?? error}</p>}
      {guardado && <p className="register-success" role="status">✓ Configuración aplicada a la corrida.</p>}
      <footer><button type="button" className="secondary-button" onClick={onCerrar}>Cerrar</button>
        <button type="submit" className="start-button" disabled={!valor || !!problema || guardando}>{guardando ? 'Guardando…' : 'Guardar y aplicar'}</button></footer>
    </form>
  </div>
}

/** Botón de la cabecera que abre la configuración de la corrida. */
export function BotonConfiguracion({ runId }: { runId: string | null | undefined }) {
  const [abierto, setAbierto] = useState(false)
  if (!runId) return null
  return <>
    <button type="button" className="upload-pill" onClick={() => setAbierto(true)}><Icon name="settings" />Configuración</button>
    {abierto && <ConfiguracionCorridaModal runId={runId} onCerrar={() => setAbierto(false)} />}
  </>
}
