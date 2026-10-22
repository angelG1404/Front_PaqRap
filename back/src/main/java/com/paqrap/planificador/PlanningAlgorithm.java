package com.paqrap.planificador;

/**
 * Strategy del planificador: recibe una foto inmutable del estado de la operación y devuelve,
 * por vehículo, la secuencia ordenada de pedidos a entregar. El motor de simulación decide
 * cuándo invocarlo y aplica el resultado; el algoritmo nunca toca el estado vivo.
 */
public interface PlanningAlgorithm {

    String nombre();

    PlanResultado planificar(InstanciaPlanificacion instancia, long limiteMs);
}
