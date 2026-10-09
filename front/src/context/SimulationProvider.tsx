import { useState, type ReactNode } from 'react'
import { crearParametrosIniciales } from '../config/defaults'
import type { EstadoSimulacion } from '../types'
import { SimulationContext } from './SimulationContext'
export function SimulationProvider({ children }: { children: ReactNode }) {
  const [estado, setEstado] = useState<EstadoSimulacion>(() => ({
    parametros: crearParametrosIniciales(), archivoPedidos: null, infoArchivoPedidos: null, runIds: { DIARIO: 'mock-diario-default' },
  }))
  return <SimulationContext.Provider value={{ ...estado,
    setParametros: (parametros) => setEstado((prev) => ({ ...prev, parametros })),
    setArchivo: (archivoPedidos, infoArchivoPedidos) => setEstado((prev) => ({ ...prev, archivoPedidos, infoArchivoPedidos })),
    setRunId: (escenario, runId) => setEstado((prev) => ({ ...prev, runIds: { ...prev.runIds, [escenario]: runId } })),
    clearRunId: (escenario) => setEstado((prev) => {
      const nextRunIds = { ...prev.runIds }
      delete nextRunIds[escenario]
      return { ...prev, runIds: nextRunIds }
    }),
  }}>{children}</SimulationContext.Provider>
}
