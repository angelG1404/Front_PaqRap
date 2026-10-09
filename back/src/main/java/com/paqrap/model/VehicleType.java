package com.paqrap.model;

/**
 * Vehicle types available in PaqRap fleet with capacity, speed, and cost specs.
 */
public enum VehicleType {
    AUTO("Auto", 24, 40.0, 8.00),
    MOTO("Moto", 8, 25.0, 6.00),
    BICI("Bicicleta", 4, 12.0, 3.00);

    private final String displayName;
    private final int capacity; // Packages of P
    private final double speedKmH; // Average speed km/h
    private final double costPerKm; // Operational cost in S/ per km

    VehicleType(String displayName, int capacity, double speedKmH, double costPerKm) {
        this.displayName = displayName;
        this.capacity = capacity;
        this.speedKmH = speedKmH;
        this.costPerKm = costPerKm;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getSpeedKmH() {
        return speedKmH;
    }

    public double getCostPerKm() {
        return costPerKm;
    }

    /**
     * Travel time in hours for a given distance in km.
     */
    public double travelTimeHours(double distanceKm) {
        return distanceKm / speedKmH;
    }
}
