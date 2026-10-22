package com.paqrap.planificador;

import com.paqrap.model.Solution;
import com.paqrap.solver.ALNSOptimizer;

import java.util.List;

/** Adapta {@link ALNSOptimizer} al contrato del planificador con un límite de tiempo real. */
public class AlnsPlanner implements PlanningAlgorithm {

    private final int maxIteraciones;

    public AlnsPlanner(int maxIteraciones) {
        this.maxIteraciones = maxIteraciones;
    }

    @Override
    public String nombre() {
        return "ALNS";
    }

    @Override
    public PlanResultado planificar(InstanciaPlanificacion instancia, long limiteMs) {
        long inicio = System.currentTimeMillis();
        Solution inicial = ConstructorSolucionInicial.construir(instancia);
        if (inicial.getTotalServedOrders() == 0) return PlanResultado.desde(nombre(), instancia, inicial, inicio);

        ALNSOptimizer.ALNSConfig config = new ALNSOptimizer.ALNSConfig();
        config.maxIterations = maxIteraciones;
        config.maxTimeMs = Math.max(1, limiteMs - (System.currentTimeMillis() - inicio));
        config.verboseLogging = false;
        Solution optimizada = new ALNSOptimizer(config).solve(inicial, List.of(), List.of(), instancia.bloqueos());
        return PlanResultado.desde(nombre(), instancia, optimizada, inicio);
    }
}
