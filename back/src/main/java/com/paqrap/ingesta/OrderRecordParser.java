package com.paqrap.ingesta;

import com.paqrap.model.*;

public class OrderRecordParser implements CsvRecordParser<Order> {
    private int lineNumber = 0;

    @Override
    public Order parse(String line) {
        lineNumber++;
        String[] parts = line.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Formato inválido en línea " + lineNumber);
        }

        String timeToken = parts[0];
        String[] data = parts[1].split(",");

        if (data.length != 5) {
            throw new IllegalArgumentException("Formato de datos inválido en línea " + lineNumber);
        }

        double placementTimeHours = TimeConverter.parseHours(timeToken);
        double posX = Double.parseDouble(data[0]);
        double posY = Double.parseDouble(data[1]);
        String idCliente = data[2];
        int cantidad = Integer.parseInt(data[3]);
        double horizonteHoras = Double.parseDouble(data[4]);

        if (cantidad <= 0) {
            throw new IllegalArgumentException("Cantidad inválida (" + cantidad + ") en línea " + lineNumber);
        }

        Location destination = new Location(idCliente, idCliente, posX, posY, Location.LocationType.CUSTOMER);
        String id = "PED-" + lineNumber;

        return new Order(id, destination, cantidad, placementTimeHours, horizonteHoras);
    }
}
