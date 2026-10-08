import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { MapaEnVivo } from '../components/mapa/MapaEnVivo'
import { useSimulation } from '../context/SimulationContext'
import { useSnapshots } from '../hooks/useSnapshots'
import { createSource } from '../services/createSource'
import { getEscenariosActivos } from '../services/api'
import type { SnapshotSource } from '../services/snapshotSource'
import { MapPageHeader } from './MapPageHeader'
import type { EscenarioActivoInfo } from '../types'

export function SimulationMapPage() {
  const { parametros, runIds, setRunId } = useSimulation()
  const navigate = useNavigate()

  const currentEscenario = parametros.escenario || '5D'
  const existingRunId = runIds[currentEscenario] || runIds['5D'] || runIds['COLAPSO'] || `mock-${currentEscenario.toLowerCase()}-${Date.now()}`

  const [activeRuns, setActiveRuns] = useState<EscenarioActivoInfo[]>([])
  const [selectedRun, setSelectedRun] = useState<EscenarioActivoInfo>(() => ({
    runId: existingRunId,
    escenario: currentEscenario,
    estado: 'EN_CURSO',
    topic: `/topic/sim/${existingRunId}`,
  }))
  const [showSummaryModal, setShowSummaryModal] = useState(false)
  const [modalAutoOpened, setModalAutoOpened] = useState(false)

  useEffect(() => {
    let mounted = true
    async function init() {
      const runs = await getEscenariosActivos().catch(() => [])
      const simRuns = runs.filter((r) => r.escenario === '5D' || r.escenario === 'COLAPSO')
      if (!mounted) return
      setActiveRuns(simRuns)

      if (simRuns.length > 0) {
        const target = simRuns.find((r) => r.runId === runIds[currentEscenario] || r.escenario === currentEscenario) || simRuns[0]
        setSelectedRun(target)
        setRunId(target.escenario, target.runId)
      } else {
        setRunId(currentEscenario, existingRunId)
      }
    }
    init()
  }, [currentEscenario, runIds, setRunId, existingRunId])

  const source = useMemo<SnapshotSource>(() => {
    return createSource(selectedRun.escenario, selectedRun.runId, parametros)
  }, [selectedRun, parametros])

  const { snapshot, estadoConexion } = useSnapshots(source)

  useEffect(() => {
    if (snapshot && (snapshot.estado === 'FINALIZADO' || snapshot.estado === 'COLAPSADO')) {
      if (!modalAutoOpened) {
        setShowSummaryModal(true)
        setModalAutoOpened(true)
      }
      source.stop()
    }
  }, [snapshot?.estado, modalAutoOpened, source])

  useEffect(() => {
    return () => {
      source.stop()
    }
  }, [source])

  const isColapsed = snapshot?.estado === 'COLAPSADO'
  const isFinished = snapshot?.estado === 'FINALIZADO'

  return <div className="map-page">
    <MapPageHeader snapshot={snapshot} escenario={selectedRun.escenario} />

    {activeRuns.length > 1 && <div className="bg-blue-50 border border-blue-200 p-3 mx-6 mt-4 rounded-lg flex items-center justify-between">
      <span className="font-semibold text-blue-900">Múltiples simulaciones en curso:</span>
      <div className="flex gap-2">
        {activeRuns.map((r) => <button key={r.runId} type="button" className={`px-3 py-1 rounded text-sm ${selectedRun.runId === r.runId ? 'bg-blue-600 text-white font-bold' : 'bg-white text-blue-800 border'}`} onClick={() => {
          setSelectedRun(r)
          setRunId(r.escenario, r.runId)
          setModalAutoOpened(false)
        }}>{r.escenario === '5D' ? 'Simulación 5D' : 'Colapso Logístico'}</button>)}
      </div>
    </div>}

    {isColapsed && <div className="run-alert flex items-center justify-between p-4 bg-red-600 text-white font-bold mx-6 mt-4 rounded-lg" role="alert">
      <span>⚠ Simulación detenida por Colapso Logístico — {snapshot?.causa}</span>
      <button type="button" className="bg-white text-red-700 px-4 py-2 rounded shadow hover:bg-gray-100" onClick={() => setShowSummaryModal(true)}>Ver resumen</button>
    </div>}

    {isFinished && <div className="run-complete flex items-center justify-between p-4 bg-green-600 text-white font-bold mx-6 mt-4 rounded-lg" role="status">
      <span>✓ Corrida finalizada. {snapshot?.resumen?.entregados} de {snapshot?.resumen?.pedidosTotal} pedidos entregados.</span>
      <button type="button" className="bg-white text-green-700 px-4 py-2 rounded shadow hover:bg-gray-100" onClick={() => setShowSummaryModal(true)}>Ver resumen</button>
    </div>}

    <MapaEnVivo snapshot={snapshot} estadoConexion={estadoConexion} escenario={selectedRun.escenario} />

    {showSummaryModal && snapshot?.resumen && <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-2xl max-w-md w-full p-6 text-gray-800">
        <h2 className="text-xl font-bold mb-1">{isColapsed ? 'Colapso logístico' : 'Simulación finalizada'}</h2>
        <p className="text-sm text-gray-500 mb-4">Resumen de ejecución y métricas finales</p>

        <div className="space-y-3 bg-gray-50 p-4 rounded-lg text-sm">
          <div className="flex justify-between"><span>Pedidos totales:</span><strong>{snapshot.resumen.pedidosTotal}</strong></div>
          <div className="flex justify-between"><span>Entregados:</span><strong className="text-green-600">{snapshot.resumen.entregados}</strong></div>
          <div className="flex justify-between"><span>Atrasados:</span><strong className="text-red-600">{snapshot.resumen.atrasados}</strong></div>
          <div className="flex justify-between"><span>No asignados:</span><strong>{snapshot.resumen.noAsignados}</strong></div>
          <div className="flex justify-between border-t pt-2 font-bold text-base"><span>Costo total:</span><span>S/ {snapshot.resumen.costoTotal.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span></div>
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
