import { GRID_ALTO, GRID_ANCHO, INICIOS_DE_TURNO } from '../config/defaults'
import { NOMBRES_TIPO } from '../config/theme'
import type { ConfigAlmacen, ConfigAverias, ConfigVehiculo } from '../types'

/** Entrada numérica que admite vaciarse mientras se escribe (NaN) y avisa del valor como número. */
function Numero({ etiqueta, valor, onChange, min, max, step = 1 }: { etiqueta: string; valor: number; onChange: (v: number) => void; min: number; max: number; step?: number }) {
  return <input type="number" aria-label={etiqueta} min={min} max={max} step={step} required value={Number.isNaN(valor) ? '' : valor} onChange={(e) => onChange(e.target.valueAsNumber)} />
}

/**
 * Reglas de los tres tipos de avería. El operador solo elige unidad y tipo al registrar una avería; el sistema aplica
 * estas reglas para fijar su fin y su remolque. Un cambio rige para las averías que se registren desde ese momento.
 */
export function AveriasEditor({ valor, onChange }: { valor: ConfigAverias; onChange: (v: ConfigAverias) => void }) {
  const { tipo1, tipo2, tipo3 } = valor
  return <div className="cfg-averias">
    <fieldset><legend>Tipo 1 · menor</legend>
      <label>Duración fuera de servicio (h)<Numero etiqueta="Tipo 1: duración en horas" valor={tipo1.duracionHoras} min={0.25} max={72} step={0.25} onChange={(v) => onChange({ ...valor, tipo1: { duracionHoras: v } })} /></label>
      <p>Fin = ocurrencia + duración. La unidad espera en el lugar.</p></fieldset>
    <fieldset><legend>Tipo 2 · intermedia</legend>
      <label>Turnos de espera<select aria-label="Tipo 2: turnos de espera" value={tipo2.turnosEspera} onChange={(e) => onChange({ ...valor, tipo2: { ...tipo2, turnosEspera: Number(e.target.value) } })}>{[1, 2, 3].map((n) => <option key={n} value={n}>{n === 1 ? 'Hasta el fin del turno siguiente' : `Hasta el fin del ${n}.º turno siguiente`}</option>)}</select></label>
      <label>Remolque al central a las (h)<Numero etiqueta="Tipo 2: horas hasta el remolque" valor={tipo2.horasRemolque} min={0} max={48} step={0.5} onChange={(v) => onChange({ ...valor, tipo2: { ...tipo2, horasRemolque: v } })} /></label>
      <p>Si el remolque cae después del fin, la unidad no se remolca.</p></fieldset>
    <fieldset><legend>Tipo 3 · mayor</legend>
      <label>Mínimo fuera de servicio (h)<Numero etiqueta="Tipo 3: horas mínimas" valor={tipo3.horasMinimas} min={1} max={240} onChange={(v) => onChange({ ...valor, tipo3: { ...tipo3, horasMinimas: v } })} /></label>
      <label>Regresa en el turno que inicia a las<select aria-label="Tipo 3: turno de regreso" value={tipo3.horaRetorno} onChange={(e) => onChange({ ...valor, tipo3: { ...tipo3, horaRetorno: Number(e.target.value) } })}>{INICIOS_DE_TURNO.map((h) => <option key={h} value={h}>{String(h).padStart(2, '0')}:00</option>)}</select></label>
      <label>Remolque al central a las (h)<Numero etiqueta="Tipo 3: horas hasta el remolque" valor={tipo3.horasRemolque} min={0} max={48} step={0.5} onChange={(v) => onChange({ ...valor, tipo3: { ...tipo3, horasRemolque: v } })} /></label></fieldset>
  </div>
}

/** Ubicación (X, Y) y capacidad de cada almacén; el central tiene inventario infinito. */
export function AlmacenesEditor({ valor, onChange }: { valor: ConfigAlmacen[]; onChange: (v: ConfigAlmacen[]) => void }) {
  const cambiar = (id: string, parche: Partial<ConfigAlmacen>) => onChange(valor.map((a) => a.id === id ? { ...a, ...parche } : a))
  return <div className="table-scroll"><table className="warehouse-table cfg-table"><thead><tr><th>ALMACÉN</th><th>X (0–{GRID_ANCHO})</th><th>Y (0–{GRID_ALTO})</th><th>CAPACIDAD</th></tr></thead><tbody>
    {valor.map((a) => <tr key={a.id}><td><strong>{a.nombre}</strong></td>
      <td><Numero etiqueta={`${a.nombre}: X`} valor={a.x} min={0} max={GRID_ANCHO} onChange={(v) => cambiar(a.id, { x: v })} /></td>
      <td><Numero etiqueta={`${a.nombre}: Y`} valor={a.y} min={0} max={GRID_ALTO} onChange={(v) => cambiar(a.id, { y: v })} /></td>
      <td>{a.capacidad === null ? '∞' : <Numero etiqueta={`${a.nombre}: capacidad`} valor={a.capacidad} min={1} max={1_000_000} onChange={(v) => cambiar(a.id, { capacidad: v })} />}</td></tr>)}
  </tbody></table></div>
}

/** Capacidad (paquetes) de cada tipo de unidad; velocidad y costo por km son del enunciado y no se editan. */
export function CapacidadesEditor({ valor, onChange }: { valor: ConfigVehiculo[]; onChange: (v: ConfigVehiculo[]) => void }) {
  return <div className="table-scroll"><table className="fleet-table cfg-table"><thead><tr><th>TIPO</th><th>CAPACIDAD (paq.)</th><th>VEL.</th><th>COSTO/KM</th></tr></thead><tbody>
    {valor.map((v) => <tr key={v.tipo}><td>{NOMBRES_TIPO[v.tipo.toUpperCase() as keyof typeof NOMBRES_TIPO]}</td>
      <td><Numero etiqueta={`Capacidad de ${v.tipo}`} valor={v.capacidad} min={1} max={1000} onChange={(c) => onChange(valor.map((x) => x.tipo === v.tipo ? { ...x, capacidad: c } : x))} /></td>
      <td>{v.velocidadKmH ?? '—'} km/h</td><td>S/ {(v.costoPorKm ?? 0).toFixed(2)}</td></tr>)}
  </tbody></table></div>
}
