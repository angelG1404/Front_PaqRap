# PaqRap — Guía para iniciar y probar la solución integrada

Esta guía explica cómo levantar el **Planificador** (back, Spring Boot) y el **Visualizador** (front, React), y
propone un guion de pruebas para los tres escenarios del caso: **Operación diaria**, **Simulación 5D** y
**Colapso logístico**. Sigue la arquitectura del entregable `sol.integrada.sem08` (REST para comandos,
WebSocket/STOMP para el mapa en vivo, un motor común para los tres escenarios).

---

## 1. Requisitos

| Herramienta | Versión | Comprobar |
|---|---|---|
| Java (JDK) | 17 o superior (probado con 21) | `java -version` |
| Maven | 3.9.x | `mvn -v` |
| Node.js + npm | Node 20 o superior (probado con 24) | `node -v` |

> **¿No tienes `mvn` en el PATH?** El repositorio hermano `PaqRap-DP1-EQ8H` trae Maven 3.9.6. Desde
> `Front_PaqRap\back` puedes usarlo así (PowerShell):
> `& "..\..\PaqRap-DP1-EQ8H\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run`

## 2. Qué hay en la carpeta

```
Front_PaqRap/
├─ back/                 Planificador: Spring Boot (REST + WebSocket), motor de simulación, ALNS y ATS
│  └─ src/main/java/com/paqrap/
│     ├─ planificador/   Strategy: PlanningAlgorithm, AlnsPlanner, AtsPlanner, GreedyPlanner, AlgorithmSelector
│     ├─ simulacion/     SimulationEngine, ScenarioRunner, SimulationClock, SnapshotMapper, GridPathfinder
│     ├─ solver/ model/  Algoritmos y dominio del equipo (ALNSOptimizer, ATSOptimizer, Route, Order…)
│     ├─ ingesta/        Parsers de pedidos y bloqueos
│     └─ web/            Controladores REST, publicador WebSocket y reenvío del SPA
├─ front/                Visualizador: React + Vite + TypeScript (mapa en canvas)
├─ data/                 Archivos de prueba (ver sección 6)
└─ g_codigo_algoritmo/   Versión de consola del algoritmo (experimentación numérica)
```

## 3. Opción A — Desarrollo (dos terminales)

**Terminal 1 · Backend** (queda en `http://localhost:8081`):

```powershell
cd Front_PaqRap\back
mvn spring-boot:run
```

Espera la línea `Started PaqrapApplication` y luego `Operación diaria iniciada automáticamente.`

**Terminal 2 · Frontend** (queda en `http://localhost:5173`):

```powershell
cd Front_PaqRap\front
npm install
npm run dev
```

Abre **http://localhost:5173**. Vite reenvía `/api` y `/ws` al backend (`VITE_BACKEND_URL` en `front/.env`),
así que el front no necesita conocer el puerto del back.

**Desde otro dispositivo de la misma red** (celular, otra laptop): abre `http://<IP-de-tu-equipo>:5173`.
Vite ya escucha en todas las interfaces; si no carga, permite Node.js en el firewall de Windows.

## 4. Opción B — Un solo servidor (para el laboratorio o el despliegue)

El front se compila dentro de `back/src/main/resources/static` y el JAR sirve todo desde una sola URL:

```powershell
cd Front_PaqRap\front
npm install
npm run build
cd ..\back
mvn package -DskipTests
java -jar target\paqrap-back-1.0.0.jar
```

Abre **http://localhost:8081** (o `http://<IP>:8081` desde otro dispositivo). Las rutas del SPA
(`/simulacion/mapa`, `/operacion-diaria`, …) se pueden recargar o compartir directamente.

## 5. Configuración

### Backend (`back/src/main/resources/application.properties`)

Cada valor se puede cambiar sin editar el archivo: con variable de entorno (columna 2) o con
`--clave=valor` al final de `java -jar …`.

| Propiedad | Variable | Por defecto | Para qué |
|---|---|---|---|
| `server.port` | `PORT` | `8081` | Puerto HTTP/WebSocket. (En algunos Windows el 8080 lo ocupa un servicio del sistema.) |
| `paqrap.sim.factor.5d` | `PAQRAP_FACTOR_5D` | `160` | Minutos simulados por minuto real. 160x → 5 días en **45 min** (el caso pide 30–60). |
| `paqrap.sim.factor.colapso` | `PAQRAP_FACTOR_COLAPSO` | `480` | Velocidad del escenario de colapso. |
| `paqrap.sim.factor.diario` | `PAQRAP_FACTOR_DIARIO` | `1` | Operación diaria en tiempo real. |
| `paqrap.plan.intervalo-min.simulacion` | — | `5` | Minutos simulados entre planificaciones por lotes. |
| `paqrap.plan.limite-ms` | — | `1500` | Tiempo real máximo de cada ejecución de ALNS/ATS. |
| `paqrap.plan.max-pedidos` | — | `60` | Pedidos más urgentes que entran en cada lote. |
| `paqrap.diario.auto-iniciar` | — | `true` | La operación diaria arranca sola al levantar el servidor. |
| `cors.allowed-origins` | `CORS_ALLOWED_ORIGINS` | `*` | Orígenes permitidos si front y back están en dominios distintos. |

**Prueba rápida de la 5D** (5 días en ~5 minutos), en PowerShell:

```powershell
$env:PAQRAP_FACTOR_5D = "1440"; mvn spring-boot:run
```

o con el JAR: `java -jar target\paqrap-back-1.0.0.jar --paqrap.sim.factor.5d=1440`.
Para la presentación vuelve a 160x: la velocidad la fija el backend, no hay control de velocidad en el
visualizador (RF-068).

### Frontend (`front/.env`)

| Variable | Valor | Efecto |
|---|---|---|
| `VITE_USE_MOCK` | `false` | `true` usa datos simulados en el navegador, sin backend (modo demo). |
| `VITE_API_URL` / `VITE_WS_URL` | vacías | Mismo origen que la página. Solo se llenan si el back está en otro dominio. |
| `VITE_BACKEND_URL` | `http://localhost:8081` | Destino del proxy de Vite en desarrollo. |

## 6. Archivos de prueba (`data/`)

| Archivo | Contenido | Úsalo para |
|---|---|---|
| `ventas.txt` | 641 pedidos de 31 días (~15 por día) | Simulación 5D (usa los días 01 a 05) |
| `bloqueo.txt` | 669 bloqueos del mes | Simulación 5D y colapso |
| `ventas_small.txt`, `bloqueo_small.txt` | 25 pedidos y 5 bloqueos | Pruebas cortas |
| `pedidos_colapso.txt` | 3 305 pedidos en 6 días, demanda creciente (100 → 1 420 por día) | Colapso logístico |
| `generar_colapso.mjs` | Generador determinista del archivo anterior | `node data/generar_colapso.mjs [dias] [pedidosDia1] [crecimiento]` |

Formatos:

- Pedidos: `DDdHHhMMm:posX,posY,idCliente,cantidad,horizonteHoras` — ej. `01d06h00m:31,14,c9103,04,04`
- Bloqueos: `DDdHHhMMm-DDdHHhMMm:x1,y1,x2,y2,…` — tramos horizontales o verticales.

El **día 01** del archivo corresponde a las 00:00 de la *fecha de inicio* elegida en Parámetros. En la
operación diaria, el día 01 es **hoy**.

## 7. Guion de pruebas

### Prueba 0 · Pruebas automáticas del motor (sin servidor)

```powershell
cd Front_PaqRap\back
mvn test
```

`SimulationEngineTest` corre el motor sin Spring y verifica: la 5D completa con `ventas.txt` +
`bloqueo.txt` (los 76 pedidos de los 5 días se entregan, 0 atrasos), que ATS y Greedy también planifican,
que una avería reasigna los pedidos y remolca la unidad, que el colapso se detecta y las reglas de turnos.
Tarda unos 25 s.

### Prueba 1 · Operación diaria (tiempo real)

1. Abre **Operación Diaria**. El indicador verde del menú muestra que la corrida está activa y la cabecera
   dice *Conectado*.
2. Pestaña **Registro → Nuevo pedido**: pulsa *Elegir en el mapa*, haz clic en una celda, completa cliente,
   cantidad y plazo, y registra.
   - Esperado: el pedido aparece como pin hueco (sin unidad) y en segundos se rellena (asignado); una unidad
     sale del almacén y se dibuja su ruta. El indicador morado muestra `ALNS · N planes · último X ms`.
3. Registra un pedido de **más de 24 paquetes**. Esperado: se divide en partes (`ORD-…-1`, `ORD-…-2`) que
   atienden unidades distintas (entrega fraccionada).
4. **Cargar bloqueos** (botón en la cabecera): sube un archivo con un bloqueo vigente hoy, por ejemplo
   `01d00h00m-01d23h59m:40,5,40,20,50,20`. Esperado: línea roja en el mapa y las unidades que iban a
   cruzarla la rodean (selecciona una para ver su ruta).
5. **Registrar avería**: elige una unidad *en ruta* y el tipo. Esperado: el ícono se pone rojo con `!`, sus
   pedidos pasan a otra unidad al instante y la avería aparece en *Averías activas*. Tipo 1 dura 2 h; tipos
   2 y 3 se remolcan al Almacén Central a las 4 h.
6. Pestañas **Vehículos** y **Pedidos**: filtra por estado, busca por ID y haz clic para centrar el mapa y
   abrir el panel de la unidad (ETA, destino, carga y ruta planificada con sus paradas).

> En tiempo real los vehículos avanzan despacio (una bicicleta recorre 1 km en 5 min). Es el
> comportamiento esperado del escenario día a día.

### Prueba 2 · Simulación 5D

1. **Simulación → Parámetros**: sube `data/ventas.txt` (requerido) y `data/bloqueo.txt` (opcional).
2. Elige *Simulación 5D*, el algoritmo (**ALNS** o **ATS**), la fecha de inicio, los umbrales del semáforo
   y la flota. Pulsa **Iniciar Corrida**.
3. En el mapa observa: reloj simulado (día y hora), duración real, velocidad, atrasados, costo
   acumulado y el planificador trabajando por lotes.
4. Al llegar a las 120 h simuladas la corrida termina sola con estado **FINALIZADA** y abre el resumen
   (pedidos, entregados, atrasados, no asignados, cumplimiento, distancia y costo).
5. **Detener** (botón rojo) termina antes y muestra el resumen hasta ese momento. Permite iniciar otra 5D.

Resultado de referencia (ALNS, `ventas.txt` + `bloqueo.txt`): 76 pedidos en 5 días, todos entregados a
tiempo; costo ≈ S/ 22 000 y ≈ 2 770 km.

### Prueba 3 · Colapso logístico

1. Parámetros → *Colapso Logístico*, archivo `data/pedidos_colapso.txt` (y opcionalmente `bloqueo.txt`).
2. Esperado: la demanda crece cada día hasta que un pedido incumple su plazo; la corrida se congela con
   estado **DETENIDA / CONGELADA**, el banner rojo indica el pedido y la hora, y se abre el resumen.
   Con la configuración por defecto ocurre alrededor del **día 4**, a los ~10 minutos reales.
3. Con `ventas.txt` no hay colapso (la flota alcanza): la corrida termina como *FINALIZADA* con el mensaje
   «Sin colapso».

### Prueba 4 · Varios escenarios y varios dispositivos a la vez

- DIARIO, 5D y COLAPSO pueden correr al mismo tiempo (una corrida por tipo). Con dos simulaciones activas el
  mapa muestra el selector *Múltiples simulaciones en curso*.
- Abre el mismo escenario en otro dispositivo: ambos ven el mismo estado; el zoom y la zona visible son
  propios de cada uno. Un dispositivo que entra tarde encuentra la corrida en `GET /api/escenarios/activos`.
- Si el WebSocket se corta, el mapa sigue actualizándose consultando `GET /api/escenarios/{runId}/estado`
  cada 2,5 s y reconecta solo.

### Prueba 5 · API directa (opcional)

```bash
curl http://localhost:8081/api/escenarios/activos
```

```bash
curl -X POST http://localhost:8081/api/pedidos -H "Content-Type: application/json" -d "{\"x\":34,\"y\":15,\"clienteId\":\"c9103\",\"cantidad\":4,\"horizonteHoras\":12}"
```

## 8. Cómo leer el mapa

- **Almacenes**: ícono azul oscuro, barra de stock (los intermedios se recargan a 1 000 a las 23:59:59; el
  central es infinito) y, encima, fichas con las **unidades estacionadas** por tipo (en vez de 37 íconos
  superpuestos).
- **Unidades**: color por tipo (carro azul, moto naranja, bicicleta verde); el **anillo** es el semáforo de
  su pedido más crítico; rojo con `!` = averiada; insignia verde ✓ = entregando (1 h en el cliente).
- **Pedidos**: pin **hueco** = aún sin unidad; pin **relleno** = planificado o en ruta; color = semáforo
  por holgura del plazo (rangos configurables); círculo gris con ✓ = entregado.
- **Bloqueos**: líneas rojas con trazo blanco, solo mientras están vigentes.
- **Rutas**: líneas tenues para toda la flota; al seleccionar una unidad su ruta se resalta con paradas
  numeradas y su ETA.
- **Capas**: en la leyenda puedes ocultar rutas, pedidos, entregados o bloqueos.
- **Detalle**: pasa el cursor sobre unidades, pedidos, almacenes o bloqueos para ver un resumen; clic en
  una unidad (o en un pedido asignado) para seguirla. Rueda o botones +/− para ampliar; arrastra para mover;
  desde ~7 px/km se dibujan las calles cada 1 km.

## 9. Contrato entre front y back

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api/mapa/base` | Grilla, almacenes y umbrales por defecto |
| POST | `/api/archivos/pedidos` | Sube pedidos (multipart `file`) → `{ archivoId, totalPedidos, errores[] }` |
| POST | `/api/archivos/bloqueos` | Sube bloqueos (multipart `file`) → mismo formato |
| POST | `/api/escenarios/{DIARIO\|5D\|COLAPSO}/iniciar` | Crea la corrida (`archivoPedidosId`, `archivoBloqueosId`, algoritmo, fecha, flota, umbrales); 409 si ya hay una del mismo tipo |
| GET | `/api/escenarios/activos` | Corridas en curso |
| GET | `/api/escenarios/{runId}/estado` | Último snapshot (respaldo del WebSocket) |
| POST | `/api/escenarios/{runId}/detener` | Detiene la corrida y publica el resumen |
| POST | `/api/escenarios/{runId}/averias` | `{ vehiculoId, tipo: 1\|2\|3 }` → reasigna sus pedidos |
| POST | `/api/escenarios/{runId}/bloqueos` | Agrega bloqueos a una corrida en curso |
| POST | `/api/pedidos` | Registra un pedido en la operación diaria (201) |
| GET | `/api/pedidos/{id}` | Detalle de un pedido |
| WS | `/ws` → `/topic/sim/{runId}` | Snapshot completo cada segundo |

Errores: `{ "error": "CODIGO", "mensaje": "…" }` con 400, 404 o 409. El snapshot amplía el contrato v1
con campos opcionales: `bloqueos`, `paradas`, `averia`, `planificacion`, `duracionSeg`, `factorVelocidad`,
métricas de costo y distancia, y los estados `AVERIADO` (vehículo) y `DETENIDA` (corrida).

## 10. Supuestos del motor (para validar con el equipo)

- **Tiempo**: el motor mide horas desde el día 01 a las 00:00. En DIARIO el reloj es el real.
- **Planificación por lotes**: cada 5 min simulados (1 min en DIARIO, o de inmediato ante un pedido nuevo,
  una avería o un cambio de bloqueos) se replanifican los pedidos pendientes y los planificados que aún no
  salieron; lo que va en ruta no se toca (RF-019). El algoritmo corre en otro hilo sobre una copia
  inmutable. La solución inicial es una inserción EDF que respeta la hora y el almacén desde los que cada
  unidad queda libre; ALNS o ATS la mejoran con su propia función objetivo.
- **Movimiento**: 1 km por celda, velocidad del tipo de unidad, caminos ortogonales evitando los nodos
  bloqueados vigentes (si un bloqueo aparece a mitad de camino, la unidad se re-rutea). Entregar toma 1 h.
  Tras la última entrega la unidad vuelve al almacén más cercano.
- **Flota inicial**: repartida entre los tres almacenes (IDs `TA01…`, `TM01…`, `TB01…`).
- **Stock**: cada salida descuenta del almacén; si un intermedio no alcanza, la unidad va a cargar al central.
- **Fraccionamiento**: un pedido mayor que la unidad más grande de la flota se divide en partes.
- **Averías**: tipo 1 = 2 h; tipo 2 = hasta el fin del turno siguiente; tipo 3 = 48 h y vuelve en el turno
  de las 15:00. Tipos 2 y 3 se remolcan al central a las 4 h. Los pedidos no entregados se reasignan.
- **Colapso**: la corrida se detiene cuando el primer pedido incumple su plazo (criterio a confirmar con el
  profesor, como indica la arquitectura).
- **No modelado aún**: hora de alimentación por turno y mantenimientos programados (el parser de
  mantenimientos existe, pero el motor no los usa).

## 11. Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| `Port 8081 was already in use` | Otro proceso usa el puerto: `$env:PORT="8090"` y cambia `VITE_BACKEND_URL` a ese puerto. |
| `mvn` no se reconoce | Usa el Maven incluido (ver sección 1) o instálalo y agrégalo al PATH. |
| El mapa dice *Esperando datos* o *Error de conexión* | El backend no está levantado o `VITE_BACKEND_URL` apunta a otro puerto. |
| «Ya hay una corrida activa…» al iniciar | Solo se permite una corrida por tipo: usa *Ver corrida activa* y **Detener**. |
| La 5D tarda 45 minutos | Es lo que pide el caso (30–60 min). Para probar rápido usa `PAQRAP_FACTOR_5D=1440`. |
| Desde el celular no carga | Usa la IP del equipo (no `localhost`) y permite el puerto en el firewall. |
