package com.paqrap.planificador;

import com.paqrap.model.Order;
import com.paqrap.model.Roadblock;

import java.util.List;

/**
 * Copia inmutable del estado que necesita el algoritmo (ProblemInstance de la arquitectura).
 * Los {@link Order} se comparten por referencia porque los algoritmos solo leen sus campos finales.
 */
public record InstanciaPlanificacion(double ahoraHoras,
                                     List<VehiculoDisponible> vehiculos,
                                     List<Order> pedidos,
                                     List<Roadblock> bloqueos) {

    public InstanciaPlanificacion {
        vehiculos = List.copyOf(vehiculos);
        pedidos = List.copyOf(pedidos);
        bloqueos = List.copyOf(bloqueos);
    }
}
