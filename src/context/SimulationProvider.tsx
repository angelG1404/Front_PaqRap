import { useState, type ReactNode } from 'react'
import { crearParametrosIniciales } from '../config/defaults'
import type { EstadoSimulacion } from '../types'
import { SimulationContext } from './SimulationContext'
export function SimulationProvider({ children }: { children: ReactNode }) {
  const [estado, setEstado] = useState<EstadoSimulacion>(() => ({
    parametros: crearParametrosIniciales(), archivoPedidos: null, infoArchivoPedidos: null,
  }))
  return <SimulationContext.Provider value={{ ...estado,
    setParametros: (parametros) => setEstado((prev) => ({ ...prev, parametros })),
    setArchivo: (archivoPedidos, infoArchivoPedidos) => setEstado((prev) => ({ ...prev, archivoPedidos, infoArchivoPedidos })),
  }}>{children}</SimulationContext.Provider>
}
