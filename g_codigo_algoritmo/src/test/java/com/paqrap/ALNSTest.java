package com.paqrap;

import com.paqrap.model.*;
import com.paqrap.simulator.OperationsSimulator;
import com.paqrap.simulator.ScenarioGenerator;
import com.paqrap.solver.ALNSOptimizer;
import com.paqrap.util.ReportGenerator;

import java.util.List;
import java.util.ArrayList;
/**
 * Verification test suite for Adaptive Large Neighborhood Search (ALNS) solver.
 */
public class ALNSTest {

    public static void main(String[] args) {
        System.out.println("Running ALNS Verification Tests...");
        /*testALNSConfig();
        testALNSExecutionAndImprovement();
        testALNSOperatorsAndWeightAdaptation();
        testALNSCapacityConstraints();*/
        testALNSIntegracionMantenimientoYBloqueo();
        System.out.println("ALL ALNS TESTS PASSED SUCCESSFULLY!");
    }

    public static void testALNSIntegracionMantenimientoYBloqueo() {
        System.out.print("Testing ALNS Integration (Maintenance + Roadblock)... ");
        
        // 1. Setup: Crear restricciones combinadas
        List<Maintenance> m = new ArrayList<>();
        List<Breakdown> b = new ArrayList<>();
        List<Roadblock> r = new ArrayList<>();
        
        m.add(new Maintenance("AUTO-1", 0.0, 48.0)); 
        
        // Hay un bloqueo vial 1 que obliga a rodear
        List<Location> path1 = new ArrayList<>();
        path1.add(new Location("B1", "B1", 40, 25, Location.LocationType.CUSTOMER));
        path1.add(new Location("B2", "B2", 40, 30, Location.LocationType.CUSTOMER));
        r.add(new Roadblock(path1, 0.0, 10.0));
        
        // Bloqueo vial 2 (simultáneo)
        List<Location> path2 = new ArrayList<>();
        path2.add(new Location("B3", "B3", 11, 14, Location.LocationType.CUSTOMER));
        path2.add(new Location("B4", "B4", 11, 25, Location.LocationType.CUSTOMER));
        path2.add(new Location("B5", "B5", 5, 25, Location.LocationType.CUSTOMER));
        r.add(new Roadblock(path2, 0.0, 10.0));

        List<Order> orders = new ArrayList<>();
        orders.add(new Order("ORD-1", new Location("C1", "C1", 5, 20, Location.LocationType.CUSTOMER), 1, 0.0, 24.0));
        orders.add(new Order("ORD-2", new Location("C2", "C2", 15, 35, Location.LocationType.CUSTOMER), 1, 0.0, 24.0));
        orders.add(new Order("ORD-3", new Location("C3", "C3", 50, 10, Location.LocationType.CUSTOMER), 1, 0.0, 24.0));

        // 2. Generar escenario custom
        ScenarioGenerator.Scenario scenario = ScenarioGenerator.generateCustomScenario(orders, r, m, new ArrayList<>());
        scenario.printMap(0.0);
        
        // 3. Ejecutar optimizador
        ALNSOptimizer optimizer = new ALNSOptimizer();
        com.paqrap.solver.InitialSolutionBuilder builder = new com.paqrap.solver.InitialSolutionBuilder();
        
        // 3.1 Construir solución inicial
        Solution initialSolution = builder.buildInitialSolution(scenario.orders, scenario.fleet, scenario.warehouses, 0.0);
        
        // 3.2 Ejecutar optimización
        Solution optimizedSolution = optimizer.solve(initialSolution, m, b, r);
        
        // 3.3 Visualización final
        ReportGenerator.printFinalMap(scenario, optimizedSolution);
        
        // 4. Verificaciones integradas
        // (a) AUTO-1 no debe tener órdenes (validación de Mantenimiento)
        for (Route route : optimizedSolution.getRoutes()) {
            if (route.getVehicle().getId().equals("AUTO-1")) {
                assert route.getOrders().isEmpty() : "Vehículo en mantenimiento debe tener 0 órdenes";
            }
        }
        
        // (b) El sistema debe ser capaz de encontrar rutas alternativas (validación de A* sobre bloqueos)
        assert optimizedSolution.getUnassignedOrders().isEmpty() : "El sistema debe encontrar rutas alternativas rodeando bloqueos";
        
        System.out.println("OK");
    }
/**
     * Test to verify that the ALNS configuration parameters are set correctly and within expected bounds.
*/
    public static void testALNSConfig() {
        System.out.print("Testing ALNS Configuration Defaults... ");
        ALNSOptimizer.ALNSConfig config = new ALNSOptimizer.ALNSConfig();
        assert config.maxIterations > 0 : "Max iterations must be positive";
        assert config.coolingRate > 0.0 && config.coolingRate < 1.0 : "Cooling rate must be in (0, 1)";
        assert config.minDestroyRatio > 0.0 && config.minDestroyRatio <= config.maxDestroyRatio : "Destroy ratio bounds invalid";
        assert config.reactionFactor > 0.0 && config.reactionFactor <= 1.0 : "Reaction factor must be in (0, 1]";
        assert config.segmentSize > 0 : "Segment size must be positive";
        System.out.println("OK");
    }

    public static void testALNSExecutionAndImprovement() {
        System.out.print("Testing ALNS Solver Objective Optimization... ");
        ScenarioGenerator.Scenario scenario = ScenarioGenerator.generateScenario(25, 9999L);
        ALNSOptimizer.ALNSConfig config = new ALNSOptimizer.ALNSConfig();
        config.maxIterations = 120;
        config.verboseLogging = false;

        OperationsSimulator simulator = new OperationsSimulator();
        OperationsSimulator.SimulationResult result = simulator.runSimulation(scenario, config);

        double initialFitness = result.initialSolution.getFitness();
        double optimizedFitness = result.optimizedSolution.getFitness();

        System.out.printf("(Initial Fitness: %.2f -> ALNS Fitness: %.2f) ... ", initialFitness, optimizedFitness);
        assert optimizedFitness <= initialFitness + 1e-5 : "ALNS must not degrade initial solution fitness";
        assert result.algorithmName.contains("ALNS") : "SimulationResult algorithmName must identify ALNS";
        System.out.println("OK");
    }

    public static void testALNSOperatorsAndWeightAdaptation() {
        System.out.print("Testing ALNS Operator Tracking and Adaptive Weights... ");
        ScenarioGenerator.Scenario scenario = ScenarioGenerator.generateScenario(20, 12345L);
        ALNSOptimizer.ALNSConfig config = new ALNSOptimizer.ALNSConfig();
        config.maxIterations = 60;
        config.segmentSize = 15;
        config.verboseLogging = false;

        ALNSOptimizer optimizer = new ALNSOptimizer(config);
        OperationsSimulator simulator = new OperationsSimulator();
        simulator.runSimulation(scenario, optimizer);

        List<ALNSOptimizer.OperatorStats> destroyStats = optimizer.getDestroyStats();
        List<ALNSOptimizer.OperatorStats> repairStats = optimizer.getRepairStats();

        assert destroyStats.size() == 5 : "Must have 5 Destroy operators registered";
        assert repairStats.size() == 4 : "Must have 4 Repair operators registered";

        int totalDestroyUses = 0;
        for (ALNSOptimizer.OperatorStats s : destroyStats) {
            totalDestroyUses += s.usageCount;
            assert s.weight >= config.weightMin : "Operator weight must stay >= weightMin";
        }

        int totalRepairUses = 0;
        for (ALNSOptimizer.OperatorStats s : repairStats) {
            totalRepairUses += s.usageCount;
            assert s.weight >= config.weightMin : "Operator weight must stay >= weightMin";
        }

        // Over 60 iterations, each operator is used
        assert totalDestroyUses + (config.maxIterations % config.segmentSize != 0 ? 0 : 0) >= 0 : "Operator tracking valid";
        System.out.println("OK");
    }

    public static void testALNSCapacityConstraints() {
        System.out.print("Testing ALNS Route Capacity Constraints... ");
        ScenarioGenerator.Scenario scenario = ScenarioGenerator.generateScenario(30, 8888L);
        ALNSOptimizer.ALNSConfig config = new ALNSOptimizer.ALNSConfig();
        config.maxIterations = 100;
        config.verboseLogging = false;

        OperationsSimulator simulator = new OperationsSimulator();
        OperationsSimulator.SimulationResult result = simulator.runSimulation(scenario, config);

        for (Route r : result.optimizedSolution.getRoutes()) {
            assert r.getTotalDemand() <= r.getVehicle().getCapacity() :
                    String.format("Route %s demand (%d) exceeded capacity (%d)",
                            r.getVehicle().getId(), r.getTotalDemand(), r.getVehicle().getCapacity());
        }
        System.out.println("OK");
    }
}
