import { MockSource } from '../mock/MockSource'
import { LiveSource } from './LiveSource'
import { continuousDailySource } from './continuousDailySource'
import { dailySource } from '../mock/dailySource'
import { USE_MOCK } from './api'
import type { SnapshotSource } from './snapshotSource'
import type { Snapshot } from '../types/snapshot'
import type { ParametrosCorrida } from '../types'

export function createSource(
  escenario: Snapshot['escenario'],
  runIdOrTopic: string,
  options?: Pick<ParametrosCorrida, 'fechaInicio' | 'umbralesSemaforo'>,
): SnapshotSource {
  if (USE_MOCK) {
    if (escenario === 'DIARIO') return continuousDailySource(dailySource)
    return new MockSource(escenario, options)
  }
  return new LiveSource(runIdOrTopic)
}
