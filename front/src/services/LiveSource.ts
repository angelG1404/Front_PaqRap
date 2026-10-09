import { Client } from '@stomp/stompjs'
import type { Snapshot, EstadoConexion } from '../types/snapshot'
import type { SnapshotSource } from './snapshotSource'

export class LiveSource implements SnapshotSource {
  private client: Client | null = null
  private runIdOrTopic: string
  private onConnectionChange?: (estado: EstadoConexion) => void

  constructor(runIdOrTopic: string, onConnectionChange?: (estado: EstadoConexion) => void) {
    this.runIdOrTopic = runIdOrTopic
    this.onConnectionChange = onConnectionChange
  }

  start(onSnapshot: (snapshot: Snapshot) => void): void {
    const wsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws'
    this.onConnectionChange?.('CONECTANDO')

    const topic = this.runIdOrTopic.startsWith('/') ? this.runIdOrTopic : `/topic/sim/${this.runIdOrTopic}`

    this.client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 3000,
      debug: () => {},
      onConnect: () => {
        this.onConnectionChange?.('CONECTADO')
        this.client?.subscribe(topic, (message) => {
          try {
            const snapshot: Snapshot = JSON.parse(message.body)
            onSnapshot(snapshot)
          } catch (e) {
            console.error('Error parsing WS snapshot message', e)
          }
        })
      },
      onStompError: () => {
        this.onConnectionChange?.('ERROR')
      },
      onWebSocketClose: () => {
        this.onConnectionChange?.('DESCONECTADO')
      },
      onWebSocketError: () => {
        this.onConnectionError()
      }
    })

    this.client.activate()
  }

  private onConnectionError(): void {
    this.onConnectionChange?.('ERROR')
  }

  stop(): void {
    if (this.client) {
      this.client.deactivate()
      this.client = null
    }
    this.onConnectionChange?.('DESCONECTADO')
  }
}
