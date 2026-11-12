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
    /** Capacidad por tipo definida para la corrida; null usa la capacidad base de {@link VehicleType}. */
    private java.util.Map<VehicleType, Integer> capacities;

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
        if (capacities != null) {
            Integer configurada = capacities.get(type);
            if (configurada != null) return configurada;
        }
        return type.getCapacity();
    }

    public java.util.Map<VehicleType, Integer> getCapacities() {
        return capacities;
    }

    public void setCapacities(java.util.Map<VehicleType, Integer> capacities) {
        this.capacities = capacities;
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
