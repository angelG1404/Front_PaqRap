import type { Snapshot } from '../types/snapshot'

/** Muestra qué algoritmo planifica la corrida y cuánto tardó su última ejecución. */
export function PlanificacionPill({ snapshot }: { snapshot: Snapshot | null }) {
  const plan = snapshot?.planificacion
  if (!plan) return null
  const detalle = plan.enCurso ? 'planificando…' : plan.ejecuciones ? `${plan.ejecuciones} planes · último ${plan.duracionMs ?? 0} ms` : 'esperando pedidos'
  return <span className={`plan-pill ${plan.enCurso ? 'busy' : ''}`} title="Planificador por lotes: replanifica pedidos pendientes, averías y bloqueos"><i />{plan.algoritmo} · {detalle}</span>
}
