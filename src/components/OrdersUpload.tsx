import { useEffect, useRef, useState } from 'react'
import { EXTENSIONES_PEDIDOS, MAX_ERRORES_VISIBLES } from '../config/defaults'
import { useSimulation } from '../context/SimulationContext'
import { leerArchivoPedidos } from '../services/validarPedidos'
import { Card } from './Card'
import { Icon } from './Icon'
export function OrdersUpload() {
  const { archivoPedidos, infoArchivoPedidos: info, setArchivo } = useSimulation()
  const input = useRef<HTMLInputElement>(null)
  const request = useRef(0)
  const [reading, setReading] = useState(false)
  const [dragging, setDragging] = useState(false)
  useEffect(() => () => { request.current++ }, [])
  async function select(file?: File) {
    if (!file) return
    const id = ++request.current
    setReading(true)
    setArchivo(file, null)
    try {
      const result = await leerArchivoPedidos(file)
      if (id === request.current) setArchivo(file, result)
    } catch (error) {
      if (id === request.current) setArchivo(file, { nombre: file.name, tamanoBytes: file.size, totalPedidos: 0, valido: false, errores: [{ linea: 0, mensaje: error instanceof Error ? error.message : 'No se pudo leer el archivo.' }] })
    } finally { if (id === request.current) setReading(false) }
  }
  function remove() { request.current++; setReading(false); setArchivo(null, null); if (input.current) input.current.value = '' }
  return <Card title="Archivos de Entrada" subtitle="Carga los pedidos en formato de texto (.txt o .csv)" icon="file" badge={info?.valido ? '● Obligatorio listo' : 'Obligatorio'}>
    <input ref={input} type="file" className="sr-only" tabIndex={-1} aria-label="Archivo de pedidos" accept={EXTENSIONES_PEDIDOS.join(',')} onChange={(event) => { void select(event.target.files?.[0]); event.target.value = '' }} />
    <div className={`dropzone ${dragging ? 'dragging' : ''}`} onDragOver={(event) => { event.preventDefault(); setDragging(true) }} onDragLeave={() => setDragging(false)} onDrop={(event) => { event.preventDefault(); setDragging(false); void select(event.dataTransfer.files[0]) }}>
      <span className="upload-icon"><Icon name="upload" /></span><h3>Archivo de pedidos <span className="required">REQUERIDO</span></h3>
      <p>Arrastra y suelta tu archivo aquí</p><span className="muted">o selecciónalo desde tu equipo</span>
      <button type="button" className="secondary-button" onClick={() => input.current?.click()}>{archivoPedidos ? 'Reemplazar archivo' : 'Seleccionar archivo'}</button>
    </div>
    <div aria-live="polite" aria-busy={reading}>
      {reading && <p className="mt-3 text-sm">Leyendo y validando {archivoPedidos?.name}…</p>}
      {info && <div className={`file-result ${info.valido ? 'success' : 'error'}`}><div className="flex items-center justify-between gap-3"><strong>{info.valido ? '✓ Validado' : 'Con errores'}</strong><button type="button" className="text-button" onClick={remove}>Quitar archivo</button></div><p className="break-all font-semibold mt-2">{info.nombre}</p><p>{info.totalPedidos} pedidos válidos · {new Intl.NumberFormat('es-PE').format(info.tamanoBytes)} bytes</p>
        {!!info.errores.length && <><p className="mt-2 font-semibold">{info.errores.length} errores en total</p><ul>{info.errores.slice(0, MAX_ERRORES_VISIBLES).map((error, i) => <li key={i}>{error.linea ? `Línea ${error.linea}: ` : ''}{error.mensaje}</li>)}</ul>{info.errores.length > MAX_ERRORES_VISIBLES && <p>Se muestran las primeras {MAX_ERRORES_VISIBLES} líneas inválidas.</p>}</>}
      </div>}
    </div>
    <p className="file-hint">Formato: DDdHHhMMm:posX,posY,idCliente,cantidad,horizonteHoras</p>
  </Card>
}
