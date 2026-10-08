import { MockSource } from './MockSource'

/** Compartida por la página y la API mock; nunca crear otra fuente al registrar. */
export const dailySource = new MockSource('DIARIO')
