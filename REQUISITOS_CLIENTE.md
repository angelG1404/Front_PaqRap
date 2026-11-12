# Requisitos funcionales derivados de las preguntas del cliente

Los identificadores `RF-CL-xx` son provisionales: no se conoce la numeración libre de la LE v02, así que hay que
reemplazarlos por los que correspondan antes de entregar el documento.

## 1. Almacenes: envíos y paquetes entregados en las últimas x horas (x = 4)

| ID | Requisito | Estado |
| --- | --- | --- |
| RF-CL-01 | El panel de almacenes debe listar los almacenes con, para cada uno, los envíos despachados y los paquetes entregados en las últimas x horas simuladas (x = 4 por defecto). | Implementado |
| RF-CL-02 | Al seleccionar un almacén, el sistema debe mostrar el detalle de la ventana (desde/hasta): envíos despachados, envíos entregados y envíos en tránsito, cada uno con pedido, cantidad, unidad, cliente, destino y hora. | Implementado |
| RF-CL-03 | La ventana x debe ser un parámetro del sistema (`ScenarioDefaults.VENTANA_ACTIVIDAD_HORAS`) y de la consulta (`?horas=`, entre 0 y 72). | Implementado |
| RF-CL-04 | La actividad se calcula con el reloj de la corrida: en simulación, con horas simuladas; en operación diaria, con la hora real. | Implementado |

**Origen de los datos.** Cada pedido guarda el almacén desde el que salió y la hora de despacho y de entrega
(`PedidoSim`). El motor calcula la ventana en `SimulationEngine.actividadAlmacen`. El resumen por almacén viaja en
cada snapshot (`almacenes[].actividad`) y el detalle se consulta con
`GET /api/escenarios/{runId}/almacenes/{almacenId}/actividad?horas=4`.

## 2. Enlace bidireccional entre el panel de almacenes y el mapa

| ID | Requisito | Estado |
| --- | --- | --- |
| RF-CL-05 | Ida: al seleccionar un almacén en la lista, el mapa debe resaltarlo (anillo azul) y centrarse en él. | Implementado |
| RF-CL-06 | Vuelta: al seleccionar un almacén en el mapa (clic), el panel debe abrir la pestaña Almacenes, marcar ese almacén en la lista y mostrar su detalle. | Implementado |
| RF-CL-07 | Volver a elegir el almacén ya seleccionado (en la lista) debe quitar la selección en ambos lados. | Implementado |
| RF-CL-08 | Si el panel está plegado, la selección desde el mapa debe desplegarlo; la fila elegida debe quedar visible en la lista. | Implementado |

## 3. Atributos de la avería (tipos 1, 2 y 3)

El operador solo elige **la unidad y el tipo**. El resto de atributos los fija el motor al registrarla y los
actualiza durante la corrida; la entidad sigue el documento de estructura de datos (`Averia`).

| ID | Requisito | Estado |
| --- | --- | --- |
| RF-CL-09 | Al registrar una avería, el sistema debe fijar: identificador (`AV-nnnn`), ocurrencia = momento actual de la corrida, ubicación = posición de la unidad en ese instante, estado = activa y el fin según el tipo. | Implementado |
| RF-CL-10 | Tipo 1 (menor): fin = ocurrencia + 2 h. La unidad queda detenida en el lugar y vuelve al servicio al terminar. | Implementado |
| RF-CL-11 | Tipo 2 (intermedia): fin = fin del turno siguiente al de la ocurrencia (turnos 07:00, 15:00 y 23:00). A las 4 h la unidad se remolca al Almacén Central y su ubicación se actualiza. | Implementado |
| RF-CL-12 | Tipo 3 (mayor): fin = primer inicio de turno de 15:00 posterior a 48 h desde la ocurrencia. A las 4 h la unidad se remolca al Almacén Central y su ubicación se actualiza. | Implementado |
| RF-CL-13 | Al llegar el fin, la avería pasa a *resuelta*, se registra su hora de resolución y la unidad vuelve a operar. Las averías resueltas se conservan en el historial de la corrida. | Implementado |
| RF-CL-14 | Los pedidos de la unidad averiada que no se entregaron vuelven a pendiente y se replanifican de inmediato; la avería guarda cuántos fueron. | Implementado |
| RF-CL-15 | La interfaz debe mostrar, por avería: id, unidad, tipo, estado, ocurrencia, fin estimado y duración, ubicación actual, remolque (programado o realizado), resolución y pedidos reasignados; con filtro activas / resueltas / todas. Al registrar, debe explicar la regla del tipo elegido. | Implementado |

**Dónde está.** Reglas y actualización: `SimulationEngine.registrarAveria`, `remolcarAlCentral` y `terminarAveria`.
Entidad: `AveriaSim`. Snapshot: `averias[]` (más `vehiculos[].averia`). API: `POST` y `GET /api/escenarios/{runId}/averias`.
Interfaz: `RegistroAveriaForm` e `IncidenciasPanel`.

## 4. Configuración editable (nuevos requisitos)

| ID | Requisito | Estado |
| --- | --- | --- |
| RF-CL-16 | Los atributos de los tres tipos de avería deben poder modificarse en el programa: tipo 1 (duración), tipo 2 (turnos de espera y horas hasta el remolque) y tipo 3 (horas mínimas, turno de regreso 07:00/15:00/23:00 y horas hasta el remolque). Se definen en Parámetros antes de iniciar y se pueden cambiar con la corrida en marcha. | Implementado |
| RF-CL-17 | Un cambio en las reglas de avería rige para las averías que se registren desde ese momento; las ya registradas conservan sus atributos. | Implementado |
| RF-CL-18 | La ubicación (X, Y) y la capacidad de cada almacén deben poder modificarse (antes de iniciar y en caliente). Al mover un almacén, las unidades estacionadas se mueven con él y las que regresan recalculan su camino; al bajar la capacidad el stock se ajusta; el central mantiene inventario infinito. | Implementado |
| RF-CL-19 | La lista de unidades de transporte debe poder ordenarse por nivel de ocupación ascendente y descendente (además de por ID), con el control visible sobre la lista. | Implementado |
| RF-CL-20 | La capacidad de cada tipo de unidad (auto, moto, bicicleta) debe ser configurable (antes de iniciar y en caliente). Rige para las rutas que salgan desde ese momento; un pedido pendiente que deje de caber se fracciona. | Implementado |
| RF-CL-21 | Los valores configurados se validan (rangos, almacenes en celdas distintas dentro de la grilla) y el sistema rechaza con un mensaje claro lo que no cumpla. | Implementado |

**Dónde está.** Modelo y validación: `ConfiguracionCorrida`. Aplicación en caliente: `SimulationEngine.aplicarConfiguracion`.
API: `GET` y `PUT /api/escenarios/{runId}/configuracion` (el `PUT` es parcial); al iniciar, el campo `configuracion` de
`POST /api/escenarios/{tipo}/iniciar`. Interfaz: tarjetas editables en Parámetros y botón *Configuración* en el mapa.
Orden de la lista: `MapaEnVivo` (barra *Ordenar por ocupación*).

## 5. Dudas por confirmar con el cliente

1. **¿Qué significa «envíos y paquetes entregados»?** Se interpretó como dos indicadores por almacén: envíos que
   *salieron* del almacén en las últimas 4 h y paquetes *entregados* a clientes en ese lapso, para pedidos cuyo origen
   es ese almacén. Si el cliente quiere otra cosa (por ejemplo, paquetes que *llegaron* al almacén), hay que cambiarlo.
2. **¿Pedido que vuelve a pendiente por una avería?** Hoy deja de contar como despachado desde ese almacén. Físicamente
   los paquetes sí salieron; si el cliente lo prefiere, se puede conservar el envío como despachado.
3. **¿La ventana de 4 h es fija o la elige el usuario?** Hoy es un valor del sistema; la consulta admite otro valor,
   pero la interfaz no tiene un selector.
4. **El caso habla de «aeropuerto/ciudad».** En PaqRap los nodos son almacenes (central y dos intermedios); se aplicó
   a ellos. Confirmar que la pregunta del cliente viene de otra plantilla.
5. **Averías: ¿solo las registra el operador o también llegan por archivo?** Hoy solo se registran a mano (en
   operación diaria y en simulación). El parser de averías por archivo existe pero el motor no lo usa.
6. **Tipo 2: «permanece en el lugar 4 horas como máximo».** Se interpretó como remolque al central a las 4 h, con el fin
   de la indisponibilidad al final del turno siguiente. Lo mismo se aplicó al tipo 3 (traslado a taller a las 4 h).
7. **Carga a bordo de la unidad averiada.** Los paquetes se reasignan como pedidos pendientes que salen de un almacén;
   no se modela que otra unidad los recoja en el lugar de la avería (el enunciado habla de reasignar productos en camino).
8. **Cantidad de unidades de la flota en caliente.** Hoy solo se define al iniciar; las capacidades sí cambian en caliente.
9. **Averías ya registradas.** Si el cliente quiere que un cambio de reglas también actualice las averías activas, hay que añadirlo.
10. **Capacidad de un almacén que sube.** El stock no aumenta de inmediato: se completa en la recarga de las 23:59:59.
