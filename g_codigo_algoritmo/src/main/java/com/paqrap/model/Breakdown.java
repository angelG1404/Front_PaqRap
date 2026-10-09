//CAMBIO ITERACION 01
package com.paqrap.model;

/**
 * Representa una avería imprevista de un vehículo.
 */
public class Breakdown {
    public String vehicleId;
    public double startMomentHours;
    public int type; // Tipo 1, 2 o 3

    public Breakdown(String vehicleId, double startMomentHours, int type) {
        this.vehicleId = vehicleId;
        this.startMomentHours = startMomentHours;
        this.type = type;
    }
}
