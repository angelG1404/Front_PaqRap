// Genera pedidos_colapso.txt: demanda que crece cada día hasta superar la capacidad de la flota.
// Uso: node data/generar_colapso.mjs [dias] [pedidosDia1] [crecimiento]
// Determinista (semilla fija): el mismo comando produce siempre el mismo archivo.
import { writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const [dias = 6, pedidosDia1 = 100, crecimiento = 1.7] = process.argv.slice(2).map(Number)
let semilla = 20262
const azar = () => { semilla = (semilla * 1103515245 + 12345) % 2147483648; return semilla / 2147483648 }
const entero = (min, max) => min + Math.floor(azar() * (max - min + 1))
const dos = (n) => String(n).padStart(2, '0')
const horizontes = [4, 8, 12, 18, 36, 36]

const lineas = []
for (let dia = 1; dia <= dias; dia++) {
  const cantidad = Math.round(pedidosDia1 * crecimiento ** (dia - 1))
  const minutos = Array.from({ length: cantidad }, () => entero(0, 1439)).sort((a, b) => a - b)
  for (const m of minutos) {
    const cliente = `c${entero(1000, 9999)}`
    lineas.push(`${dos(dia)}d${dos(Math.floor(m / 60))}h${dos(m % 60)}m:${entero(0, 70)},${entero(0, 50)},${cliente},${dos(entero(1, 10))},${dos(horizontes[entero(0, horizontes.length - 1)])}`)
  }
}
const destino = join(dirname(fileURLToPath(import.meta.url)), 'pedidos_colapso.txt')
writeFileSync(destino, lineas.join('\n') + '\n')
console.log(`${lineas.length} pedidos en ${dias} días -> ${destino}`)
