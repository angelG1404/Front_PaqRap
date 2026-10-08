export type Point = [number, number]
export interface Viewport { width: number; height: number; scale: number; offsetX: number; offsetY: number }
export function project(point: Point, view: Viewport): Point {
  return [view.offsetX + point[0] * view.scale, view.offsetY - point[1] * view.scale]
}
export function unproject(point: Point, view: Viewport): Point {
  return [(point[0] - view.offsetX) / view.scale, (view.offsetY - point[1]) / view.scale]
}
/** Distancia Manhattan a lo largo de una ruta, sin cortar las esquinas. */
export function pointOnPath(path: Point[], progress: number): Point {
  if (!path.length) return [0, 0]
  const lengths = path.slice(1).map((p, i) => Math.abs(p[0] - path[i][0]) + Math.abs(p[1] - path[i][1]))
  let remaining = lengths.reduce((a, b) => a + b, 0) * Math.max(0, Math.min(1, progress))
  for (let i = 0; i < lengths.length; i++) {
    if (remaining <= lengths[i] && lengths[i] > 0) {
      const from = path[i], to = path[i + 1]
      // Aun si una fuente manda dos puntos diagonales, el trayecto es ortogonal.
      const dx = to[0] - from[0], dy = to[1] - from[1]
      return remaining <= Math.abs(dx) ? [from[0] + Math.sign(dx) * remaining, from[1]] : [to[0], from[1] + Math.sign(dy) * (remaining - Math.abs(dx))]
    }
    remaining -= lengths[i]
  }
  return path[path.length - 1]
}
export function movementPath(from: Point, to: Point, previousRoute: Point[]): Point[] {
  const end = previousRoute.findIndex((p) => p[0] === to[0] && p[1] === to[1])
  const segment = previousRoute.findIndex((p, i) => {
    const next = previousRoute[i + 1]
    if (!next) return false
    return p[0] === next[0] && from[0] === p[0] && from[1] >= Math.min(p[1], next[1]) && from[1] <= Math.max(p[1], next[1])
      || p[1] === next[1] && from[1] === p[1] && from[0] >= Math.min(p[0], next[0]) && from[0] <= Math.max(p[0], next[0])
  })
  return end >= 0 && segment >= 0 && segment < end ? [from, ...previousRoute.slice(segment + 1, end + 1)] : [from, [to[0], from[1]], to]
}
