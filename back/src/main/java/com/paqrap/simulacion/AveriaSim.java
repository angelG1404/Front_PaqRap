package com.paqrap.simulacion;

/**
 * Avería de una unidad dentro de una corrida (entidad Averia del documento de estructura de datos).
 * Solo el tipo y la unidad los elige el operador; el resto de atributos los fija o actualiza el motor.
 */
final class AveriaSim {

    enum Estado { ACTIVA, RESUELTA }

    final String id;
    final String vehiculoId;
    final int tipo;
    final double ocurrenciaHoras;
    /** Fin de la indisponibilidad; lo calcula el motor según el tipo (ver {@code SimulationEngine.registrarAveria}). */
    final double finHoras;
    /** Hora en que la unidad se remolca al almacén central; negativo si el tipo no lo requiere (tipo 1). */
    final double remolqueHoras;
    final int origenX;
    final int origenY;
    final int pedidosReasignados;

    Estado estado = Estado.ACTIVA;
    /** Ubicación actual de la unidad averiada: el lugar de la avería hasta el remolque, luego el almacén central. */
    int x;
    int y;
    boolean remolcada;
    double resueltaHoras = -1;

    AveriaSim(String id, String vehiculoId, int tipo, double ocurrenciaHoras, double finHoras, double remolqueHoras,
              int x, int y, int pedidosReasignados) {
        this.id = id;
        this.vehiculoId = vehiculoId;
        this.tipo = tipo;
        this.ocurrenciaHoras = ocurrenciaHoras;
        this.finHoras = finHoras;
        this.remolqueHoras = remolqueHoras;
        this.origenX = x;
        this.origenY = y;
        this.x = x;
        this.y = y;
        this.pedidosReasignados = pedidosReasignados;
    }

    boolean activa() {
        return estado == Estado.ACTIVA;
    }
}
