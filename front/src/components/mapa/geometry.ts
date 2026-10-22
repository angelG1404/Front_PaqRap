export type Point = [number, number]
export interface Viewport { width: number; height: number; scale: number; offsetX: number; offsetY: number }
const EPS = 1e-3

export function project(point: Point, view: Viewport): Point {
  return [view.offsetX + point[0] * view.scale, view.offsetY - point[1] * view.scale]
}
export function unproject(point: Point, view: Viewport): Point {
  return [(point[0] - view.offsetX) / view.scale, (view.offsetY - point[1]) / view.scale]
}
function segmentLength(a: Point, b: Point): number {
  return Math.abs(b[0] - a[0]) + Math.abs(b[1] - a[1])
}
export function pathLength(path: Point[]): number {
  let total = 0
  for (let i = 1; i < path.length; i++) total += segmentLength(path[i - 1], path[i])
  return total
}
/** Distancia Manhattan a lo largo de una ruta, sin cortar las esquinas. */
export function pointOnPath(path: Point[], progress: number): Point {
  if (!path.length) return [0, 0]
  let remaining = pathLength(path) * Math.max(0, Math.min(1, progress))
  for (let i = 1; i < path.length; i++) {
    const from = path[i - 1], to = path[i], length = segmentLength(from, to)
    if (remaining <= length && length > 0) {
      // Aun si una fuente manda dos puntos diagonales, el trayecto es ortogonal.
      const dx = to[0] - from[0], dy = to[1] - from[1]
      return remaining <= Math.abs(dx) ? [from[0] + Math.sign(dx) * remaining, from[1]] : [to[0], from[1] + Math.sign(dy) * (remaining - Math.abs(dx))]
    }
    remaining -= length
  }
  return path[path.length - 1]
}
/** Parte de la ruta que falta recorrer desde el avance indicado (el primer punto es la posición actual). */
export function remainingPath(path: Point[], progress: number): Point[] {
  if (path.length < 2) return path.slice()
  const current = pointOnPath(path, progress)
  let remaining = pathLength(path) * Math.max(0, Math.min(1, progress))
  for (let i = 1; i < path.length; i++) {
    const length = segmentLength(path[i - 1], path[i])
    if (remaining < length - EPS) return [current, ...path.slice(i)]
    remaining -= length
  }
  return [current]
}
function onSegment(p: Point, a: Point, b: Point): boolean {
  if (Math.abs(a[0] - b[0]) < EPS && Math.abs(p[0] - a[0]) < EPS) return p[1] >= Math.min(a[1], b[1]) - EPS && p[1] <= Math.max(a[1], b[1]) + EPS
  if (Math.abs(a[1] - b[1]) < EPS && Math.abs(p[1] - a[1]) < EPS) return p[0] >= Math.min(a[0], b[0]) - EPS && p[0] <= Math.max(a[0], b[0]) + EPS
  return false
}
/**
 * Trayecto entre dos snapshots: sigue la ruta que el vehículo tenía anunciada hasta la nueva posición.
 * Si la nueva posición no está sobre esa ruta (re-ruteo o salida nueva), usa una L ortogonal; si el
 * salto es enorme (remolque), aparece directamente en destino.
 */
export function movementPath(from: Point, to: Point, previousRoute: Point[]): Point[] {
  if (Math.abs(from[0] - to[0]) < EPS && Math.abs(from[1] - to[1]) < EPS) return [to]
  for (let i = 0; i < previousRoute.length - 1; i++) {
    if (onSegment(to, previousRoute[i], previousRoute[i + 1])) return [from, ...previousRoute.slice(1, i + 1), to]
  }
  if (segmentLength(from, to) > 15) return [to]
  return [from, [to[0], from[1]], to]
}
/** Distancia en píxeles de un punto a un segmento (para el hover de bloqueos). */
export function distanceToSegment(p: Point, a: Point, b: Point): number {
  const dx = b[0] - a[0], dy = b[1] - a[1]
  const length = dx * dx + dy * dy
  const t = length ? Math.max(0, Math.min(1, ((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / length)) : 0
  return Math.hypot(p[0] - (a[0] + t * dx), p[1] - (a[1] + t * dy))
}
