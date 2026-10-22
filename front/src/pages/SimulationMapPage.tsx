import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { MapaEnVivo } from '../components/mapa/MapaEnVivo'
import { RegistroAveriaForm } from '../components/RegistroAveriaForm'
import { useSimulation } from '../context/SimulationContext'
import { useSnapshots } from '../hooks/useSnapshots'
import { createSource } from '../services/createSource'
import { detenerCorrida, getEscenariosActivos, USE_MOCK } from '../services/api'
import { readableSimTime } from '../services/simulationTime'
import { MapPageHeader } from './MapPageHeader'
import type { EscenarioActivoInfo } from '../types'
import './operacion-diaria.css'

type Simulacion = Pick<EscenarioActivoInfo, 'runId' | 'escenario'>

export function SimulationMapPage() {
  const { parametros, runIds, setRunId } = useSimulation()
  const navigate = useNavigate()
  const preferido = parametros.escenario === 'DIARIO' ? '5D' : parametros.escenario
  const conocido = (['5D', 'COLAPSO'] as const).map((e) => runIds[e] ? { runId: runIds[e]!, escenario: e } : null).find((r) => r?.escenario === preferido) ??
    (runIds['5D'] ? { runId: runIds['5D'], escenario: '5D' as const } : runIds.COLAPSO ? { runId: runIds.COLAPSO, escenario: 'COLAPSO' as const } : null)

  const [activeRuns, setActiveRuns] = useState<EscenarioActivoInfo[]>([])
  const [selectedRun, setSelectedRun] = useState<Simulacion | null>(conocido)
  const [buscando, setBuscando] = useState(!conocido)
  // El resumen se abre solo al terminar; se recuerda la corrida cuyo resumen el usuario cerró.
  const [resumenCerrado, setResumenCerrado] = useState<string | null>(null)
  const [deteniendo, setDeteniendo] = useState(false)

  // Un dispositivo que entra tarde descubre las simulaciones en curso y se une a una.
  useEffect(() => {
    let mounted = true
    getEscenariosActivos().then((runs) => {
      if (!mounted) return
      const simRuns = runs.filter((r) => r.escenario === '5D' || r.escenario === 'COLAPSO')
      setActiveRuns(simRuns)
      setSelectedRun((actual) => actual ?? simRuns.find((r) => r.escenario === preferido) ?? simRuns[0] ?? null)
      setBuscando(false)
    })
    return () => { mounted = false }
  }, [preferido])

  useEffect(() => { if (selectedRun) setRunId(selectedRun.escenario as '5D' | 'COLAPSO', selectedRun.runId) }, [selectedRun, setRunId])

  const source = useMemo(() => selectedRun ? createSource(selectedRun.escenario, selectedRun.runId, parametros) : null, [selectedRun]) // eslint-disable-line react-hooks/exhaustive-deps
  const { snapshot, estadoConexion } = useSnapshots(source)

  const terminado = !!snapshot && snapshot.estado !== 'EN_CURSO'
  const showSummaryModal = terminado && resumenCerrado !== snapshot?.runId
  const setShowSummaryModal = (abrir: boolean) => setResumenCerrado(abrir ? null : snapshot?.runId ?? null)
  // La corrida terminada ya no publica: se congela el mapa con el último snapshot.
  useEffect(() => { if (terminado) source?.stop() }, [terminado, source])

  async function detener() {
    if (!selectedRun || !window.confirm('¿Detener la simulación en curso? Se mostrará el resumen hasta este momento.')) return
    setDeteniendo(true)
    try { await detenerCorrida(selectedRun.runId) } catch (err) { window.alert(err instanceof Error ? err.message : 'No se pudo detener la corrida.') }
    finally { setDeteniendo(false) }
  }

  if (buscando) return <div className="parameters p-8 text-center text-gray-600" role="status">Buscando simulaciones en curso…</div>
  if (!selectedRun) return <section className="placeholder card"><h2>No hay simulaciones en curso</h2><p>Configura e inicia una corrida 5D o de colapso logístico.</p><Link className="secondary-button" to="/simulacion/parametros">Ir a parámetros</Link></section>

  const isColapsed = snapshot?.estado === 'COLAPSADO'
  const isFinished = snapshot?.estado === 'FINALIZADO' || snapshot?.estado === 'DETENIDA'
  const resumen = snapshot?.resumen

  return <div className="map-page">
    <MapPageHeader snapshot={snapshot} escenario={selectedRun.escenario} onDetener={USE_MOCK ? undefined : detener} deteniendo={deteniendo} />

    {activeRuns.length > 1 && <div className="bg-blue-50 border border-blue-200 p-3 mx-6 mt-4 rounded-lg flex items-center justify-between">
      <span className="font-semibold text-blue-900">Múltiples simulaciones en curso:</span>
      <div className="flex gap-2">
        {activeRuns.map((r) => <button key={r.runId} type="button" className={`px-3 py-1 rounded text-sm ${selectedRun.runId === r.runId ? 'bg-blue-600 text-white font-bold' : 'bg-white text-blue-800 border'}`} onClick={() => {
          setSelectedRun(r)
        }}>{r.escenario === '5D' ? 'Simulación 5D' : 'Colapso Logístico'}</button>)}
      </div>
    </div>}

    {isColapsed && <div className="run-alert flex items-center justify-between p-4 bg-red-600 text-white font-bold mx-6 mt-4 rounded-lg" role="alert">
      <span>⚠ Simulación detenida por Colapso Logístico — {snapshot?.causa}</span>
      <button type="button" className="bg-white text-red-700 px-4 py-2 rounded shadow hover:bg-gray-100" onClick={() => setShowSummaryModal(true)}>Ver resumen</button>
    </div>}

    {isFinished && <div className="run-complete flex items-center justify-between p-4 bg-green-600 text-white font-bold mx-6 mt-4 rounded-lg" role="status">
      <span>✓ {snapshot?.estado === 'DETENIDA' ? 'Corrida detenida por el operador.' : 'Corrida finalizada.'} {resumen?.entregados} de {resumen?.pedidosTotal} pedidos entregados.</span>
      <button type="button" className="bg-white text-green-700 px-4 py-2 rounded shadow hover:bg-gray-100" onClick={() => setShowSummaryModal(true)}>Ver resumen</button>
    </div>}

    <MapaEnVivo snapshot={snapshot} estadoConexion={estadoConexion} escenario={selectedRun.escenario}
      etiquetaRegistro="Incidencias" panelRegistrar={USE_MOCK ? undefined : <RegistroAveriaForm runId={selectedRun.runId} vehiculos={snapshot?.vehiculos ?? []} disponible={!!snapshot && !terminado} />} />

    {showSummaryModal && resumen && <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-2xl max-w-md w-full p-6 text-gray-800">
        <h2 className="text-xl font-bold mb-1">{isColapsed ? 'Colapso logístico' : snapshot?.estado === 'DETENIDA' ? 'Simulación detenida' : 'Simulación finalizada'}</h2>
        <p className="text-sm text-gray-500 mb-4">Resumen de ejecución y métricas finales{resumen.simTimeFinal ? ` · ${readableSimTime(resumen.simTimeFinal)}` : ''}</p>
        {snapshot?.causa && <p className="text-sm mb-3 text-gray-700">{snapshot.causa}</p>}

        <div className="space-y-3 bg-gray-50 p-4 rounded-lg text-sm">
          <div className="flex justify-between"><span>Pedidos totales:</span><strong>{resumen.pedidosTotal}</strong></div>
          <div className="flex justify-between"><span>Entregados:</span><strong className="text-green-600">{resumen.entregados}</strong></div>
          <div className="flex justify-between"><span>Atrasados:</span><strong className="text-red-600">{resumen.atrasados}</strong></div>
          <div className="flex justify-between"><span>No asignados:</span><strong>{resumen.noAsignados}</strong></div>
          {resumen.cumplimientoPct !== undefined && <div className="flex justify-between"><span>Cumplimiento de plazos:</span><strong>{resumen.cumplimientoPct.toLocaleString('es-PE')} %</strong></div>}
          {resumen.distanciaKm !== undefined && <div className="flex justify-between"><span>Distancia recorrida:</span><strong>{resumen.distanciaKm.toLocaleString('es-PE')} km</strong></div>}
          <div className="flex justify-between border-t pt-2 font-bold text-base"><span>Costo total:</span><span>S/ {resumen.costoTotal.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span></div>
        </div>

        <div className="mt-6 flex gap-3">
          <button type="button" className="secondary-button flex-1" onClick={() => setShowSummaryModal(false)}>Cerrar</button>
          <button type="button" className="start-button flex-1 justify-center" onClick={() => {
            setShowSummaryModal(false)
            navigate('/simulacion/parametros')
          }}>Volver a Parámetros</button>
        </div>
      </div>
    </div>}
  </div>
}
