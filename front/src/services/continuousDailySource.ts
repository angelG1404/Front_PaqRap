import type { SnapshotSource } from './snapshotSource'

/** Ignora las señales de cierre de Diario y sigue aceptando posiciones nuevas. */
export function continuousDailySource(source: SnapshotSource): SnapshotSource {
  return {
    start(onSnapshot) {
      source.start((snapshot) => onSnapshot({ ...snapshot, escenario: 'DIARIO', estado: 'EN_CURSO', causa: undefined, resumen: undefined }))
    },
    stop() { source.stop() },
  }
}
