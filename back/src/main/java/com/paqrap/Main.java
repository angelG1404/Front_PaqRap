package com.paqrap;

import com.paqrap.simulator.OperationsSimulator;
import com.paqrap.simulator.ScenarioGenerator;
import com.paqrap.solver.ALNSOptimizer;
import com.paqrap.solver.ATSOptimizer;
import com.paqrap.util.ReportGenerator;
import com.paqrap.model.*;
import java.util.*;

/**
 * Main application entry point for PaqRap VRP Optimization (PUCP 2026-2).
 * Supports both Adaptive Tabu Search (ATS) and Adaptive Large Neighborhood Search (ALNS),
 * as well as side-by-side comparative analysis.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("==========================================================================================");
        System.out.println("   PROTOTIPO DE OPTIMIZACIÓN DE RUTAS EN MAPA MATRICIAL - PAQRAP (PUCP 2026-2)");
        System.out.println("   Metaheurísticas: Búsqueda Tabú Adaptativa (ATS) / Adaptive Large Neighborhood Search (ALNS)");
        System.out.println("==========================================================================================");

        // Determine execution mode from arguments (default: compare)
        String mode = "compare";
        if (args.length > 0) {
            String arg = args[0].trim().toLowerCase().replace("-", "");
            if (arg.equals("alns")) {
                mode = "alns";
            } else if (arg.equals("ats")) {
                mode = "ats";
            } else if (arg.equals("compare")) {
                mode = "compare";
            }
        }

        System.out.printf("Modo seleccionado: [%s.toUpperCase()]\n", mode.toUpperCase());
        System.out.println("Opciones de ejecución:");
        System.out.println("  - java -cp bin com.paqrap.Main alns     -> Ejecuta únicamente ALNS");
        System.out.println("  - java -cp bin com.paqrap.Main ats      -> Ejecuta únicamente ATS");
        System.out.println("  - java -cp bin com.paqrap.Main compare  -> Ejecuta y compara ATS vs ALNS (por defecto)");

        int orderCount = 20; // Customer orders
        long randomSeed = 20260909L;

        System.out.printf(
                "\n[1] Generando Escenario de Operaciones en Matriz (%dx%d) con %d Pedidos y Flota Heterogénea...\n",
                15, 15, orderCount);
        
        List<Maintenance> m = new ArrayList<>();
        List<Breakdown> b = new ArrayList<>();
        List<Roadblock> r = new ArrayList<>();

        if (mode.equals("ats") || mode.equals("compare")) {
            m.add(new Maintenance("AUTO-1", 0.0, 48.0));
            // Roadblock: Ejemplo, bloqueo simple en un par de ubicaciones (ubicación fija para demo)
            List<Location> path = new ArrayList<>();
            path.add(new Location("BLOCKED-1", "Bloqueo 1", 10.0, 10.0, Location.LocationType.CUSTOMER));
            r.add(new Roadblock(path, 0.0, 48.0));
            System.out.println("[INFO] Inyectando restricciones de prueba: Mantenimiento en AUTO-1 y Bloqueo en [10,10]");
        }

        ScenarioGenerator.Scenario scenario = ScenarioGenerator.generateScenario(orderCount, randomSeed, m, b, r);

        System.out.println("\nResumen del Escenario Matriz:");
        System.out.printf(" - Tamaño de Mapa Matriz: %d Filas x %d Columnas (Distancia Manhattan/Euclidiana)\n",
                scenario.gridRows, scenario.gridCols);
        System.out.printf(" - Almacenes: %d (1 Central permanente en [15,15] + 2 Intermedios recargables a 1000 pqts/día)\n",
                scenario.warehouses.size());
        System.out.printf(" - Vehículos en Flota: %d (Autos: 24 pqts/40 kmh; Motos: 8 pqts/25 kmh; Bicis: 4 pqts/12 kmh)\n",
                scenario.fleet.size());
        System.out.printf(" - Pedidos a Procesar: %d con plazos de 4h, 8h, 12h, 18h o 36h\n", scenario.orders.size());

        OperationsSimulator simulator = new OperationsSimulator();

        if (mode.equals("alns")) {
            System.out.println("\n[2] Ejecutando Simulación con Solucionador ALNS (Adaptive Large Neighborhood Search)...");
            ALNSOptimizer.ALNSConfig alnsConfig = new ALNSOptimizer.ALNSConfig();
            alnsConfig.maxIterations = 200;
            alnsConfig.coolingRate = 0.985;
            alnsConfig.segmentSize = 25;
            alnsConfig.verboseLogging = true;

            OperationsSimulator.SimulationResult alnsResult = simulator.runSimulation(scenario, alnsConfig);

            System.out.println("\n[3] Generando Reportes de Desempeño General...");
            ReportGenerator.printSummaryReport(scenario, alnsResult);

            System.out.println("\n[4] GENERANDO REPRODUCCIÓN DE MAPA MATRICIAL TURNO A TURNO...");
            ReportGenerator.renderTurnByTurnSimulation(scenario, alnsResult.optimizedSolution, 10, 100);

        } else if (mode.equals("ats")) {
            System.out.println("\n[2] Ejecutando Simulación con Solucionador ATS (Adaptive Tabu Search)...");
            ATSOptimizer.ATSConfig atsConfig = new ATSOptimizer.ATSConfig();
            atsConfig.maxIterations = 150;
            atsConfig.initialTenure = 6;
            atsConfig.minTenure = 3;
            atsConfig.maxTenure = 20;
            atsConfig.stagnationThreshold = 15;
            atsConfig.verboseLogging = true;

            OperationsSimulator.SimulationResult atsResult = simulator.runSimulation(scenario, atsConfig);

            System.out.println("\n[3] Generando Reportes de Desempeño General...");
            ReportGenerator.printSummaryReport(scenario, atsResult);

            System.out.println("\n[4] GENERANDO REPRODUCCIÓN DE MAPA MATRICIAL TURNO A TURNO...");
            ReportGenerator.renderTurnByTurnSimulation(scenario, atsResult.optimizedSolution, 10, 100);

        } else {
            // Compare mode: Run both ATS and ALNS on the exact same scenario
            System.out.println("\n[2] Ejecutando Solucionador 1: ATS (Adaptive Tabu Search)...");
            ATSOptimizer.ATSConfig atsConfig = new ATSOptimizer.ATSConfig();
            atsConfig.maxIterations = 150;
            atsConfig.initialTenure = 6;
            atsConfig.minTenure = 3;
            atsConfig.maxTenure = 20;
            atsConfig.stagnationThreshold = 15;
            atsConfig.verboseLogging = false; // Keep quiet for clean side-by-side output

            OperationsSimulator.SimulationResult atsResult = simulator.runSimulation(scenario, atsConfig);
            System.out.printf("  -> ATS Finalizado en %d ms | Costo: S/ %.2f | Fitness: %.2f\n",
                    atsResult.executionTimeMs, atsResult.optimizedSolution.getTotalMonetaryCost(), atsResult.optimizedSolution.getFitness());

            System.out.println("\n[3] Ejecutando Solucionador 2: ALNS (Adaptive Large Neighborhood Search)...");
            ALNSOptimizer.ALNSConfig alnsConfig = new ALNSOptimizer.ALNSConfig();
            alnsConfig.maxIterations = 200;
            alnsConfig.coolingRate = 0.985;
            alnsConfig.segmentSize = 25;
            alnsConfig.verboseLogging = false; // Keep quiet for clean side-by-side output

            OperationsSimulator.SimulationResult alnsResult = simulator.runSimulation(scenario, alnsConfig);
            System.out.printf("  -> ALNS Finalizado en %d ms | Costo: S/ %.2f | Fitness: %.2f\n",
                    alnsResult.executionTimeMs, alnsResult.optimizedSolution.getTotalMonetaryCost(), alnsResult.optimizedSolution.getFitness());

            System.out.println("\n[4] GENERANDO CUADRO COMPARATIVO DIRECTO (ATS vs ALNS)...");
            ReportGenerator.printComparisonReport(atsResult, alnsResult);

            // Render turn-by-turn for the best solution between ATS and ALNS
            OperationsSimulator.SimulationResult bestResult =
                    (alnsResult.optimizedSolution.getFitness() <= atsResult.optimizedSolution.getFitness()) ? alnsResult : atsResult;

            System.out.printf("\n[5] GENERANDO REPRODUCCIÓN TURNO A TURNO DE LA MEJOR SOLUCIÓN (%s)...\n",
                    bestResult.algorithmName);
            ReportGenerator.renderTurnByTurnSimulation(scenario, bestResult.optimizedSolution, 8, 100);
        }
    }
}
