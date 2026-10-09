//CAMBIO ITERACION 01
package com.paqrap.model;

/**
 * Representa el mantenimiento programado de un vehículo.
 */
public class Maintenance {
    public String vehicleId;
    public double startTimeHours;
    public double endTimeHours;

    public Maintenance(String vehicleId, double startTimeHours, double endTimeHours) {
        this.vehicleId = vehicleId;
        this.startTimeHours = startTimeHours;
        this.endTimeHours = endTimeHours;
    }
}
