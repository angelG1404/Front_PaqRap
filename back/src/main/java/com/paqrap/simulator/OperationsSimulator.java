package com.paqrap.simulator;

import com.paqrap.model.*;
import com.paqrap.solver.ATSOptimizer;
import com.paqrap.solver.InitialSolutionBuilder;
import com.paqrap.solver.RouteOptimizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simulates PaqRap operations and evaluates execution metrics.
 */
public class OperationsSimulator {

    public static class SimulationResult {
        public String algorithmName;
        public RouteOptimizer optimizer;
        public Solution initialSolution;
        public Solution optimizedSolution;
        public List<String> optimizerLogs;
        public Map<Order.TrafficLightStatus, Integer> trafficLightCounts;
        public double totalLateHours;
        public int totalLateOrders;
        public long executionTimeMs;

        public SimulationResult() {
            this.trafficLightCounts = new HashMap<>();
            for (Order.TrafficLightStatus status : Order.TrafficLightStatus.values()) {
                trafficLightCounts.put(status, 0);
            }
        }
    }

    public SimulationResult runSimulation(ScenarioGenerator.Scenario scenario, ATSOptimizer.ATSConfig config) {
        return runSimulation(scenario, new ATSOptimizer(config));
    }

    public SimulationResult runSimulation(ScenarioGenerator.Scenario scenario, com.paqrap.solver.ALNSOptimizer.ALNSConfig config) {
        return runSimulation(scenario, new com.paqrap.solver.ALNSOptimizer(config));
    }

    public SimulationResult runSimulation(ScenarioGenerator.Scenario scenario, com.paqrap.solver.RouteOptimizer optimizer) {
        long startTime = System.currentTimeMillis();
        SimulationResult result = new SimulationResult();
        result.algorithmName = optimizer.getAlgorithmName();
        result.optimizer = optimizer;

        // 1. Generate Initial Solution
        InitialSolutionBuilder builder = new InitialSolutionBuilder();
        result.initialSolution = builder.buildInitialSolution(scenario.orders, scenario.fleet, scenario.warehouses, 0.0);

        // 2. Run Optimizer (ATS or ALNS)
        // 2. Run Optimizer (ATS or ALNS)
        if (optimizer instanceof ATSOptimizer) {
            result.optimizedSolution = ((ATSOptimizer) optimizer).solve(result.initialSolution, scenario.maintenances, scenario.breakdowns, scenario.roadblocks);
        } else if (optimizer instanceof com.paqrap.solver.ALNSOptimizer) {
            result.optimizedSolution = ((com.paqrap.solver.ALNSOptimizer) optimizer).solve(result.initialSolution, scenario.maintenances, scenario.breakdowns, scenario.roadblocks);
        } else {
            result.optimizedSolution = optimizer.solve(result.initialSolution);
        }
        result.optimizerLogs = optimizer.getLogs();

        // 3. Process Route Deliveries & Traffic Light Status
        for (Route r : result.optimizedSolution.getRoutes()) {
            double[] deliveryTimes = new double[r.getOrders().size()];
            double lateHours = r.calculateDeliveryTimesAndLateCount(deliveryTimes);
            result.totalLateHours += lateHours;

            for (int i = 0; i < r.getOrders().size(); i++) {
                Order order = r.getOrders().get(i);
                double delivTime = deliveryTimes[i];
                order.setActualDeliveryTimeHours(delivTime);

                Order.TrafficLightStatus status = order.evaluateStatus(delivTime);
                result.trafficLightCounts.put(status, result.trafficLightCounts.get(status) + 1);

                if (order.isLATE(delivTime)) {
                    result.totalLateOrders++;
                }
            }
        }

        result.executionTimeMs = System.currentTimeMillis() - startTime;
        return result;
    }
}
