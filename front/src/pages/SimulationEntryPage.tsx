import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { useSimulation } from '../context/SimulationContext'
import { getEscenariosActivos } from '../services/api'

/** Resolve the simulation entry from server state, including visits from other devices. */
export function SimulationEntryPage() {
  const { parametros, setParametros, setRunId } = useSimulation()
  const [destination, setDestination] = useState<string | null>(null)
  useEffect(() => {
    let mounted = true
    void getEscenariosActivos().then((runs) => {
      if (!mounted) return
      const simulations = runs.filter((run) => run.estado === 'EN_CURSO' && (run.escenario === '5D' || run.escenario === 'COLAPSO'))
      const active = simulations.find((run) => run.escenario === parametros.escenario) ?? simulations[0]
      if (active) {
        setRunId(active.escenario, active.runId)
        if (parametros.escenario !== active.escenario) setParametros({ ...parametros, escenario: active.escenario })
      }
      setDestination(active ? '/simulacion/mapa' : '/simulacion/parametros')
    })
    return () => { mounted = false }
  }, [parametros, setParametros, setRunId])
  return destination ? <Navigate to={destination} replace /> : <div className="parameters" role="status">Buscando simulaciones en curso…</div>
}
