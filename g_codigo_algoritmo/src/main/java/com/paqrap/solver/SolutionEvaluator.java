package com.paqrap.solver;


import com.paqrap.model.Solution;

/**
 * Evaluates solution quality based on the new fitness function (suma de horas de llegada).
 */
public class SolutionEvaluator {

    /**
     * Calculates the objective fitness function value based on sum of arrival hours. Lower is better.
     */
    public double evaluate(Solution solution) {
        //CAMBIO ITERACION 01: Eliminadas penalizaciones. Usando nuevo fitness.
        solution.calculateFitness();
        return solution.getFitness();
    }
}
