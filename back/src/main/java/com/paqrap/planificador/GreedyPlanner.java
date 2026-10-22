package com.paqrap.planificador;

import com.paqrap.model.Solution;

/** Respaldo de la arquitectura: inserción EDF sin mejora metaheurística. */
public class GreedyPlanner implements PlanningAlgorithm {

    @Override
    public String nombre() {
        return "GREEDY";
    }

    @Override
    public PlanResultado planificar(InstanciaPlanificacion instancia, long limiteMs) {
        long inicio = System.currentTimeMillis();
        Solution solucion = ConstructorSolucionInicial.construir(instancia);
        return PlanResultado.desde(nombre(), instancia, solucion, inicio);
    }
}
