import { useCallback, useMemo, useState, type ReactNode } from 'react'
import { crearParametrosIniciales } from '../config/defaults'
import { USE_MOCK } from '../services/api'
import type { ArchivoPedidosInfo, EstadoSimulacion, ParametrosCorrida } from '../types'
import { SimulationContext } from './SimulationContext'
export function SimulationProvider({ children }: { children: ReactNode }) {
  const [estado, setEstado] = useState<EstadoSimulacion>(() => ({
    parametros: crearParametrosIniciales(), archivoPedidos: null, infoArchivoPedidos: null, archivoBloqueos: null, infoArchivoBloqueos: null,
    // Con el backend real, el runId de DIARIO se obtiene de /api/escenarios/activos.
    runIds: USE_MOCK ? { DIARIO: 'mock-diario-default' } : {},
  }))
  // Funciones estables: las páginas las usan en efectos sin volver a dispararlos.
  const setParametros = useCallback((parametros: ParametrosCorrida) => setEstado((prev) => ({ ...prev, parametros })), [])
  const setArchivo = useCallback((archivoPedidos: File | null, infoArchivoPedidos: ArchivoPedidosInfo | null) => setEstado((prev) => ({ ...prev, archivoPedidos, infoArchivoPedidos })), [])
  const setArchivoBloqueos = useCallback((archivoBloqueos: File | null, infoArchivoBloqueos: ArchivoPedidosInfo | null) => setEstado((prev) => ({ ...prev, archivoBloqueos, infoArchivoBloqueos })), [])
  const setRunId = useCallback((escenario: '5D' | 'COLAPSO' | 'DIARIO', runId: string | undefined) => setEstado((prev) => prev.runIds[escenario] === runId ? prev : ({ ...prev, runIds: { ...prev.runIds, [escenario]: runId } })), [])
  const clearRunId = useCallback((escenario: '5D' | 'COLAPSO' | 'DIARIO') => setEstado((prev) => {
    const nextRunIds = { ...prev.runIds }
    delete nextRunIds[escenario]
    return { ...prev, runIds: nextRunIds }
  }), [])
  const value = useMemo(() => ({ ...estado, setParametros, setArchivo, setArchivoBloqueos, setRunId, clearRunId }), [estado, setParametros, setArchivo, setArchivoBloqueos, setRunId, clearRunId])
  return <SimulationContext.Provider value={value}>{children}</SimulationContext.Provider>
}
