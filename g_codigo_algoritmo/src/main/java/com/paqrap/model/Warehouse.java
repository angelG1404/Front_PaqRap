package com.paqrap.model;

/**
 * Warehouse entity in the PaqRap logistics network.
 */
public class Warehouse {
    private final String id;
    private final Location location;
    private final boolean isCentral;
    private final int maxCapacity; // 1,000 for intermediate, infinite for central
    private int currentStock;

    public Warehouse(String id, Location location, boolean isCentral, int maxCapacity) {
        this.id = id;
        this.location = location;
        this.isCentral = isCentral;
        this.maxCapacity = maxCapacity;
        this.currentStock = maxCapacity;
    }

    public String getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isCentral() {
        return isCentral;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public int getCurrentStock() {
        return isCentral ? Integer.MAX_VALUE : currentStock;
    }

    public boolean withdraw(int amount) {
        if (isCentral) {
            return true;
        }
        if (currentStock >= amount) {
            currentStock -= amount;
            return true;
        }
        return false;
    }

    /**
     * Daily instant reload at 23:59:59.
     */
    public void dailyReload() {
        if (!isCentral) {
            this.currentStock = maxCapacity;
        }
    }

    @Override
    public String toString() {
        return String.format("Almacen[%s (%s) | Stock: %s/%s]",
                id, isCentral ? "Central" : "Intermedio",
                isCentral ? "INF" : String.valueOf(currentStock),
                isCentral ? "INF" : String.valueOf(maxCapacity));
    }
}
