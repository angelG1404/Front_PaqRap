  ATS (Adaptive Tabu Search) — Estado de Implementación

  1. Qué resuelve
  El algoritmo ATS resuelve el problema de enrutamiento de vehículos (VRP) para la flota de PaqRap, considerando una
  flota heterogénea (autos, motos, bicicletas), ventanas de tiempo estrictas y múltiples almacenes. El objetivo es
  asignar pedidos de forma eficiente a los vehículos disponibles, respetando restricciones operativas dinámicas en un
  mapa cuadriculado con obstáculos y eventos inesperados.

  2. Objetivo que optimiza
  El algoritmo minimiza una función de Fitness definida como la suma de las horas de llegada de todos los pedidos.
   * ¿Por qué?: Se prioriza la puntualidad extrema. Al minimizar la hora de entrega de cada pedido individual,
     garantizamos que el cumplimiento de las ventanas de tiempo sea la métrica principal, por encima de la distancia
     recorrida o el costo monetario.
   * Ajuste: Se ha añadido una penalización por vehículo utilizado, lo que obliga al optimizador a consolidar pedidos en
     menos vehículos siempre que los tiempos de entrega lo permitan.

  3. Restricciones duras que respeta
  Si una solución viola cualquiera de estas reglas, se marca como no factible y es descartada por el optimizador:
   * Capacidad del vehículo: La suma de paquetes de los pedidos en una ruta no puede exceder el límite del vehículo.
   * Ventana de tiempo (Deadline): Cada pedido debe ser entregado antes de su hora límite asignada.
   * Mantenimiento programado: El vehículo no puede estar en uso durante el periodo de mantenimiento.
   * Avería activa: Si un vehículo está averiado, no puede realizar entregas.

  4. Reglas de negocio especiales cubiertas
   * Mantenimiento preventivo: El vehículo queda bloqueado según su ID y rango de tiempo; el sistema impide asignar
     pedidos a vehículos con mantenimiento programado.
   * Avería: Si un vehículo sufre una avería, el sistema invalida sus rutas actuales. El optimizador debe reasignar los
     pedidos pendientes a otros vehículos operativos.
   * Bloqueo vial: Las coordenadas definidas como bloqueadas actúan como paredes infranqueables en el mapa. El algoritmo
     A* recalcula los caminos de los vehículos para rodear estas zonas, obligando a los vehículos a tomar rutas
     alternativas.

  5. Cómo funciona el algoritmo (flujo general)
   1. Solución inicial: Se genera mediante una heurística Greedy basada en Earliest Deadline First (EDF), intentando
      cubrir todos los pedidos lo más pronto posible.
   2. Generación de vecindario: Se crean soluciones candidatas mediante operadores locales: RELOCATE (mover un pedido),
      SWAP (intercambiar pedidos) y VEHICLE_REASSIGN (mover un pedido a otro vehículo).
   3. Filtro de factibilidad: Se eliminan inmediatamente todas las soluciones que violan restricciones duras.
   4. Selección: Se elige el mejor movimiento no tabú (o que cumpla el criterio de aspiración).
   5. Mecanismo adaptativo: Si tras varias iteraciones no hay mejora (estancamiento), el algoritmo aumenta la "Tenencia
      Tabú" para forzar la diversificación y explorar nuevas zonas del mapa.
   6. Criterio de parada: El proceso se repite por 300 iteraciones o hasta agotar el tiempo.
   7. Resultado: Retorna la mejor solución encontrada. Si el problema es demasiado restrictivo para la flota, algunos
      pedidos pueden quedar como "no asignados" (colapso logístico).

  6. Estructura de clases principales

  | Clase | Responsabilidad |
  |---|---|
  | `Solution` | Contenedor del estado actual (rutas, pedidos no asignados) y fitness. |
  | `Route` | Define la secuencia de pedidos y el vehículo asignado a una ruta específica. |
  | `Order` | Define los datos del pedido: destino, cantidad, ventana de tiempo y estado. |
  | `Vehicle` | Representa la unidad de transporte, su tipo, capacidad y estado (mantenimiento/avería). |
  | `ATSOptimizer` | Motor central que ejecuta la búsqueda tabú adaptativa. |
  | `TabuList` | Gestiona los movimientos prohibidos temporalmente para evitar ciclos. |
  | `ATSConfig` | Parámetros de configuración (iteraciones, tenencia inicial, límite de estancamiento). |
  | `Maintenance` | Define restricciones temporales de mantenimiento por vehículo. |
  | `Breakdown` | Define eventos de avería que inmovilizan vehículos. |
  | `Roadblock` | Define coordenadas y tiempos donde el camino es intransitable. |
  | `AStarGridRouter` | Calcula caminos óptimos esquivando bloqueos mediante A*. |

  7. Limitaciones conocidas de esta iteración
   * Datos fijos: Los datos de mantenimiento, averías y bloqueos se definen como objetos en el código (tests) o se
     inyectan; no hay un motor de lectura de archivos JSON/CSV.
   * Simulación estática: No existe un reloj dinámico que avance en tiempo real; el algoritmo optimiza sobre una foto
     estática del escenario.
   * Dependencia en estado interno: isFeasible() depende del estado del objeto Vehicle. Esto es funcional pero requiere
     que el escenario inicial esté perfectamente sincronizado con el estado de los vehículos.

  8. Cómo probarlo
  Para verificar el comportamiento del optimizador, ejecuta el test de integración que simula restricciones combinadas
  (mantenimiento + bloqueos + asignación heterogénea):


  java -cp "target/classes;target/test-classes" com.paqrap.ATSTest


  Este test valida que el vehículo en mantenimiento no trabaje, que se rodeen los bloqueos viales y que el sistema
  encuentre una solución óptima factible imprimiendo un mapa de rutas final.

  
  
  ---
  Fecha de versión: 17 de septiembre de 2026. Corresponde a la Iteración 01 del algoritmo ATS.
