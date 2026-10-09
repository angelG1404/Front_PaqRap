package com.paqrap.util;

import com.paqrap.model.*;
import com.paqrap.simulator.OperationsSimulator;
import com.paqrap.simulator.ScenarioGenerator;

import java.util.*;

/**
 * Utility for rendering execution reports, route schedules, comparative analyses (ATS vs ALNS),
 * and Turn-by-Turn Matrix simulation playback for PaqRap.
 */
public class ReportGenerator {

    public static void printHeader(String title) {
        System.out.println("\n==========================================================================================");
        System.out.println("   " + title);
        System.out.println("==========================================================================================");
    }

    public static void printSummaryReport(ScenarioGenerator.Scenario scenario, OperationsSimulator.SimulationResult result) {
        String algo = (result.algorithmName != null) ? result.algorithmName : "ATS";
        printHeader("REPORTE DE RESULTADOS DE SIMULACIÓN Y OPTIMIZACIÓN - " + algo.toUpperCase() + " - PAQRAP");

        Solution initial = result.initialSolution;
        Solution opt = result.optimizedSolution;

        double initialCost = initial.getTotalMonetaryCost();
        double optCost = opt.getTotalMonetaryCost();
        double costSavings = initialCost - optCost;
        double costSavingsPct = (initialCost > 0) ? (costSavings / initialCost) * 100.0 : 0.0;

        System.out.printf("Algoritmo Utilizado:            %s\n", algo);
        System.out.printf("Tiempo de Ejecución:            %d ms\n", result.executionTimeMs);
        System.out.println("------------------------------------------------------------------------------------------");
        System.out.printf(" Solución Inicial (EDF Greedy) -> Costo: S/ %.2f | Distancia: %.2f km | Fitness: %.2f\n",
                initialCost, initial.getTotalDistanceKm(), initial.getFitness());
        //CAMBIO ITERACION 01: Cambiado "Fitness" por Suma de Horas
        System.out.printf(" Solución Optimizada (%-8s) -> Costo: S/ %.2f | Distancia: %.2f km | Fitness (Suma hrs): %.2f\n",
                (algo.contains("ALNS") ? "ALNS" : "ATS"), optCost, opt.getTotalDistanceKm(), opt.getFitness());
        System.out.printf(" >>> AHORRO DE COSTO OPERATIVO: S/ %.2f (%.2f%% de optimización)\n", costSavings, costSavingsPct);
        System.out.println("------------------------------------------------------------------------------------------");

        //CAMBIO ITERACION 01: Nueva sección de estadísticas
        System.out.println("\n--- ESTADÍSTICAS DE ESCENARIO (MANTENIMIENTO/AVERÍA/BLOQUEO) ---");
        System.out.printf(" Vehículos afectados por Avería/Mantenimiento: %d\n", 0); // Placeholder
        System.out.printf(" Pedidos reasignados: %d\n", 0); // Placeholder
        System.out.printf(" Tramos recalculados por BloqueoVial: %d\n", 0); // Placeholder

        System.out.println("\n--- MONITOR DE CUMPLIMIENTO Y ESTADO DE SEMÁFORO ---");
        Map<Order.TrafficLightStatus, Integer> counts = result.trafficLightCounts;
        int totalOrders = opt.getTotalServedOrders();

        for (Order.TrafficLightStatus status : Order.TrafficLightStatus.values()) {
            int count = counts.getOrDefault(status, 0);
            double pct = (totalOrders > 0) ? (count * 100.0 / totalOrders) : 0.0;
            String icon = (status == Order.TrafficLightStatus.VERDE) ? "[🟢 VERDE]" :
                    (status == Order.TrafficLightStatus.AMBAR) ? "[🟡 ÁMBAR]" : "[🔴 ROJO]";
            System.out.printf(" %-12s: %3d / %3d pedidos (%6.2f%%) - %s\n",
                    icon, count, totalOrders, pct, status.getLabel());
        }

        System.out.printf(" Pedidos fuera de plazo (Late): %d | Horas acumuladas de retraso: %.2f hrs\n",
                result.totalLateOrders, result.totalLateHours);

        printHeader("DETALLE DE RUTAS Y ASIGNACIÓN DE FLOTA");
        int routeIdx = 1;
        
        for (Route r : opt.getRoutes()) {
            if (r.getOrders().isEmpty()) continue;
            Vehicle v = r.getVehicle();
            String typeCode = (v.getType() == VehicleType.AUTO) ? "U" :
                            (v.getType() == VehicleType.MOTO) ? "M" : "C";
            
            System.out.printf("\n🚛 RUTA #%d - Vehículo [%s] %s (%s) | Capacidad: %d/%d paquetes\n",
                    routeIdx++, typeCode, v.getId(), v.getType().getDisplayName(), r.getTotalDemand(), v.getCapacity());
            System.out.printf("   Depósito Origen: %s | Distancia: %.2f km | Costo: S/ %.2f\n",
                    r.getDepot().getName(), r.getTotalDistanceKm(), r.getCost());
            System.out.println("   Secuencia de Visitas (Transporte utilizado: " + v.getId() + " - " + v.getType().getDisplayName() + "):");

            double[] delivTimes = new double[r.getOrders().size()];
            r.calculateDeliveryTimesAndLateCount(delivTimes);
            
            AStarGridRouter router = new AStarGridRouter(71, 51);
            Location currentLoc = r.getDepot();

            for (int i = 0; i < r.getOrders().size(); i++) {
                Order o = r.getOrders().get(i);
                
                // Trazar camino usando los bloqueos del escenario recibido
                List<Location> path = router.calculatePath(currentLoc, o.getDestination(), scenario.roadblocks, delivTimes[i]);
                
                Order.TrafficLightStatus status = o.evaluateStatus(delivTimes[i]);
                String colorTag = (status == Order.TrafficLightStatus.VERDE) ? "VERDE" :
                        (status == Order.TrafficLightStatus.AMBAR) ? "AMBAR" : "ROJO";

                System.out.printf("     [%d] Pedido %s atendido por %s -> Cliente (%s) | Límite: %.1fh | Entrega Est.: %.2fh [%s]\n",
                        (i + 1), o.getId(), v.getId(), o.getDestination().getName(), o.getDeadlineHours(), delivTimes[i], colorTag);
                System.out.print("         Camino: ");
                for (Location loc : path) {
                    System.out.printf("[%d,%d] ", (int)loc.getX(), (int)loc.getY());
                }
                System.out.println();
                
                currentLoc = o.getDestination();
            }
        }
        System.out.println("\n==========================================================================================");
    }

    public static void printSummaryReport(OperationsSimulator.SimulationResult result) {
        // Solo para compatibilidad, redirige a la version completa pasando un escenario vacio o nulo si es necesario
        // Pero en este caso, mejor que lance un error o cree un escenario vacio
        printSummaryReport(new ScenarioGenerator.Scenario(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()), result);
    }

    /**
     * Renders a turn-by-turn simulation playback of vehicle routes on the console.
     */
    public static void renderTurnByTurnSimulation(ScenarioGenerator.Scenario scenario, Solution solution, int totalTurns, int delayMs) {
        printHeader("REPRODUCCIÓN TURNO A TURNO DEL MAPA DE OPERACIONES (30 MIN/TURNO)");

        double stepHours = 0.5; // Each turn advances 30 minutes
        List<Route> activeRoutes = new ArrayList<>();
        for (Route r : solution.getRoutes()) {
            if (!r.getOrders().isEmpty()) {
                activeRoutes.add(r);
            }
        }

        for (int turn = 1; turn <= totalTurns; turn++) {
            double currentSimTime = turn * stepHours;
            System.out.printf("\n--- TURNO %2d / %2d (Tiempo Simulado: %.1f hrs) ---\n", turn, totalTurns, currentSimTime);

            for (int rIdx = 0; rIdx < activeRoutes.size(); rIdx++) {
                Route route = activeRoutes.get(rIdx);
                Vehicle v = route.getVehicle();
                double[] delivTimes = new double[route.getOrders().size()];
                route.calculateDeliveryTimesAndLateCount(delivTimes);

                // Determine vehicle status at currentSimTime
                String statusDesc = "En Depósito (" + route.getDepot().getName() + ")";
                Location currentApproxLoc = route.getDepot();

                double prevTime = 0.0;
                for (int i = 0; i < route.getOrders().size(); i++) {
                    Order order = route.getOrders().get(i);
                    double arrival = delivTimes[i];
                    double finish = arrival + 1.0; // 1 hr service

                    if (currentSimTime < arrival && currentSimTime >= prevTime) {
                        statusDesc = String.format("En Tránsito hacia Pedido %s (%s)", order.getId(), order.getDestination().getName());
                        currentApproxLoc = order.getDestination();
                        break;
                    } else if (currentSimTime >= arrival && currentSimTime <= finish) {
                        statusDesc = String.format("Descargando/Entregando Pedido %s en %s", order.getId(), order.getDestination().getName());
                        currentApproxLoc = order.getDestination();
                        break;
                    } else if (i == route.getOrders().size() - 1 && currentSimTime > finish) {
                        statusDesc = "Ruta completada -> Retornando / En Depósito";
                        currentApproxLoc = route.getDepot();
                    }
                    prevTime = finish;
                }

                System.out.printf("  [Vehículo %-7s | %-9s] Pos aprox: [%.1f, %.1f] | Estado: %s\n",
                        v.getId(), v.getType().getDisplayName(), currentApproxLoc.getX(), currentApproxLoc.getY(), statusDesc);
            }

            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        System.out.println("\n--- Fin de Reproducción de Turnos ---");
    }

    /**
     * Prints a side-by-side comparative report between ATS and ALNS results.
     */
    public static void printComparisonReport(OperationsSimulator.SimulationResult atsResult, OperationsSimulator.SimulationResult alnsResult) {
        printHeader("CUADRO COMPARATIVO: BÚSQUEDA TABÚ ADAPTATIVA (ATS) vs ALNS");

        Solution atsOpt = atsResult.optimizedSolution;
        Solution alnsOpt = alnsResult.optimizedSolution;
        Solution initial = atsResult.initialSolution;

        double initialCost = initial.getTotalMonetaryCost();

        double atsCost = atsOpt.getTotalMonetaryCost();
        double alnsCost = alnsOpt.getTotalMonetaryCost();

        double atsSaving = initialCost - atsCost;
        double alnsSaving = initialCost - alnsCost;

        double atsSavingPct = (initialCost > 0) ? (atsSaving / initialCost) * 100.0 : 0.0;
        double alnsSavingPct = (initialCost > 0) ? (alnsSaving / initialCost) * 100.0 : 0.0;

        int atsVerde = atsResult.trafficLightCounts.getOrDefault(Order.TrafficLightStatus.VERDE, 0);
        int alnsVerde = alnsResult.trafficLightCounts.getOrDefault(Order.TrafficLightStatus.VERDE, 0);

        int atsAmbar = atsResult.trafficLightCounts.getOrDefault(Order.TrafficLightStatus.AMBAR, 0);
        int alnsAmbar = alnsResult.trafficLightCounts.getOrDefault(Order.TrafficLightStatus.AMBAR, 0);

        int atsRojo = atsResult.trafficLightCounts.getOrDefault(Order.TrafficLightStatus.ROJO, 0);
        int alnsRojo = alnsResult.trafficLightCounts.getOrDefault(Order.TrafficLightStatus.ROJO, 0);

        System.out.printf("%-36s | %-24s | %-24s\n", "MÉTRICA / INDICADOR", "ATS (Tabú Adaptativo)", "ALNS (Large Neighb.)");
        System.out.println("------------------------------------------------------------------------------------------");
        System.out.printf("%-36s | S/ %-21.2f | S/ %-21.2f\n", "Costo Operativo Total", atsCost, alnsCost);
        System.out.printf("%-36s | %-21.2f km | %-21.2f km\n", "Distancia Total Recorrida", atsOpt.getTotalDistanceKm(), alnsOpt.getTotalDistanceKm());
        System.out.printf("%-36s | %-24.2f | %-24.2f\n", "Función Objetivo (Fitness)", atsOpt.getFitness(), alnsOpt.getFitness());
        System.out.printf("%-36s | S/ %.2f (%.1f%%)         | S/ %.2f (%.1f%%)\n", "Ahorro vs Greedy Inicial", atsSaving, atsSavingPct, alnsSaving, alnsSavingPct);
        System.out.printf("%-36s | %-24d | %-24d\n", "Tiempo de Cómputo (ms)", atsResult.executionTimeMs, alnsResult.executionTimeMs);
        System.out.println("------------------------------------------------------------------------------------------");
        System.out.printf("%-36s | %-24d | %-24d\n", "🟢 Pedidos en Verde (Seguros)", atsVerde, alnsVerde);
        System.out.printf("%-36s | %-24d | %-24d\n", "🟡 Pedidos en Ámbar (Alerta)", atsAmbar, alnsAmbar);
        System.out.printf("%-36s | %-24d | %-24d\n", "🔴 Pedidos en Rojo (Críticos)", atsRojo, alnsRojo);
        System.out.printf("%-36s | %-24d | %-24d\n", "Pedidos con Entrega Tardía", atsResult.totalLateOrders, alnsResult.totalLateOrders);
        System.out.printf("%-36s | %-21.2f hrs| %-21.2f hrs\n", "Horas Acumuladas de Retraso", atsResult.totalLateHours, alnsResult.totalLateHours);
        System.out.println("------------------------------------------------------------------------------------------");

        String winnerCost = (alnsCost < atsCost - 0.01) ? "ALNS (-S/ " + String.format("%.2f", atsCost - alnsCost) + ")" :
                (atsCost < alnsCost - 0.01) ? "ATS (-S/ " + String.format("%.2f", alnsCost - atsCost) + ")" : "EMPATE";

        String winnerTime = (alnsResult.executionTimeMs < atsResult.executionTimeMs) ? "ALNS" : "ATS";
        String winnerPunctuality = (alnsResult.totalLateOrders < atsResult.totalLateOrders) ? "ALNS" :
                (atsResult.totalLateOrders < alnsResult.totalLateOrders) ? "ATS" : "EMPATE";

        System.out.printf("🏆 Menor Costo Operativo:       %s\n", winnerCost);
        System.out.printf("⚡ Menor Tiempo de Cómputo:     %s\n", winnerTime);
        System.out.printf("🎯 Mayor Puntualidad de Rutas:  %s\n", winnerPunctuality);
        System.out.println("==========================================================================================");
    }

    /**
     * Renders a turn-by-turn simulation playback of vehicle routes on the console.
     */
    public static void printFinalMap(ScenarioGenerator.Scenario scenario, Solution solution) {
        printHeader("MAPA FINAL DE RUTAS SEGUIDAS");
        int gridRows = scenario.gridRows;
        int gridCols = scenario.gridCols;
        char[][] map = new char[gridRows][gridCols];
        for (int x = 0; x < gridRows; x++) Arrays.fill(map[x], '.');

        // Marcar Almacenes
        for (Warehouse w : scenario.warehouses) {
            int wx = (int) Math.round(w.getLocation().getX());
            int wy = (int) Math.round(w.getLocation().getY());
            if (wx >= 0 && wx < gridRows && wy >= 0 && wy < gridCols) map[wx][wy] = 'A';
        }

        // Marcar Pedidos
        for (Order o : scenario.orders) {
            int px = (int) Math.round(o.getDestination().getX());
            int py = (int) Math.round(o.getDestination().getY());
            if (px >= 0 && px < gridRows && py >= 0 && py < gridCols) {
                if (map[px][py] == '.') map[px][py] = 'P';
            }
        }

        // Marcar Obstáculos
        for (Roadblock rb : scenario.roadblocks) {
            for (Location loc : rb.path) {
                int rx = (int) Math.round(loc.getX());
                int ry = (int) Math.round(loc.getY());
                if (rx >= 0 && rx < gridRows && ry >= 0 && ry < gridCols) map[rx][ry] = 'X';
            }
        }

        // Marcar rutas
        AStarGridRouter router = new AStarGridRouter(gridRows, gridCols);
        for (Route r : solution.getRoutes()) {
            if (r.getOrders().isEmpty()) continue;
            
            char routeChar;
            switch (r.getVehicle().getType()) {
                case AUTO: routeChar = 'U'; break;
                case MOTO: routeChar = 'M'; break;
                case BICI: routeChar = 'C'; break;
                default: routeChar = '?';
            }
            
            Location currentLoc = r.getDepot();
            for (Order o : r.getOrders()) {
                List<Location> path = router.calculatePath(currentLoc, o.getDestination(), scenario.roadblocks, 0.0);
                for (Location loc : path) {
                    int lx = (int) loc.getX();
                    int ly = (int) loc.getY();
                    // Solo marcar si no es un almacen ('A')
                    if (map[lx][ly] != 'A' && map[lx][ly] != 'P') {
                        map[lx][ly] = routeChar;
                    }
                }
                currentLoc = o.getDestination();
            }
        }

        // Imprimir (Origen 0,0 en esquina inferior izquierda)
        for (int y = gridCols - 1; y >= 0; y--) {
            for (int x = 0; x < gridRows; x++) {
                System.out.print(map[x][y] + " ");
            }
            System.out.println();
        }
        System.out.println("Leyenda: . Vacío | A Almacén | P Pedido | X Bloqueo | U ruta");

        // Reporte enumerado de asignaciones
        System.out.println("\n--- Resumen de Asignación Final ---");
        int count = 1;
        for (Route r : solution.getRoutes()) {
            if (r.getOrders().isEmpty()) continue;
            for (Order o : r.getOrders()) {
                System.out.printf("%d. Pedido %s atendido por %s (%s)\n", 
                                count++, o.getId(), r.getVehicle().getId(), r.getVehicle().getType().getDisplayName());
            }
        }
    }
}
