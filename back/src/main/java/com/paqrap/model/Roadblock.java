//CAMBIO ITERACION 01
package com.paqrap.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa un bloqueo vial en la grilla.
 */
public class Roadblock {
    private static final java.util.concurrent.atomic.AtomicLong SECUENCIA = new java.util.concurrent.atomic.AtomicLong();
    /** Identificador único del bloqueo; el router lo usa para recordar distancias por conjunto de bloqueos vigentes. */
    public final long uid = SECUENCIA.incrementAndGet();
    public List<Location> path; // Secuencia de nodos bloqueados
    /** Rectángulo que contiene todos los nodos bloqueados; permite descartar un bloqueo lejano sin recorrer su camino. */
    public final int minX, maxX, minY, maxY;
    public double startTimeHours;
    public double endTimeHours;

    public Roadblock(List<Location> path, double startTimeHours, double endTimeHours) {
        this.path = decompose(path);
        int x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y0 = Integer.MAX_VALUE, y1 = Integer.MIN_VALUE;
        for (Location loc : this.path) {
            int x = (int) Math.round(loc.getX()), y = (int) Math.round(loc.getY());
            x0 = Math.min(x0, x); x1 = Math.max(x1, x);
            y0 = Math.min(y0, y); y1 = Math.max(y1, y);
        }
        this.minX = x0; this.maxX = x1; this.minY = y0; this.maxY = y1;
        this.startTimeHours = startTimeHours;
        this.endTimeHours = endTimeHours;
    }

    private List<Location> decompose(List<Location> path) {
        List<Location> fullPath = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            Location p1 = path.get(i);
            Location p2 = path.get(i + 1);
            int x1 = (int) Math.round(p1.getX());
            int y1 = (int) Math.round(p1.getY());
            int x2 = (int) Math.round(p2.getX());
            int y2 = (int) Math.round(p2.getY());
            
            if (x1 == x2) { // Vertical
                int min = Math.min(y1, y2);
                int max = Math.max(y1, y2);
                for (int y = min; y <= max; y++) {
                    fullPath.add(new Location("BLOCKED", "Blocked", x1, y, Location.LocationType.CUSTOMER));
                }
            } else if (y1 == y2) { // Horizontal
                int min = Math.min(x1, x2);
                int max = Math.max(x1, x2);
                for (int x = min; x <= max; x++) {
                    fullPath.add(new Location("BLOCKED", "Blocked", x, y1, Location.LocationType.CUSTOMER));
                }
            } else {
                throw new IllegalArgumentException("Tramo diagonal no soportado: " + p1 + " a " + p2);
            }
        }
        return fullPath;
    }
}
