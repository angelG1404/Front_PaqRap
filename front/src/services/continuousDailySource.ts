import type { SnapshotSource } from './snapshotSource'

/** Ignora las señales de cierre de Diario y sigue aceptando posiciones nuevas. */
export function continuousDailySource(source: SnapshotSource): SnapshotSource {
  return {
    start(onSnapshot, onEstado) {
      source.start((snapshot) => onSnapshot({ ...snapshot, escenario: 'DIARIO', estado: 'EN_CURSO', causa: undefined, resumen: undefined }), onEstado)
    },
    stop() { source.stop() },
  }
}
