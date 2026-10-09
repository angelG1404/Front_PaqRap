package com.paqrap.model;

/**
 * Customer Order requiring delivery of product 'P'.
 */
public class Order {
    public enum TrafficLightStatus {
        VERDE("Verde - En tiempo seguro"),
        AMBAR("Ámbar - Alerta de tiempo"),
        ROJO("Rojo - Crítico / En riesgo");

        private final String label;

        TrafficLightStatus(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final String id;
    private final Location destination;
    private final int quantity; // Packages of product P
    private final double placementTimeHours; // Simulation time when order was registered (hours)
    private final double maxDeliveryWindowHours; // 4, 8, 12, 18, or 36 hours
    private double actualDeliveryTimeHours; // Recorded after simulation/route execution
    //CAMBIO ITERACION 01
    private boolean reassigned = false;

    public Order(String id, Location destination, int quantity, double placementTimeHours, double maxDeliveryWindowHours) {
        this.id = id;
        this.destination = destination;
        this.quantity = quantity;
        this.placementTimeHours = placementTimeHours;
        this.maxDeliveryWindowHours = maxDeliveryWindowHours;
        this.actualDeliveryTimeHours = -1; // Unassigned / not delivered yet
    }
    
    //CAMBIO ITERACION 01
    public boolean isReassigned() { return reassigned; }
    public void setReassigned(boolean reassigned) { this.reassigned = reassigned; }

    public String getId() {
        return id;
    }

    public Location getDestination() {
        return destination;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPlacementTimeHours() {
        return placementTimeHours;
    }

    public double getMaxDeliveryWindowHours() {
        return maxDeliveryWindowHours;
    }

    public double getDeadlineHours() {
        return placementTimeHours + maxDeliveryWindowHours;
    }

    public double getActualDeliveryTimeHours() {
        return actualDeliveryTimeHours;
    }

    public void setActualDeliveryTimeHours(double actualDeliveryTimeHours) {
        this.actualDeliveryTimeHours = actualDeliveryTimeHours;
    }

    /**
     * Determines the traffic light status (VERDE, AMBAR, ROJO) based on predicted or actual delivery time.
     */
    public TrafficLightStatus evaluateStatus(double estimatedDeliveryTimeHours) {
        double elapsedTime = estimatedDeliveryTimeHours - placementTimeHours;
        double ratio = elapsedTime / maxDeliveryWindowHours;

        if (ratio <= 0.75) {
            return TrafficLightStatus.VERDE;
        } else if (ratio <= 0.95) {
            return TrafficLightStatus.AMBAR;
        } else {
            return TrafficLightStatus.ROJO;
        }
    }

    public boolean isLATE(double deliveryTimeHours) {
        return deliveryTimeHours > getDeadlineHours() + 1e-5;
    }

    @Override
    public String toString() {
        return String.format("Pedido[%s | Cant: %d | Ventana: %.0fh | Limite: %.1fh]",
                id, quantity, maxDeliveryWindowHours, getDeadlineHours());
    }
}
