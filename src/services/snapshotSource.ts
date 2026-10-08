import type { Snapshot } from '../types/snapshot'
export interface SnapshotSource {
  start(onSnapshot: (s: Snapshot) => void): void
  stop(): void
}
