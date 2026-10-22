package com.paqrap.planificador;

/** Instancia el algoritmo según el parámetro {@code algoritmo} de la corrida (ALNS, ATS o GREEDY). */
public final class AlgorithmSelector {

    private AlgorithmSelector() {
    }

    public static PlanningAlgorithm seleccionar(String nombre, int maxIteraciones) {
        String clave = nombre == null ? "ALNS" : nombre.trim().toUpperCase();
        return switch (clave) {
            case "ATS" -> new AtsPlanner(maxIteraciones);
            case "GREEDY" -> new GreedyPlanner();
            default -> new AlnsPlanner(maxIteraciones);
        };
    }
}
