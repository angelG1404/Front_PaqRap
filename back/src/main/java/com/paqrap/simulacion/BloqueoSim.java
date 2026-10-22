package com.paqrap.simulacion;

import com.paqrap.ingesta.RoadblockRecordParser;
import com.paqrap.model.Location;
import com.paqrap.model.Roadblock;

import java.util.ArrayList;
import java.util.List;

/** Bloqueo vial: polilínea original (para dibujarla) y su descomposición en nodos (para rutear). */
public final class BloqueoSim {

    final String id;
    final List<int[]> vertices;
    final List<int[]> nodos;
    final Roadblock roadblock;

    /** Línea del archivo de bloqueos: DDdHHhMMm-DDdHHhMMm:x1,y1,x2,y2,... (tiempos relativos al día 01). */
    public static BloqueoSim desdeLinea(String id, String linea) {
        Roadblock roadblock = new RoadblockRecordParser().parse(linea.trim());
        String[] coords = linea.trim().split(":")[1].split(",");
        List<int[]> vertices = new ArrayList<>();
        for (int i = 0; i + 1 < coords.length; i += 2) {
            vertices.add(new int[]{Integer.parseInt(coords[i].trim()), Integer.parseInt(coords[i + 1].trim())});
        }
        return new BloqueoSim(id, roadblock, vertices);
    }

    public BloqueoSim(String id, Roadblock roadblock, List<int[]> vertices) {
        this.id = id;
        this.roadblock = roadblock;
        this.vertices = List.copyOf(vertices);
        List<int[]> n = new ArrayList<>();
        for (Location loc : roadblock.path) n.add(new int[]{(int) Math.round(loc.getX()), (int) Math.round(loc.getY())});
        this.nodos = List.copyOf(n);
    }

    public List<int[]> vertices() {
        return vertices;
    }

    public double inicioHoras() {
        return roadblock.startTimeHours;
    }

    public double finHoras() {
        return roadblock.endTimeHours;
    }

    boolean vigente(double horas) {
        return horas >= inicioHoras() && horas < finHoras();
    }
}
