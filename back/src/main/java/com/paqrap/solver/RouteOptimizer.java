package com.paqrap.solver;

import com.paqrap.model.Solution;
import java.util.List;

/**
 * Common interface for PaqRap Vehicle Routing Problem (VRP) metaheuristic solvers.
 */
public interface RouteOptimizer {

    /**
     * Optimizes an initial delivery routing solution.
     *
     * @param initialSolution Initial solution (e.g. built by Earliest Deadline First greedy heuristic)
     * @return Best optimized solution found by the algorithm
     */
    Solution solve(Solution initialSolution);

    /**
     * Human-readable name of the metaheuristic algorithm (e.g., "ATS", "ALNS").
     */
    String getAlgorithmName();

    /**
     * Execution execution and adaptation trace logs.
     */
    List<String> getLogs();
}
