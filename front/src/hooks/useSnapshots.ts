import { useEffect, useState } from 'react'
import type { SnapshotSource } from '../services/snapshotSource'
import type { EstadoConexion, Snapshot } from '../types/snapshot'

interface Estado { source: SnapshotSource | null; snapshot: Snapshot | null; estadoConexion: EstadoConexion }

export function useSnapshots(source: SnapshotSource | null) {
  const [state, setState] = useState<Estado>({ source: null, snapshot: null, estadoConexion: 'CONECTANDO' })
  useEffect(() => {
    if (!source) return
    let active = true
    try {
      source.start(
        (snapshot) => { if (active) setState((prev) => ({ source, snapshot, estadoConexion: prev.source === source && prev.estadoConexion !== 'CONECTANDO' ? prev.estadoConexion : 'CONECTADO' })) },
        (estadoConexion) => { if (active) setState((prev) => ({ source, snapshot: prev.source === source ? prev.snapshot : null, estadoConexion })) },
      )
    } catch {
      queueMicrotask(() => { if (active) setState({ source, snapshot: null, estadoConexion: 'ERROR' }) })
    }
    return () => { active = false; source.stop() }
  }, [source])
  // Al cambiar de fuente, lo de la anterior ya no aplica.
  return state.source === source ? { snapshot: state.snapshot, estadoConexion: state.estadoConexion } : { snapshot: null, estadoConexion: 'CONECTANDO' as EstadoConexion }
}
