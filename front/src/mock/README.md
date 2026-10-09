`MockSource` implementa `SnapshotSource` sin conexiones al backend. Acepta `5D`, `COLAPSO` o `DIARIO` en el constructor.

Emite cada segundo y mueve las unidades una celda ortogonal cada tres segundos, exclusivamente para esta demo. Simulación avanza 30 minutos por emisión; Diario usa tiempo real, empieza con seis pedidos y nunca emite un estado final. Los valores se encuentran en `src/config/defaults.ts`.

Las páginas consumen la fuente mediante `useSnapshots`; el canvas no depende del mock. El archivo de pedidos de la pantalla de parámetros aún no alimenta esta demo.

Validación reproducible: `npm run test:map`. Comprueba rutas, interpolación en esquinas, límites, semáforos, métricas, cierre de las simulaciones, continuidad de Diario y limpieza de temporizadores.

Operación Diaria comparte la instancia de `dailySource.ts` con `registrarPedido()` de `services/api.ts`. Con `VITE_USE_MOCK=true` (configurado en `.env`), el registro no usa red: publica el pedido pendiente inmediatamente y lo asigna a una unidad con capacidad disponible en los siguientes ticks. Una cantidad mayor que la capacidad de toda la flota permanece pendiente.

Las respuestas de registro contienen fechas ISO; los pedidos del snapshot conservan el formato `DDdHHhMMm` del contrato. El formulario conserva los últimos cinco registros mientras la página está abierta, incluso al cambiar de pestaña. La sesión mock se reinicia al volver a entrar a la página.

`npm run test:daily` comprueba registro, validación, deadlines, publicación/asignación, capacidad, continuidad y el contrato HTTP mediante un fetch simulado. Con `VITE_USE_MOCK=false`, solo el registro usa `POST /api/pedidos` contra el origen `VITE_API_URL`; la fuente real del mapa se integrará en el siguiente paso.
