import { useState, useEffect } from 'react'
import { MapaEnVivo } from '../components/mapa/MapaEnVivo'
import { useSnapshots } from '../hooks/useSnapshots'
import { createSource } from '../services/createSource'
import { getEscenariosActivos, iniciarEscenario } from '../services/api'
import type { SnapshotSource } from '../services/snapshotSource'
import { formatFechaHora } from '../services/simulationTime'
import { RegistroPedidoForm, type CoordenadasPedido } from '../components/RegistroPedidoForm'
import { ALGORITMOS, FLOTA, GRID_ALTO, GRID_ANCHO, UMBRALES_SEMAFORO } from '../config/defaults'
import { ICONOS_TIPO, NOMBRES_TIPO } from '../config/theme'
import type { TipoVehiculo } from '../types/snapshot'
import { Icon } from '../components/Icon'
import { useSimulation } from '../context/SimulationContext'
import './operacion-diaria.css'

export function DailyOperationPage() {
  const { runIds, setRunId, parametros } = useSimulation()
  const initialRunId = runIds.DIARIO || 'mock-diario-default'
  const [activeRunId, setActiveRunId] = useState<string | null>(initialRunId)
  const [loading, setLoading] = useState(false)
  const [algoritmo, setAlgoritmo] = useState<'ALNS' | 'ATS'>('ALNS')
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  const [source, setSource] = useState<SnapshotSource>(() => createSource('DIARIO', initialRunId, parametros))

  useEffect(() => {
    let mounted = true
    async function checkDiario() {
      const runs = await getEscenariosActivos().catch(() => [])
      const diarioRun = runs.find((r) => r.escenario === 'DIARIO' && r.estado === 'EN_CURSO')
      if (diarioRun && mounted) {
        setActiveRunId(diarioRun.runId)
        setRunId('DIARIO', diarioRun.runId)
        setSource(createSource('DIARIO', diarioRun.runId, parametros))
      }
    }
    checkDiario()
    return () => {
      mounted = false
      source.stop()
    }
  }, [])

  const { snapshot, estadoConexion } = useSnapshots(source)
  const [coordenadas, setCoordenadas] = useState<CoordenadasPedido>({ x: '', y: '' })
  const [eligiendo, setEligiendo] = useState(false)
  const x = Number(coordenadas.x), y = Number(coordenadas.y)
  const puntoElegido = coordenadas.x !== '' && coordenadas.y !== '' && Number.isInteger(x) && Number.isInteger(y) && x >= 0 && x <= GRID_ANCHO && y >= 0 && y <= GRID_ALTO ? { x, y } : null

  async function handleIniciarOperacion() {
    setLoading(true)
    setErrorMsg(null)
    try {
      const res = await iniciarEscenario('DIARIO', {
        escenario: 'DIARIO',
        algoritmo,
        fechaInicio: new Date().toISOString(),
        umbralesSemaforo: UMBRALES_SEMAFORO,
        flota: { auto: 15, moto: 15, bicicleta: 7 },
      })
      setActiveRunId(res.runId)
      setRunId('DIARIO', res.runId)
      const s = createSource('DIARIO', res.runId, parametros)
      setSource(s)
    } catch (err) {
      setErrorMsg(err instanceof Error ? err.message : 'No se pudo iniciar la operación diaria.')
    } finally {
      setLoading(false)
    }
  }

  if (!activeRunId) {
    return <div className="parameters p-8 flex flex-col items-center justify-center">
      <div className="card max-w-lg w-full p-6 text-center">
        <h2>No hay una operación en curso</h2>
        <p className="mt-2 text-gray-600">Selecciona el algoritmo y comienza la operación del día.</p>
        <label className="field-label mt-4 block text-left">ALGORITMO</label>
        <select className="w-full p-2 border rounded mt-1" value={algoritmo} onChange={(e) => setAlgoritmo(e.target.value as any)}>
          {ALGORITMOS.map((a) => <option key={a}>{a}</option>)}
        </select>
        {errorMsg && <p className="text-red-600 mt-2">{errorMsg}</p>}
        <button type="button" className="start-button mt-6 w-full justify-center" disabled={loading} onClick={handleIniciarOperacion}>
          <Icon name="play" /> {loading ? 'Iniciando...' : 'Iniciar operación del día'}
        </button>
      </div>
    </div>
  }

  return <div className="map-page daily-page">
    <header className="daily-header"><div className="daily-heading"><div><h1>Operación en Tiempo Real</h1><span className="live-badge"><i />En vivo — <time dateTime={snapshot?.simTimeIso}>{formatFechaHora(snapshot?.simTimeIso)}</time></span></div><p>Supervisión y despacho continuo de flota activa</p><span className={`daily-connection ${estadoConexion.toLowerCase()}`} role="status">● {estadoConexion === 'CONECTADO' ? 'Conectado' : estadoConexion === 'ERROR' ? 'Error de conexión' : estadoConexion === 'DESCONECTADO' ? 'Desconectado' : 'Conectando…'}</span></div>
      <div className="fleet-counters" aria-label="Flota configurada">{FLOTA.map((item) => { const type = item.id.toUpperCase() as TipoVehiculo; return <span key={item.id} title={NOMBRES_TIPO[type]}><Icon name={ICONOS_TIPO[type]} /><strong>{item.cantidad}</strong><span className="sr-only">{NOMBRES_TIPO[type]}</span></span> })}</div>
      <div className="package-counter"><span>PAQUETES</span><strong>{snapshot?.metricas.entregados ?? '—'} <i>/</i> {snapshot?.metricas.pedidosTotal ?? '—'}</strong><small>entregados / total</small></div>
    </header>
    <MapaEnVivo snapshot={snapshot} estadoConexion={estadoConexion} escenario="DIARIO" seleccionandoPunto={eligiendo} puntoElegido={puntoElegido} onElegirPunto={(pickedX, pickedY) => { setCoordenadas({ x: String(pickedX), y: String(pickedY) }); setEligiendo(false) }} onCancelarSeleccion={() => setEligiendo(false)} panelRegistrar={<RegistroPedidoForm coordenadas={coordenadas} setCoordenadas={setCoordenadas} eligiendo={eligiendo} onElegir={() => setEligiendo(true)} onCancelar={() => setEligiendo(false)} disponible={estadoConexion === 'CONECTADO' && !!snapshot} />} />
  </div>
}
