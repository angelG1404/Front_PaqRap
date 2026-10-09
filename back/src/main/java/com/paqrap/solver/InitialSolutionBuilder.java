package com.paqrap.solver;

import com.paqrap.model.*;

import java.util.*;

/**
 * Generates an initial feasible solution using Earliest Deadline First (EDF) greedy heuristic.
 */
public class InitialSolutionBuilder {

    public Solution buildInitialSolution(List<Order> orders, List<Vehicle> fleet, List<Warehouse> warehouses, double startTimeHours) {
        // Sort orders by urgency (earliest deadline first)
        List<Order> sortedOrders = new ArrayList<>(orders);
        sortedOrders.sort(Comparator.comparingDouble(Order::getDeadlineHours));

        List<Route> routes = new ArrayList<>();
        List<Order> unassigned = new ArrayList<>();

        // Map warehouses
        Warehouse central = warehouses.stream().filter(Warehouse::isCentral).findFirst().orElse(warehouses.get(0));

        // Create initial empty routes for available vehicles
        //CAMBIO ITERACION 01
        for (Vehicle v : fleet) {
            if (!v.isAvailable() || v.getActiveBreakdown() != null) continue;
            Location depot = (v.getStartDepot() != null) ? v.getStartDepot() : central.getLocation();
            routes.add(new Route(v, depot, startTimeHours));
        }

        // Assign orders iteratively to the vehicle route that minimizes marginal travel distance & cost while keeping capacity valid
        for (Order order : sortedOrders) {
            boolean assigned = false;
            double bestInsertionCost = Double.MAX_VALUE;
            Route bestRoute = null;
            int bestIndex = -1;

            for (Route r : routes) {
                //CAMBIO ITERACION 01: Verificación extra de disponibilidad
                if (!r.getVehicle().isAvailable() || r.getVehicle().getActiveBreakdown() != null) continue;
                
                if (r.getTotalDemand() + order.getQuantity() <= r.getVehicle().getCapacity()) {
                    // Try all insertion positions in route r
                    List<Order> currOrders = r.getOrders();
                    for (int i = 0; i <= currOrders.size(); i++) {
                        double deltaDist = calculateInsertionDeltaDistance(r, i, order);
                        double deltaCost = deltaDist * r.getVehicle().getCostPerKm();

                        if (deltaCost < bestInsertionCost) {
                            bestInsertionCost = deltaCost;
                            bestRoute = r;
                            bestIndex = i;
                        }
                    }
                }
            }

            if (bestRoute != null && bestIndex >= 0) {
                bestRoute.addOrderAt(bestIndex, order);
                assigned = true;
            } 
            /*else {
                // Si no hay buena inserción, buscar el primer vehículo vacío disponible
                for (Route r : routes) {
                    if (r.getOrders().isEmpty() && r.getVehicle().isAvailable()) {
                        r.addOrder(order);
                        assigned = true;
                        break;
                    }
                }
            }*/

            if (!assigned) {
                // If no vehicle has capacity, add to unassigned list
                unassigned.add(order);
            }
        }

        Solution solution = new Solution(routes, unassigned);
        SolutionEvaluator evaluator = new SolutionEvaluator();
        evaluator.evaluate(solution);
        return solution;
    }

    private double calculateInsertionDeltaDistance(Route route, int index, Order newOrder) {
        Location depot = route.getDepot();
        List<Order> curr = route.getOrders();

        Location prevLoc = (index == 0) ? depot : curr.get(index - 1).getDestination();
        Location nextLoc = (index == curr.size()) ? depot : curr.get(index).getDestination();

        double originalDist = prevLoc.distanceTo(nextLoc);
        double newDist = prevLoc.distanceTo(newOrder.getDestination()) + newOrder.getDestination().distanceTo(nextLoc);

        return newDist - originalDist;
    }
}
