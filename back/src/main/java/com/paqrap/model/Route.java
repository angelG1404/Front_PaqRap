package com.paqrap.model;

import com.paqrap.util.AStarGridRouter; //CAMBIO ITERACION 01
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single delivery route performed by a vehicle.
 */
public class Route {
    private final Vehicle vehicle;
    private final Location depot;
    private final List<Order> orders;
    private final double startTimeHours;
    //CAMBIO ITERACION 01: Necesitamos acceso a restricciones de escenario
    private List<Roadblock> roadblocks = new ArrayList<>(); 

    public Route(Vehicle vehicle, Location depot, double startTimeHours) {
        this.vehicle = vehicle;
        this.depot = depot;
        this.orders = new ArrayList<>();
        this.startTimeHours = startTimeHours;
    }

    public Route(Vehicle vehicle, Location depot, double startTimeHours, List<Order> orders) {
        this.vehicle = vehicle;
        this.depot = depot;
        this.startTimeHours = startTimeHours;
        this.orders = new ArrayList<>(orders);
    }

    //CAMBIO ITERACION 01
    public void setRoadblocks(List<Roadblock> roadblocks) { this.roadblocks = roadblocks; }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Location getDepot() {
        return depot;
    }

    public double getStartTimeHours() {
        return startTimeHours;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void addOrder(Order order) {
        orders.add(order);
    }

    public void addOrderAt(int index, Order order) {
        orders.add(index, order);
    }

    public Order removeOrderAt(int index) {
        return orders.remove(index);
    }

    public boolean removeOrder(Order order) {
        return orders.remove(order);
    }

    public int getTotalDemand() {
        int sum = 0;
        for (Order o : orders) {
            sum += o.getQuantity();
        }
        return sum;
    }

    public boolean isCapacityExceeded() {
        return getTotalDemand() > vehicle.getCapacity();
    }

    /**
     * Calculates total distance traveled in kilometers.
     */
    public double getTotalDistanceKm() {
        if (orders.isEmpty()) {
            return 0.0;
        }

        double distance = 0.0;
        Location currentLoc = depot;
        AStarGridRouter router = new AStarGridRouter(71, 51);

        for (Order order : orders) {
            //CAMBIO ITERACION 01
            distance += router.calculateDistance(currentLoc, order.getDestination(), roadblocks, 0.0);
            currentLoc = order.getDestination();
        }

        // Return to depot
        //CAMBIO ITERACION 01
        distance += router.calculateDistance(currentLoc, depot, roadblocks, 0.0);
        return distance;
    }

    /**
     * Calculates monetary cost in Soles (S/).
     */
    public double getCost() {
        return getTotalDistanceKm() * vehicle.getCostPerKm();
    }

    /**
     * Calculates arrival time and delivery timestamp for each order.
     * Service time per customer = 1.0 hour.
     */
    public double calculateDeliveryTimesAndLateCount(double[] deliveryTimesOut) {
        double currentTime = startTimeHours;
        Location currentLoc = depot;
        double totalLateHours = 0.0;
        AStarGridRouter router = new AStarGridRouter(71, 51);

        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            //CAMBIO ITERACION 01
            double dist = router.calculateDistance(currentLoc, order.getDestination(), roadblocks, currentTime);
            double travelTime = vehicle.getType().travelTimeHours(dist);

            currentTime += travelTime; // Travel to customer

            if (deliveryTimesOut != null && i < deliveryTimesOut.length) {
                deliveryTimesOut[i] = currentTime;
            }

            // Check deadline
            double deadline = order.getDeadlineHours();
            if (currentTime > deadline) {
                totalLateHours += (currentTime - deadline);
            }

            // Unloading / Delivery service time: 1 hour at recipient
            currentTime += 1.0;
            currentLoc = order.getDestination();
        }

        return totalLateHours;
    }

    public Route cloneRoute() {
        Vehicle clonedVehicle = new Vehicle(this.vehicle.getId(), this.vehicle.getType(), this.vehicle.getStartDepot());
        return new Route(clonedVehicle, this.depot, this.startTimeHours, new ArrayList<>(this.orders));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Ruta[%s | %s | Pedidos: %d | Carga: %d/%d | Dist: %.2f km | Costo: S/%.2f]",
                vehicle.getId(), vehicle.getType().getDisplayName(), orders.size(),
                getTotalDemand(), vehicle.getCapacity(), getTotalDistanceKm(), getCost()));
        return sb.toString();
    }
}
