package com.paqrap.simulacion;

/**
 * Parámetros de un escenario. Los tres escenarios usan el mismo motor: solo cambian la velocidad
 * del reloj, la frecuencia de planificación y la condición de parada.
 *
 * @param factorVelocidad  minutos simulados por minuto real (1 = tiempo real)
 * @param intervaloPlanMin minutos simulados mínimos entre dos planificaciones por lotes
 * @param limitePlanMs     tiempo real máximo de cada ejecución del algoritmo
 */
public record ScenarioConfig(String tipo,
                             double factorVelocidad,
                             double intervaloPlanMin,
                             long limitePlanMs,
                             int maxIteraciones,
                             int maxPedidosPorPlan,
                             double verdeMinPct,
                             double ambarMinPct) {

    public static final String DIARIO = "DIARIO";
    public static final String SIM_5D = "5D";
    public static final String COLAPSO = "COLAPSO";

    boolean esDiario() {
        return DIARIO.equals(tipo);
    }

    boolean es5D() {
        return SIM_5D.equals(tipo);
    }

    boolean esColapso() {
        return COLAPSO.equals(tipo);
    }
}
