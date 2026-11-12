package com.paqrap.planificador;

import com.paqrap.model.Order;
import com.paqrap.model.Roadblock;
import com.paqrap.model.VehicleType;

import java.util.List;
import java.util.Map;

/**
 * Copia inmutable del estado que necesita el algoritmo (ProblemInstance de la arquitectura).
 * Los {@link Order} se comparten por referencia porque los algoritmos solo leen sus campos finales.
 */
public record InstanciaPlanificacion(double ahoraHoras,
                                     List<VehiculoDisponible> vehiculos,
                                     List<Order> pedidos,
                                     List<Roadblock> bloqueos,
                                     Map<VehicleType, Integer> capacidades) {

    public InstanciaPlanificacion(double ahoraHoras, List<VehiculoDisponible> vehiculos, List<Order> pedidos, List<Roadblock> bloqueos) {
        this(ahoraHoras, vehiculos, pedidos, bloqueos, Map.of());
    }

    public InstanciaPlanificacion {
        capacidades = Map.copyOf(capacidades);
        vehiculos = List.copyOf(vehiculos);
        pedidos = List.copyOf(pedidos);
        bloqueos = List.copyOf(bloqueos);
    }
}
