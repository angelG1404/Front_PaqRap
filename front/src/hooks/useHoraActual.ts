import { useEffect, useState } from 'react'
import { localIso } from '../services/simulationTime'

/** Fecha y hora del dispositivo (ISO local), refrescada cada segundo para que el minuto cambie a tiempo. */
export function useHoraActual(): string {
  const [ahora, setAhora] = useState(() => localIso(new Date()))
  useEffect(() => {
    const timer = window.setInterval(() => setAhora(localIso(new Date())), 1000)
    return () => window.clearInterval(timer)
  }, [])
  return ahora
}
