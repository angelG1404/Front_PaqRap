import { useMemo } from 'react'
import { MapaEnVivo } from '../components/mapa/MapaEnVivo'
import { useSimulation } from '../context/SimulationContext'
import { useSnapshots } from '../hooks/useSnapshots'
import { MockSource } from '../mock/MockSource'
import { MapPageHeader } from './MapPageHeader'

export function SimulationMapPage() {
  const { parametros } = useSimulation()
  const source = useMemo(() => new MockSource(parametros.escenario, parametros), [parametros])
  const { snapshot, estadoConexion } = useSnapshots(source)
  return <div className="map-page"><MapPageHeader snapshot={snapshot} escenario={parametros.escenario} />
    {snapshot?.estado === 'COLAPSADO' && <div className="run-alert" role="alert">⚠ Simulación detenida por Colapso Logístico — {snapshot.causa}</div>}
    {snapshot?.estado === 'FINALIZADO' && <div className="run-complete" role="status">✓ Corrida finalizada. {snapshot.resumen?.entregados} de {snapshot.resumen?.pedidosTotal} pedidos entregados.</div>}
    <MapaEnVivo snapshot={snapshot} estadoConexion={estadoConexion} escenario={parametros.escenario} />
  </div>
}
