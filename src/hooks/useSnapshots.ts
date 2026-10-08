import { useEffect, useState } from 'react'
import type { SnapshotSource } from '../services/snapshotSource'
import type { EstadoConexion, Snapshot } from '../types/snapshot'
export function useSnapshots(source: SnapshotSource) {
  const [state, setState] = useState<{ snapshot: Snapshot | null; estadoConexion: EstadoConexion }>({ snapshot: null, estadoConexion: 'CONECTANDO' })
  useEffect(() => {
    let active = true
    try {
      source.start((snapshot) => { if (active) setState({ snapshot, estadoConexion: 'CONECTADO' }) })
    } catch {
      queueMicrotask(() => { if (active) setState({ snapshot: null, estadoConexion: 'ERROR' }) })
    }
    return () => { active = false; source.stop() }
  }, [source])
  return state
}
