import type { EstadoPedido } from './snapshot'

export interface NuevoPedido {
  x: number
  y: number
  clienteId: string
  cantidad: number
  horizonteHoras: number
}

/** Fechas ISO del endpoint de registro; el snapshot conserva su contrato DDdHHhMMm. */
export interface PedidoRegistrado {
  id: string
  clienteId: string
  x: number
  y: number
  cantidad: number
  registradoEn: string
  deadline: string
  estado: EstadoPedido
}
