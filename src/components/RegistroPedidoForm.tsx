import { useRef, useState, type FormEvent } from 'react'
import { GRID_ALTO, GRID_ANCHO, HORIZONTES_VALIDOS, REGISTRO_PEDIDOS } from '../config/defaults'
import { registrarPedido } from '../services/api'
import { validarNuevoPedido } from '../services/validarNuevoPedido'
import { formatFechaHora } from '../services/simulationTime'
import type { PedidoRegistrado } from '../types/pedidos'
import { Icon } from './Icon'

export interface CoordenadasPedido { x: string; y: string }
interface Props {
  coordenadas: CoordenadasPedido
  setCoordenadas: (value: CoordenadasPedido) => void
  eligiendo: boolean
  onElegir: () => void
  onCancelar: () => void
  disponible: boolean
}

export function RegistroPedidoForm({ coordenadas, setCoordenadas, eligiendo, onElegir, onCancelar, disponible }: Props) {
  const [clienteId, setClienteId] = useState('')
  const [cantidad, setCantidad] = useState(String(REGISTRO_PEDIDOS.cantidadInicial))
  const [horizonte, setHorizonte] = useState<number>(REGISTRO_PEDIDOS.horizonteRegular)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState('')
  const [confirmacion, setConfirmacion] = useState<PedidoRegistrado | null>(null)
  const [ultimos, setUltimos] = useState<PedidoRegistrado[]>([])
  const pending = useRef(false)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (pending.current) return
    setError(''); setConfirmacion(null)
    const input = { x: coordenadas.x.trim() ? Number(coordenadas.x) : NaN, y: coordenadas.y.trim() ? Number(coordenadas.y) : NaN, clienteId, cantidad: Number(cantidad), horizonteHoras: horizonte }
    const invalid = validarNuevoPedido(input)
    if (invalid) { setError(invalid); return }
    pending.current = true; setEnviando(true); onCancelar()
    try {
      const pedido = await registrarPedido(input)
      setConfirmacion(pedido)
      setUltimos((previous) => [pedido, ...previous].slice(0, REGISTRO_PEDIDOS.ultimosVisibles))
      setCoordenadas({ x: '', y: '' }); setClienteId(''); setCantidad(String(REGISTRO_PEDIDOS.cantidadInicial)); setHorizonte(REGISTRO_PEDIDOS.horizonteRegular)
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo registrar el pedido. Intenta de nuevo.') }
    finally { pending.current = false; setEnviando(false) }
  }

  return <>
    <form className="register-order-form" onSubmit={submit} noValidate aria-label="Registrar nuevo pedido">
      <div className="register-order-heading"><Icon name="box" /><h3>Nuevo pedido</h3><span>DESPACHO</span></div>
      <fieldset disabled={enviando}>
        <div className="coordinate-fields"><label>Coordenada X<input type="number" min={0} max={GRID_ANCHO} step={1} required placeholder={`0–${GRID_ANCHO}`} value={coordenadas.x} onChange={(e) => setCoordenadas({ ...coordenadas, x: e.target.value })} /></label><label>Coordenada Y<input type="number" min={0} max={GRID_ALTO} step={1} required placeholder={`0–${GRID_ALTO}`} value={coordenadas.y} onChange={(e) => setCoordenadas({ ...coordenadas, y: e.target.value })} /></label></div>
        <button type="button" className={`pick-location ${eligiendo ? 'active' : ''}`} aria-pressed={eligiendo} disabled={!disponible} onClick={eligiendo ? onCancelar : onElegir}><Icon name="target" />{eligiendo ? 'Cancelar selección' : 'Elegir en el mapa'}</button>
        {eligiendo && <p className="pick-hint">El siguiente clic en el mapa completará X e Y.</p>}
        <label className="register-field">ID de cliente<input type="text" required placeholder="Ej. c9103" value={clienteId} onChange={(e) => setClienteId(e.target.value)} /></label>
        <label className="register-field">Cantidad<input type="number" min={1} step={1} required value={cantidad} onChange={(e) => setCantidad(e.target.value)} /></label>
        <label className="register-field">Plazo de entrega<select value={horizonte} onChange={(e) => setHorizonte(Number(e.target.value))}>{HORIZONTES_VALIDOS.map((hours) => <option key={hours} value={hours}>{hours} horas — {hours === REGISTRO_PEDIDOS.horizonteRegular ? 'Regular' : 'Priorizado'}</option>)}</select></label>
        <p className="register-note">El plazo se cuenta desde el momento del registro.</p>
        <button className="register-submit" type="submit" disabled={!disponible || enviando}><Icon name="file" />{enviando ? 'Registrando…' : 'Registrar pedido'}</button>
      </fieldset>
      {error && <p className="register-error" role="alert">{error}</p>}
      {confirmacion && <div className="register-success" role="status"><strong>✓ Pedido {confirmacion.id} registrado</strong><span>Entrega hasta {formatFechaHora(confirmacion.deadline)}</span></div>}
    </form>
    <section className="recent-orders" aria-label="Últimos pedidos registrados"><div><h3>Últimos pedidos registrados</h3><span>{ultimos.length} / {REGISTRO_PEDIDOS.ultimosVisibles}</span></div>
      {ultimos.length ? <ol>{ultimos.map((pedido) => <li key={pedido.id}><div><strong>{pedido.id}</strong><span>{pedido.cantidad} paq.</span></div><p>Entrega: <time dateTime={pedido.deadline}>{formatFechaHora(pedido.deadline)}</time></p></li>)}</ol> : <p className="recent-empty">Aún no registraste pedidos en esta sesión.</p>}
    </section>
  </>
}
