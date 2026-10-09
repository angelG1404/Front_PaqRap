import { MockSource } from '../mock/MockSource'
import { LiveSource } from './LiveSource'
import { continuousDailySource } from './continuousDailySource'
import { dailySource } from '../mock/dailySource'
import type { SnapshotSource } from './snapshotSource'
import type { Snapshot, EstadoConexion } from '../types/snapshot'
import type { ParametrosCorrida } from '../types'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

export function createSource(
  escenario: Snapshot['escenario'],
  runIdOrTopic?: string,
  options?: Pick<ParametrosCorrida, 'fechaInicio' | 'umbralesSemaforo'>,
  onConnectionChange?: (estado: EstadoConexion) => void
): SnapshotSource {
  if (USE_MOCK) {
    if (escenario === 'DIARIO') {
      return continuousDailySource(dailySource)
    }
    return new MockSource(escenario, options)
  }
  const target = runIdOrTopic || (escenario === 'DIARIO' ? 'diario' : 'default')
  return new LiveSource(target, onConnectionChange)
}
