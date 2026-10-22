import type { EstadoConexion, Snapshot } from '../types/snapshot'
export interface SnapshotSource {
  start(onSnapshot: (s: Snapshot) => void, onEstado?: (estado: EstadoConexion) => void): void
  stop(): void
}
