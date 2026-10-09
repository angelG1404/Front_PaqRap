package com.paqrap.ingesta;

import com.paqrap.model.Breakdown;

public class BreakdownRecordParser implements CsvRecordParser<Breakdown> {
    private int lineNumber = 0;

    @Override
    public Breakdown parse(String line) {
        lineNumber++;
        String[] parts = line.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Formato inválido en línea " + lineNumber);
        }

        String timeToken = parts[0];
        String[] data = parts[1].split(",");

        if (data.length != 2) {
            throw new IllegalArgumentException("Formato de datos inválido en línea " + lineNumber);
        }

        String vehicleId = data[0];
        int type = Integer.parseInt(data[1]);
        double startMoment = TimeConverter.parseHours(timeToken);

        return new Breakdown(vehicleId, startMoment, type);
    }
}
