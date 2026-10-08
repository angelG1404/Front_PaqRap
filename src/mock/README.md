`MockSource` implementa `SnapshotSource` sin conexiones al backend. Acepta `5D`, `COLAPSO` o `DIARIO` en el constructor.

Emite cada segundo y mueve las unidades una celda ortogonal cada tres segundos, exclusivamente para esta demo. Simulación avanza 30 minutos por emisión; Diario usa tiempo real, empieza con seis pedidos y nunca emite un estado final. Los valores se encuentran en `src/config/defaults.ts`.

Las páginas consumen la fuente mediante `useSnapshots`; el canvas no depende del mock. El archivo de pedidos de la pantalla de parámetros aún no alimenta esta demo.

Validación reproducible: `npm run test:map`. Comprueba rutas, interpolación en esquinas, límites, semáforos, métricas, cierre de las simulaciones, continuidad de Diario y limpieza de temporizadores.
