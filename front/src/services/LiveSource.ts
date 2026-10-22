import { Client } from '@stomp/stompjs'
import type { Snapshot, EstadoConexion } from '../types/snapshot'
import type { SnapshotSource } from './snapshotSource'
import { API_URL, getEstado } from './api'

const SONDEO_MS = 2500

/** Endpoint STOMP: VITE_WS_URL, o el del backend de VITE_API_URL, o el mismo origen de la página. */
export function wsUrl(): string {
  const explicita = import.meta.env.VITE_WS_URL
  if (explicita) return explicita
  if (API_URL) return `${API_URL.replace(/^http/, 'ws')}/ws`
  return `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`
}

/**
 * Suscripción a /topic/sim/{runId}. Pide primero el último snapshot por REST (así una corrida ya
 * terminada se ve al instante) y, si el WebSocket cae, sondea GET /estado hasta reconectar.
 */
export class LiveSource implements SnapshotSource {
  private client: Client | null = null
  private sondeo: ReturnType<typeof setInterval> | null = null
  private activo = false
  private readonly runId: string

  constructor(runIdOrTopic: string) {
    this.runId = runIdOrTopic.replace(/^\/topic\/sim\//, '')
  }

  start(onSnapshot: (snapshot: Snapshot) => void, onEstado?: (estado: EstadoConexion) => void): void {
    this.activo = true
    onEstado?.('CONECTANDO')
    const emitir = (snapshot: Snapshot) => { if (this.activo && snapshot.runId === this.runId) onSnapshot(snapshot) }
    const consultar = () => getEstado(this.runId).then(emitir).catch(() => undefined)
    void consultar()

    this.client = new Client({
      brokerURL: wsUrl(),
      reconnectDelay: 3000,
      debug: () => {},
      onConnect: () => {
        this.detenerSondeo()
        onEstado?.('CONECTADO')
        this.client?.subscribe(`/topic/sim/${this.runId}`, (message) => {
          try {
            emitir(JSON.parse(message.body) as Snapshot)
          } catch (e) {
            console.error('Snapshot WebSocket inválido', e)
          }
        })
      },
      onStompError: () => onEstado?.('ERROR'),
      onWebSocketClose: () => {
        if (!this.activo) return
        onEstado?.('DESCONECTADO')
        this.iniciarSondeo(consultar)
      },
      onWebSocketError: () => {
        if (!this.activo) return
        onEstado?.('ERROR')
        this.iniciarSondeo(consultar)
      },
    })
    this.client.activate()
  }

  private iniciarSondeo(consultar: () => void): void {
    if (this.sondeo === null) this.sondeo = setInterval(consultar, SONDEO_MS)
  }

  private detenerSondeo(): void {
    if (this.sondeo !== null) clearInterval(this.sondeo)
    this.sondeo = null
  }

  stop(): void {
    this.activo = false
    this.detenerSondeo()
    if (this.client) {
      void this.client.deactivate()
      this.client = null
    }
  }
}
