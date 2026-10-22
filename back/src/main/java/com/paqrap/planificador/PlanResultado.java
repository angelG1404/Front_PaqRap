package com.paqrap.planificador;

import com.paqrap.model.Order;
import com.paqrap.model.Route;
import com.paqrap.model.Solution;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Salida del planificador: secuencia de pedidos por vehículo y pedidos que no se pudieron asignar. */
public record PlanResultado(String algoritmo,
                            double planificadoEnHoras,
                            Map<String, List<Order>> rutas,
                            List<Order> noAsignados,
                            double fitness,
                            long duracionMs) {

    static PlanResultado desde(String algoritmo, InstanciaPlanificacion instancia, Solution solucion, long inicioMs) {
        Map<String, List<Order>> rutas = new LinkedHashMap<>();
        for (Route r : solucion.getRoutes()) {
            rutas.put(r.getVehicle().getId(), new ArrayList<>(r.getOrders()));
        }
        return new PlanResultado(algoritmo, instancia.ahoraHoras(), rutas,
                new ArrayList<>(solucion.getUnassignedOrders()), solucion.getFitness(),
                System.currentTimeMillis() - inicioMs);
    }
}
