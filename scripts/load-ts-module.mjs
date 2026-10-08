import fs from 'node:fs'
import path from 'node:path'
import ts from 'typescript'

const cache = new Map()
/** Carga módulos TS puros y sustituye import.meta.env sin añadir dependencias. */
export function moduleUrl(file, mock = true) {
  const absolute = path.resolve(file)
  const key = `${absolute}:${mock}`
  if (cache.has(key)) return cache.get(key)
  const code = ts.transpileModule(fs.readFileSync(absolute, 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2023 },
  }).outputText
    .replace(/from ['"](\.[^'"]+)['"]/g, (_, specifier) => `from '${moduleUrl(path.resolve(path.dirname(absolute), specifier + '.ts'), mock)}'`)
    .replaceAll('import.meta.env.VITE_USE_MOCK', JSON.stringify(String(mock)))
    .replaceAll('import.meta.env.VITE_API_URL', JSON.stringify('https://paqrap.example/'))
  const url = 'data:text/javascript;base64,' + Buffer.from(code).toString('base64')
  cache.set(key, url)
  return url
}
