package com.paqrap.planificador;

import com.paqrap.model.VehicleType;

/**
 * Vehículo que puede recibir una ruta nueva: está libre en un almacén o regresando a uno.
 * La ruta planificada parte del almacén {@code almacenId} en {@code disponibleDesdeHoras}.
 */
public record VehiculoDisponible(String id, VehicleType tipo, String almacenId, int x, int y,
                                 double disponibleDesdeHoras) {
}
