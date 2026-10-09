package com.paqrap;

import com.paqrap.simulator.OperationsSimulator;
import com.paqrap.simulator.ScenarioGenerator;
import com.paqrap.solver.ALNSOptimizer;
import com.paqrap.solver.ATSOptimizer;

/**
 * Cross-verification test suite comparing ATS and ALNS performance.
 */
public class OptimizerComparisonTest {

    public static void main(String[] args) {
        System.out.println("Running Optimizer Comparison Tests (ATS vs ALNS)...");
        testBothOptimizersImproveInitial();
        System.out.println("ALL COMPARISON TESTS PASSED SUCCESSFULLY!");
    }

    public static void testBothOptimizersImproveInitial() {
        System.out.print("Testing ATS and ALNS on shared scenario instances... ");

        long[] testSeeds = {12345L, 54321L};
        OperationsSimulator simulator = new OperationsSimulator();

        for (long seed : testSeeds) {
            ScenarioGenerator.Scenario scenario = ScenarioGenerator.generateScenario(20, seed);

            // 1. Run ATS
            ATSOptimizer.ATSConfig atsConfig = new ATSOptimizer.ATSConfig();
            atsConfig.maxIterations = 80;
            atsConfig.verboseLogging = false;
            OperationsSimulator.SimulationResult atsRes = simulator.runSimulation(scenario, atsConfig);

            // 2. Run ALNS
            ALNSOptimizer.ALNSConfig alnsConfig = new ALNSOptimizer.ALNSConfig();
            alnsConfig.maxIterations = 80;
            alnsConfig.verboseLogging = false;
            OperationsSimulator.SimulationResult alnsRes = simulator.runSimulation(scenario, alnsConfig);

            double initialFitness = atsRes.initialSolution.getFitness();
            assert atsRes.optimizedSolution.getFitness() <= initialFitness + 1e-4 : "ATS did not improve or preserve fitness";
            assert alnsRes.optimizedSolution.getFitness() <= initialFitness + 1e-4 : "ALNS did not improve or preserve fitness";

            assert atsRes.optimizedSolution.getTotalServedOrders() > 0 : "ATS served orders must be positive";
            assert alnsRes.optimizedSolution.getTotalServedOrders() > 0 : "ALNS served orders must be positive";

            System.out.printf("\n  [Seed %d] Initial: %.2f | ATS: %.2f | ALNS: %.2f",
                    seed, initialFitness, atsRes.optimizedSolution.getFitness(), alnsRes.optimizedSolution.getFitness());
        }

        System.out.println("\nOK");
    }
}
