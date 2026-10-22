package com.paqrap.planificador;

import com.paqrap.model.*;
import com.paqrap.solver.SolutionEvaluator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Solución inicial por inserción EDF (Earliest Deadline First) que respeta la hora y el almacén
 * desde los que cada vehículo queda disponible. A diferencia de {@code InitialSolutionBuilder},
 * evalúa cada inserción con la llegada real de la ruta para priorizar la entrega en plazo.
 */
final class ConstructorSolucionInicial {

    /** Peso de una hora de retraso frente a una hora de llegada (prioriza cumplir el plazo). */
    private static final double PENALIDAD_HORA_TARDE = 1_000.0;

    private ConstructorSolucionInicial() {
    }

    static Solution construir(InstanciaPlanificacion instancia) {
        List<Route> rutas = new ArrayList<>();
        for (VehiculoDisponible v : instancia.vehiculos()) {
            Location.LocationType tipo = "CENTRAL".equals(v.almacenId())
                    ? Location.LocationType.CENTRAL_WAREHOUSE : Location.LocationType.INTERMEDIATE_WAREHOUSE;
            Location deposito = new Location(v.almacenId(), v.almacenId(), v.x(), v.y(), tipo);
            Route ruta = new Route(new Vehicle(v.id(), v.tipo(), deposito), deposito, v.disponibleDesdeHoras());
            ruta.setRoadblocks(instancia.bloqueos());
            rutas.add(ruta);
        }

        List<Order> ordenados = new ArrayList<>(instancia.pedidos());
        ordenados.sort(Comparator.comparingDouble(Order::getDeadlineHours));
        List<Order> sinAsignar = new ArrayList<>();

        for (Order pedido : ordenados) {
            Route mejorRuta = null;
            int mejorPosicion = -1;
            double mejorDelta = Double.MAX_VALUE;
            for (Route ruta : rutas) {
                if (ruta.getTotalDemand() + pedido.getQuantity() > ruta.getVehicle().getCapacity()) continue;
                double base = costo(ruta);
                for (int pos = 0; pos <= ruta.getOrders().size(); pos++) {
                    ruta.addOrderAt(pos, pedido);
                    double delta = costo(ruta) - base;
                    ruta.removeOrderAt(pos);
                    if (delta < mejorDelta) {
                        mejorDelta = delta;
                        mejorRuta = ruta;
                        mejorPosicion = pos;
                    }
                }
            }
            if (mejorRuta == null) sinAsignar.add(pedido);
            else mejorRuta.addOrderAt(mejorPosicion, pedido);
        }

        Solution solucion = new Solution(rutas, sinAsignar);
        new SolutionEvaluator().evaluate(solucion);
        return solucion;
    }

    private static double costo(Route ruta) {
        if (ruta.getOrders().isEmpty()) return 0.0;
        double[] llegadas = new double[ruta.getOrders().size()];
        double horasTarde = ruta.calculateDeliveryTimesAndLateCount(llegadas);
        double suma = 0.0;
        for (double llegada : llegadas) suma += llegada;
        return horasTarde * PENALIDAD_HORA_TARDE + suma;
    }
}
