import { useEffect, useMemo, useState } from 'react'
import { MapaEnVivo } from '../components/mapa/MapaEnVivo'
import { useSnapshots } from '../hooks/useSnapshots'
import { createSource } from '../services/createSource'
import { cargarBloqueosEnCorrida, getEscenariosActivos, iniciarEscenario, USE_MOCK } from '../services/api'
import { formatFechaHora, localIso } from '../services/simulationTime'
import { RegistroPedidoForm, type CoordenadasPedido } from '../components/RegistroPedidoForm'
import { RegistroAveriaForm } from '../components/RegistroAveriaForm'
import { PlanificacionPill } from '../components/PlanificacionPill'
import { ALGORITMOS, FLOTA, GRID_ALTO, GRID_ANCHO, UMBRALES_SEMAFORO } from '../config/defaults'
import { ICONOS_TIPO, NOMBRES_TIPO } from '../config/theme'
import type { TipoVehiculo } from '../types/snapshot'
import { Icon } from '../components/Icon'
import { useSimulation } from '../context/SimulationContext'
import './operacion-diaria.css'

export function DailyOperationPage() {
  const { runIds, setRunId } = useSimulation()
  const [activeRunId, setActiveRunId] = useState<string | null>(USE_MOCK ? runIds.DIARIO ?? 'mock-diario-default' : null)
  const [buscando, setBuscando] = useState(!USE_MOCK)
  const [loading, setLoading] = useState(false)
  const [algoritmo, setAlgoritmo] = useState<'ALNS' | 'ATS'>('ALNS')
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  const [avisoBloqueos, setAvisoBloqueos] = useState<{ texto: string; error: boolean } | null>(null)

  // La operación diaria vive en el servidor (arranca sola); cualquier dispositivo se une a la corrida activa.
  useEffect(() => {
    if (USE_MOCK) return
    let mounted = true
    getEscenariosActivos().then((runs) => {
      if (!mounted) return
      const diario = runs.find((r) => r.escenario === 'DIARIO' && r.estado === 'EN_CURSO')
      setActiveRunId(diario?.runId ?? null)
      if (diario) setRunId('DIARIO', diario.runId)
      setBuscando(false)
    })
    return () => { mounted = false }
  }, [setRunId])

  const source = useMemo(() => activeRunId ? createSource('DIARIO', activeRunId) : null, [activeRunId])
  const { snapshot, estadoConexion } = useSnapshots(source)
  const [coordenadas, setCoordenadas] = useState<CoordenadasPedido>({ x: '', y: '' })
  const [eligiendo, setEligiendo] = useState(false)
  const x = Number(coordenadas.x), y = Number(coordenadas.y)
  const puntoElegido = coordenadas.x !== '' && coordenadas.y !== '' && Number.isInteger(x) && Number.isInteger(y) && x >= 0 && x <= GRID_ANCHO && y >= 0 && y <= GRID_ALTO ? { x, y } : null
  const detenida = !!snapshot && snapshot.estado !== 'EN_CURSO'
  const disponible = !!snapshot && !detenida && (estadoConexion === 'CONECTADO' || USE_MOCK)

  async function handleIniciarOperacion() {
    setLoading(true)
    setErrorMsg(null)
    try {
      const res = await iniciarEscenario('DIARIO', {
        escenario: 'DIARIO', algoritmo, fechaInicio: localIso(new Date()), umbralesSemaforo: UMBRALES_SEMAFORO,
        flota: { auto: FLOTA[0].cantidad, moto: FLOTA[1].cantidad, bicicleta: FLOTA[2].cantidad },
      })
      setActiveRunId(res.runId)
      setRunId('DIARIO', res.runId)
    } catch (err) {
      setErrorMsg(err instanceof Error ? err.message : 'No se pudo iniciar la operación diaria.')
    } finally {
      setLoading(false)
    }
  }

  async function handleBloqueos(file?: File) {
    if (!file || !activeRunId) return
    setAvisoBloqueos({ texto: `Cargando ${file.name}…`, error: false })
    try {
      const r = await cargarBloqueosEnCorrida(activeRunId, file)
      setAvisoBloqueos({ texto: `${r.totalPedidos} bloqueos cargados${r.errores.length ? ` · ${r.errores.length} líneas con error (línea ${r.errores[0].linea}: ${r.errores[0].mensaje})` : ''}.`, error: r.errores.length > 0 && r.totalPedidos === 0 })
    } catch (err) {
      setAvisoBloqueos({ texto: err instanceof Error ? err.message : 'No se pudieron cargar los bloqueos.', error: true })
    }
  }

  if (buscando) return <div className="parameters p-8 text-center text-gray-600" role="status">Buscando la operación diaria en curso…</div>

  if (!activeRunId || detenida) {
    return <div className="parameters p-8 flex flex-col items-center justify-center">
      <div className="card max-w-lg w-full p-6 text-center">
        <h2>{detenida ? 'La operación diaria fue detenida' : 'No hay una operación en curso'}</h2>
        <p className="mt-2 text-gray-600">Selecciona el algoritmo y comienza la operación del día.</p>
        <label className="field-label mt-4 block text-left" htmlFor="algoritmo-diario">ALGORITMO</label>
        <select id="algoritmo-diario" className="w-full p-2 border rounded mt-1" value={algoritmo} onChange={(e) => setAlgoritmo(e.target.value as 'ALNS' | 'ATS')}>
          {ALGORITMOS.map((a) => <option key={a}>{a}</option>)}
        </select>
        {errorMsg && <p className="text-red-600 mt-2">{errorMsg}</p>}
        <button type="button" className="start-button mt-6 w-full justify-center" disabled={loading} onClick={handleIniciarOperacion}>
          <Icon name="play" /> {loading ? 'Iniciando...' : 'Iniciar operación del día'}
        </button>
      </div>
    </div>
  }

  const flota = snapshot?.metricas.flota
  return <div className="map-page daily-page">
    <header className="daily-header"><div className="daily-heading"><div><h1>Operación en Tiempo Real</h1><span className="live-badge"><i />En vivo — <time dateTime={snapshot?.simTimeIso}>{formatFechaHora(snapshot?.simTimeIso)}</time></span>
      {!USE_MOCK && <label className="upload-pill"><Icon name="upload" />Cargar bloqueos<input type="file" accept=".txt,.csv" className="sr-only" onChange={(e) => { void handleBloqueos(e.target.files?.[0]); e.target.value = '' }} /></label>}
      <PlanificacionPill snapshot={snapshot} /></div>
      <p>Supervisión y despacho continuo de flota activa</p><span className={`daily-connection ${estadoConexion.toLowerCase()}`} role="status">● {estadoConexion === 'CONECTADO' ? 'Conectado' : estadoConexion === 'ERROR' ? 'Error de conexión' : estadoConexion === 'DESCONECTADO' ? 'Desconectado' : 'Conectando…'}</span>
      {avisoBloqueos && <span className={`daily-feedback ${avisoBloqueos.error ? 'error' : ''}`} role="status"> · {avisoBloqueos.texto}</span>}</div>
      <div className="fleet-counters" aria-label="Flota configurada">{FLOTA.map((item) => { const type = item.id.toUpperCase() as TipoVehiculo; return <span key={item.id} title={NOMBRES_TIPO[type]}><Icon name={ICONOS_TIPO[type]} /><strong>{flota?.[item.id] ?? item.cantidad}</strong><span className="sr-only">{NOMBRES_TIPO[type]}</span></span> })}</div>
      <div className="package-counter"><span>PEDIDOS</span><strong>{snapshot?.metricas.entregados ?? '—'} <i>/</i> {snapshot?.metricas.pedidosTotal ?? '—'}</strong><small>entregados / total</small></div>
    </header>
    <MapaEnVivo snapshot={snapshot} estadoConexion={estadoConexion} escenario="DIARIO" etiquetaRegistro="Registro" seleccionandoPunto={eligiendo} puntoElegido={puntoElegido} onElegirPunto={(pickedX, pickedY) => { setCoordenadas({ x: String(pickedX), y: String(pickedY) }); setEligiendo(false) }} onCancelarSeleccion={() => setEligiendo(false)}
      panelRegistrar={<>
        <RegistroPedidoForm coordenadas={coordenadas} setCoordenadas={setCoordenadas} eligiendo={eligiendo} onElegir={() => setEligiendo(true)} onCancelar={() => setEligiendo(false)} disponible={disponible} />
        {!USE_MOCK && <RegistroAveriaForm runId={activeRunId} vehiculos={snapshot?.vehiculos ?? []} disponible={disponible} />}
      </>} />
  </div>
}
