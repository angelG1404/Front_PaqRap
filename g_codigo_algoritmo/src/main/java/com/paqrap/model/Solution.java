package com.paqrap.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Full operational solution containing a collection of vehicle routes.
 */
public class Solution {
    private final List<Route> routes;
    private final List<Order> unassignedOrders;
    private double fitness; // Objective evaluation (cost + penalties)

    public Solution() {
        this.routes = new ArrayList<>();
        this.unassignedOrders = new ArrayList<>();
        this.fitness = Double.MAX_VALUE;
    }

    public Solution(List<Route> routes, List<Order> unassignedOrders) {
        this.routes = new ArrayList<>();
        for (Route r : routes) {
            this.routes.add(r.cloneRoute());
        }
        this.unassignedOrders = new ArrayList<>(unassignedOrders);
        this.fitness = Double.MAX_VALUE;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public List<Order> getUnassignedOrders() {
        return unassignedOrders;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    //CAMBIO ITERACION 01
    /**
     * Valida si la solución cumple con las restricciones duras.
     */
    public boolean isFeasible(List<Maintenance> maintenances, List<Breakdown> breakdowns) {
        for (Route r : routes) {
            // Si la ruta no tiene pedidos, el vehículo está libre, así que es factible
            if (r.getOrders().isEmpty()) continue;

            // 1. Capacidad
            if (r.isCapacityExceeded()) return false;

            // 2. Disponibilidad Vehículo (Averías/Mantenimiento)
            Vehicle v = r.getVehicle();
            if (!v.isAvailable() || v.getActiveBreakdown() != null) return false;
            for (Maintenance m : maintenances) {
                if (m.vehicleId.equals(v.getId())) return false; // Simple check
            }
            
            // 3. Ventanas de Tiempo (usando método existente en Route)
            double[] deliveryTimes = new double[r.getOrders().size()];
            double lateHours = r.calculateDeliveryTimesAndLateCount(deliveryTimes);
            if (lateHours > 0) return false;
        }
        return true;
    }

    //CAMBIO ITERACION 01
    /**
     * Calcula el fitness: suma de horas de llegada de cada pedido.
     */
    public void calculateFitness() {
        double totalArrivalHours = 0.0;
        //int vehiclesUsed = 0;
        
        for (Route r : routes) {
            //if (!r.getOrders().isEmpty()) {
            //    vehiclesUsed++; // Solo contamos vehículos con carga
                
                double[] deliveryTimes = new double[r.getOrders().size()];
                r.calculateDeliveryTimesAndLateCount(deliveryTimes);
                for (double time : deliveryTimes) {
                    totalArrivalHours += time;
                }
            }
        
        
        // Penalización por vehículo usado. 
        //double vehiclePenalty = vehiclesUsed * 5.0; 
        
        this.fitness = totalArrivalHours;
    }

    public double getTotalDistanceKm() {
        double dist = 0.0;
        for (Route r : routes) {
            dist += r.getTotalDistanceKm();
        }
        return dist;
    }

    public double getTotalMonetaryCost() {
        double cost = 0.0;
        for (Route r : routes) {
            cost += r.getCost();
        }
        return cost;
    }

    public int getTotalServedOrders() {
        int count = 0;
        for (Route r : routes) {
            count += r.getOrders().size();
        }
        return count;
    }

    public Solution cloneSolution() {
        Solution copy = new Solution(this.routes, this.unassignedOrders);
        copy.fitness = this.fitness;
        return copy;
    }

    @Override
    public String toString() {
        return String.format("Solucion[Rutas: %d | Atendidos: %d | Sin Asignar: %d | Dist: %.2f km | Costo Operativo: S/%.2f | Fitness: %.2f]",
                routes.size(), getTotalServedOrders(), unassignedOrders.size(),
                getTotalDistanceKm(), getTotalMonetaryCost(), fitness);
    }
}
