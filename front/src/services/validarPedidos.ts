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
export function leerArchivoPedidos(file: File): Promise<ArchivoPedidosInfo> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(validarPedidos(String(reader.result), file))
    reader.onerror = () => reject(new Error('No se pudo leer el archivo. Intenta seleccionarlo otra vez.'))
    reader.onabort = () => reject(new Error('La lectura del archivo fue cancelada.'))
    reader.readAsText(file)
  })
}
