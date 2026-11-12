import { useEffect, useState } from 'react'
import { USE_MOCK, getActividadAlmacen } from '../services/api'
import type { ActividadAlmacen } from '../types/snapshot'

const SONDEO_MS = 3000

/**
 * Actividad del almacén seleccionado en las últimas horas simuladas. Se consulta al elegirlo y se refresca cada
 * pocos segundos, porque los envíos y entregas cambian con el reloj de la corrida.
 */
export function useActividadAlmacen(runId: string | undefined, almacenId: string | null) {
  const [estado, setEstado] = useState<{ clave: string; datos: ActividadAlmacen | null; error: string | null }>({ clave: '', datos: null, error: null })
  const clave = runId && almacenId ? `${runId}/${almacenId}` : ''
  useEffect(() => {
    if (!clave || !runId || !almacenId) return
    if (USE_MOCK) return
    let activo = true
    const consultar = () => getActividadAlmacen(runId, almacenId)
      .then((datos) => { if (activo) setEstado({ clave, datos, error: null }) })
      .catch((error: unknown) => { if (activo) setEstado((previo) => ({ clave, datos: previo.clave === clave ? previo.datos : null, error: error instanceof Error ? error.message : 'No se pudo consultar el almacén.' })) })
    void consultar()
    const timer = window.setInterval(consultar, SONDEO_MS)
    return () => { activo = false; window.clearInterval(timer) }
  }, [clave, runId, almacenId])
  if (USE_MOCK) return { datos: null, error: 'La actividad por almacén requiere el backend real.', cargando: false }
  const vigente = estado.clave === clave
  return { datos: vigente ? estado.datos : null, error: vigente ? estado.error : null, cargando: !!clave && !(vigente && (estado.datos || estado.error)) }
}
