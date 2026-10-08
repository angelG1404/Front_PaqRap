import { useMemo } from 'react'
import { MapaEnVivo } from '../components/mapa/MapaEnVivo'
import { useSnapshots } from '../hooks/useSnapshots'
import { MockSource } from '../mock/MockSource'
import { MapPageHeader } from './MapPageHeader'

export function DailyOperationPage() {
  const source = useMemo(() => new MockSource('DIARIO'), [])
  const { snapshot, estadoConexion } = useSnapshots(source)
  return <div className="map-page"><MapPageHeader snapshot={snapshot} escenario="DIARIO" /><MapaEnVivo snapshot={snapshot} estadoConexion={estadoConexion} escenario="DIARIO" /></div>
}
