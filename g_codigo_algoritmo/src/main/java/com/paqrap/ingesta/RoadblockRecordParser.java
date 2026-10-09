package com.paqrap.ingesta;

import com.paqrap.model.*;
import java.util.ArrayList;
import java.util.List;

public class RoadblockRecordParser implements CsvRecordParser<Roadblock> {
    private int lineNumber = 0;

    @Override
    public Roadblock parse(String line) {
        lineNumber++;
        String[] parts = line.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Formato inválido en línea " + lineNumber);
        }

        double[] timeRange = TimeConverter.parseRange(parts[0]);
        String[] coords = parts[1].split(",");

        if (coords.length < 2 || coords.length % 2 != 0) {
            throw new IllegalArgumentException("Formato de coordenadas inválido en línea " + lineNumber);
        }

        List<Location> path = new ArrayList<>();
        for (int i = 0; i < coords.length; i += 2) {
            double x = Double.parseDouble(coords[i]);
            double y = Double.parseDouble(coords[i + 1]);
            path.add(new Location("BLOCKED", "Blocked", x, y, Location.LocationType.CUSTOMER));
        }

        return new Roadblock(path, timeRange[0], timeRange[1]);
    }
}
