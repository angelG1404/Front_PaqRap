package com.paqrap.model;

/**
 * Vehicle instance in PaqRap fleet.
 */
public class Vehicle {
    private final String id;
    private VehicleType type;
    private final Location startDepot;
    //CAMBIO ITERACION 01
    private boolean available = true;
    private Breakdown activeBreakdown = null;

    public Vehicle(String id, VehicleType type, Location startDepot) {
        this.id = id;
        this.type = type;
        this.startDepot = startDepot;
    }
    
    //CAMBIO ITERACION 01
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public Breakdown getActiveBreakdown() { return activeBreakdown; }
    public void setActiveBreakdown(Breakdown breakdown) { this.activeBreakdown = breakdown; }

    public String getId() {
        return id;
    }

    public VehicleType getType() {
        return type;
    }

    public void setType(VehicleType type) {
        this.type = type;
    }

    public Location getStartDepot() {
        return startDepot;
    }

    public int getCapacity() {
        return type.getCapacity();
    }

    public double getSpeedKmH() {
        return type.getSpeedKmH();
    }

    public double getCostPerKm() {
        return type.getCostPerKm();
    }

    @Override
    public String toString() {
        return String.format("Vehiculo[%s | Tipo: %s | Cap: %d | Costo: S/%.2f/km]",
                id, type.getDisplayName(), getCapacity(), getCostPerKm());
    }
}
