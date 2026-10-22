import { EXTENSIONES_PEDIDOS, FORMATO_PEDIDO, GRID_ALTO, GRID_ANCHO, HORIZONTES_VALIDOS } from '../config/defaults'
import type { ArchivoPedidosInfo } from '../types'
export function validarPedidos(texto: string, file: Pick<File, 'name' | 'size'>): ArchivoPedidosInfo {
  const errores: ArchivoPedidosInfo['errores'] = []
  let totalPedidos = 0
  if (!EXTENSIONES_PEDIDOS.some((ext) => file.name.toLowerCase().endsWith(ext))) errores.push({ linea: 0, mensaje: 'Selecciona un archivo .txt o .csv.' })
  texto.replace(/^\uFEFF/, '').split(/\r\n|\n|\r/).forEach((linea, index) => {
    if (!linea.trim()) return
    let mensaje: string
    if (!FORMATO_PEDIDO.test(linea)) mensaje = 'Formato inválido. Usa DDdHHhMMm:posX,posY,idCliente,cantidad,horizonteHoras.'
    else {
      const [x, y, , cantidad, horizonte] = linea.split(':')[1].split(',')
      const motivos = []
      if (Number(x) < 0 || Number(x) > GRID_ANCHO) motivos.push(`posX debe estar entre 0 y ${GRID_ANCHO}`)
      if (Number(y) < 0 || Number(y) > GRID_ALTO) motivos.push(`posY debe estar entre 0 y ${GRID_ALTO}`)
      if (!Number.isSafeInteger(Number(cantidad)) || Number(cantidad) <= 0) motivos.push('cantidad debe ser un entero mayor que 0')
      if (!HORIZONTES_VALIDOS.some((valor) => valor === Number(horizonte))) motivos.push(`horizonte debe ser ${HORIZONTES_VALIDOS.join(', ')} horas`)
      mensaje = motivos.join('; ')
    }
    if (mensaje) errores.push({ linea: index + 1, mensaje })
    else totalPedidos++
  })
  if (!totalPedidos && !errores.length) errores.push({ linea: 0, mensaje: 'El archivo no contiene pedidos.' })
  return { nombre: file.name, tamanoBytes: file.size, totalPedidos, errores, valido: errores.length === 0 && totalPedidos > 0 }
}
const FORMATO_BLOQUEO = /^\d{2}d\d{2}h\d{2}m-\d{2}d\d{2}h\d{2}m:\d+(,\d+)+$/
/** Validación rápida del archivo de bloqueos; la oficial la hace el backend al subirlo. */
export function validarBloqueos(texto: string, file: Pick<File, 'name' | 'size'>): ArchivoPedidosInfo {
  const errores: ArchivoPedidosInfo['errores'] = []
  let total = 0
  if (!EXTENSIONES_PEDIDOS.some((ext) => file.name.toLowerCase().endsWith(ext))) errores.push({ linea: 0, mensaje: 'Selecciona un archivo .txt o .csv.' })
  texto.replace(/^\uFEFF/, '').split(/\r\n|\n|\r/).forEach((linea, index) => {
    if (!linea.trim()) return
    const motivos: string[] = []
    if (!FORMATO_BLOQUEO.test(linea.trim())) motivos.push('Formato inválido. Usa DDdHHhMMm-DDdHHhMMm:x1,y1,x2,y2,...')
    else {
      const coords = linea.trim().split(':')[1].split(',').map(Number)
      if (coords.length < 4 || coords.length % 2) motivos.push('se requieren al menos dos puntos (x,y)')
      for (let i = 0; i + 1 < coords.length; i += 2) {
        if (coords[i] > GRID_ANCHO || coords[i + 1] > GRID_ALTO) { motivos.push('coordenadas fuera de la grilla'); break }
        if (i >= 2 && coords[i] !== coords[i - 2] && coords[i + 1] !== coords[i - 1]) { motivos.push('los tramos deben ser horizontales o verticales'); break }
      }
    }
    if (motivos.length) errores.push({ linea: index + 1, mensaje: motivos.join('; ') })
    else total++
  })
  if (!total && !errores.length) errores.push({ linea: 0, mensaje: 'El archivo no contiene bloqueos.' })
  return { nombre: file.name, tamanoBytes: file.size, totalPedidos: total, errores, valido: errores.length === 0 && total > 0 }
}
export function leerArchivo(file: File, validar: (texto: string, file: File) => ArchivoPedidosInfo): Promise<ArchivoPedidosInfo> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(validar(String(reader.result), file))
    reader.onerror = () => reject(new Error('No se pudo leer el archivo. Intenta seleccionarlo otra vez.'))
    reader.onabort = () => reject(new Error('La lectura del archivo fue cancelada.'))
    reader.readAsText(file)
  })
}
export function leerArchivoPedidos(file: File): Promise<ArchivoPedidosInfo> {
  return leerArchivo(file, validarPedidos)
}
