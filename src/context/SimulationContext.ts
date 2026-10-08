import { createContext, useContext } from 'react'
import type { ArchivoPedidosInfo, EstadoSimulacion, ParametrosCorrida } from '../types'
export interface SimulationContextValue extends EstadoSimulacion {
  setParametros: (parametros: ParametrosCorrida) => void
  setArchivo: (file: File | null, info: ArchivoPedidosInfo | null) => void
  setRunId: (escenario: '5D' | 'COLAPSO' | 'DIARIO', runId: string | undefined) => void
  clearRunId: (escenario: '5D' | 'COLAPSO' | 'DIARIO') => void
}
export const SimulationContext = createContext<SimulationContextValue | null>(null)
export function useSimulation() {
  const context = useContext(SimulationContext)
  if (!context) throw new Error('Falta SimulationProvider')
  return context
}
