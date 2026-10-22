package com.paqrap.planificador;

import com.paqrap.model.Solution;
import com.paqrap.solver.ATSOptimizer;

import java.util.List;

/** Adapta {@link ATSOptimizer} al contrato del planificador con un límite de tiempo real. */
public class AtsPlanner implements PlanningAlgorithm {

    private final int maxIteraciones;

    public AtsPlanner(int maxIteraciones) {
        this.maxIteraciones = maxIteraciones;
    }

    @Override
    public String nombre() {
        return "ATS";
    }

    @Override
    public PlanResultado planificar(InstanciaPlanificacion instancia, long limiteMs) {
        long inicio = System.currentTimeMillis();
        Solution inicial = ConstructorSolucionInicial.construir(instancia);
        if (inicial.getTotalServedOrders() == 0) return PlanResultado.desde(nombre(), instancia, inicial, inicio);

        ATSOptimizer.ATSConfig config = new ATSOptimizer.ATSConfig();
        config.maxIterations = maxIteraciones;
        config.maxTimeMs = Math.max(1, limiteMs - (System.currentTimeMillis() - inicio));
        config.verboseLogging = false;
        Solution optimizada = new ATSOptimizer(config).solve(inicial, List.of(), List.of(), instancia.bloqueos());
        return PlanResultado.desde(nombre(), instancia, optimizada, inicio);
    }
}
