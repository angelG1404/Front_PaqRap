package com.paqrap.model;

/**
 * Represents a geographical location (node) in the PaqRap logistics network.
 */
public class Location {
    public enum LocationType {
        CENTRAL_WAREHOUSE,
        INTERMEDIATE_WAREHOUSE,
        CUSTOMER
    }

    private final String id;
    private final String name;
    private final double x; // X coordinate in km
    private final double y; // Y coordinate in km
    private final LocationType type;

    public Location(String id, String name, double x, double y, LocationType type) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public LocationType getType() {
        return type;
    }

    /**
     * Calculates Euclidean distance in kilometers to another location.
     */
    public double distanceTo(Location other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public String toString() {
        return String.format("%s (%s, [%.1f, %.1f])", name, id, x, y);
    }
}
