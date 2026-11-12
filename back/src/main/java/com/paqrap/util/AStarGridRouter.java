package com.paqrap.util;

import com.paqrap.model.Location;
import com.paqrap.model.Roadblock;
import java.util.*;

/**
 * Implementación de A* para navegación en grilla ortogonal con bloqueos viales.
 */
public class AStarGridRouter {

    /**
     * Distancias ya calculadas, por conjunto de bloqueos vigentes y par origen-destino. Las metaheurísticas evalúan
     * miles de rutas que repiten los mismos pocos tramos (pedidos y almacenes), así que casi todo es un acierto.
     * Se vacía al superar {@link #MAX_ENTRADAS} para acotar la memoria.
     */
    private static final java.util.Map<Long, java.util.Map<Integer, Double>> CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicInteger ENTRADAS = new java.util.concurrent.atomic.AtomicInteger();
    private static final int MAX_ENTRADAS = 400_000;

    private final int gridRows;
    private final int gridCols;

    public AStarGridRouter(int gridRows, int gridCols) {
        this.gridRows = gridRows;
        this.gridCols = gridCols;
    }

    private static class Node implements Comparable<Node> {
        int x, y;
        double g, h;

        Node(int x, int y, double g, double h) {
            this.x = x;
            this.y = y;
            this.g = g;
            this.h = h;
        }

        double f() { return g + h; }

        @Override
        public int compareTo(Node o) {
            return Double.compare(this.f(), o.f());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Node)) return false;
            Node node = (Node) o;
            return x == node.x && y == node.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    /**
     * Distancia en nodos entre dos ubicaciones evitando los bloqueos vigentes en {@code currentTime}.
     * Es el camino caliente de las metaheurísticas (se llama por cada tramo de cada ruta evaluada), así que:
     * primero se descarta sin asignar memoria que algún bloqueo vigente caiga dentro del rectángulo origen-destino
     * (entonces la distancia Manhattan es exacta) y, solo si cae, se ejecuta un A* sobre arreglos primitivos.
     */
    public double calculateDistance(Location start, Location end, List<Roadblock> roadblocks, double currentTime) {
        int startX = (int) Math.round(start.getX());
        int startY = (int) Math.round(start.getY());
        int endX = (int) Math.round(end.getX());
        int endY = (int) Math.round(end.getY());
        int minX = Math.min(startX, endX), maxX = Math.max(startX, endX);
        int minY = Math.min(startY, endY), maxY = Math.max(startY, endY);

        boolean bloqueoEnElCamino = false;
        long firma = 17; // identifica el conjunto de bloqueos vigentes (los uid son únicos)
        for (Roadblock rb : roadblocks) {
            if (currentTime < rb.startTimeHours || currentTime > rb.endTimeHours) continue;
            firma = (firma ^ rb.uid) * 0x9E3779B97F4A7C15L + (firma >>> 29);
            if (bloqueoEnElCamino || rb.maxX < minX || rb.minX > maxX || rb.maxY < minY || rb.minY > maxY) continue;
            for (Location loc : rb.path) {
                int bx = (int) Math.round(loc.getX());
                int by = (int) Math.round(loc.getY());
                if (bx >= minX && bx <= maxX && by >= minY && by <= maxY) {
                    bloqueoEnElCamino = true;
                    break;
                }
            }
        }
        if (!bloqueoEnElCamino) return (maxX - minX) + (maxY - minY);

        // Fuera de la grilla (no debería ocurrir) la clave empaquetada no es única: se calcula sin recordar.
        if (startX < 0 || startX > 127 || endX < 0 || endX > 127 || startY < 0 || startY > 63 || endY < 0 || endY > 63) {
            return distanciaConRodeo(startX, startY, endX, endY, roadblocks, currentTime);
        }
        int clave = (((startX * 64 + startY) * 128 + endX) * 64 + endY);
        java.util.Map<Integer, Double> porFirma = CACHE.computeIfAbsent(firma, f -> new java.util.concurrent.ConcurrentHashMap<>());
        Double guardada = porFirma.get(clave);
        if (guardada != null) return guardada;
        double distancia = distanciaConRodeo(startX, startY, endX, endY, roadblocks, currentTime);
        if (ENTRADAS.incrementAndGet() > MAX_ENTRADAS) {
            CACHE.clear();
            ENTRADAS.set(0);
        } else {
            porFirma.put(clave, distancia);
        }
        return distancia;
    }

    /** A* sobre arreglos primitivos para cuando algún bloqueo vigente cae dentro del rectángulo origen-destino. */
    private double distanciaConRodeo(int startX, int startY, int endX, int endY, List<Roadblock> roadblocks, double currentTime) {

        boolean[] bloqueado = new boolean[gridRows * gridCols];
        for (Roadblock rb : roadblocks) {
            if (currentTime < rb.startTimeHours || currentTime > rb.endTimeHours) continue;
            for (Location loc : rb.path) {
                int bx = (int) Math.round(loc.getX());
                int by = (int) Math.round(loc.getY());
                if (bx >= 0 && bx < gridRows && by >= 0 && by < gridCols) bloqueado[bx * gridCols + by] = true;
            }
        }
        // El nodo inicial no puede estar bloqueado: la unidad no puede empezar ahí.
        if (startX < 0 || startX >= gridRows || startY < 0 || startY >= gridCols || bloqueado[startX * gridCols + startY]) {
            return Double.POSITIVE_INFINITY;
        }

        int total = gridRows * gridCols;
        int[] g = new int[total];
        java.util.Arrays.fill(g, Integer.MAX_VALUE);
        // Cola de prioridad por f = g + h con desempate estable: (f, índice) empaquetados en un long.
        PriorityQueue<Long> abiertos = new PriorityQueue<>();
        int inicio = startX * gridCols + startY;
        g[inicio] = 0;
        abiertos.add(((long) (Math.abs(startX - endX) + Math.abs(startY - endY)) << 32) | inicio);
        final int[] dx = {0, 0, 1, -1}, dy = {1, -1, 0, 0};
        while (!abiertos.isEmpty()) {
            long cabeza = abiertos.poll();
            int idx = (int) (cabeza & 0xFFFFFFFFL);
            int x = idx / gridCols, y = idx % gridCols;
            int gActual = g[idx];
            if (x == endX && y == endY) return gActual;
            if ((int) (cabeza >>> 32) > gActual + Math.abs(x - endX) + Math.abs(y - endY)) continue; // entrada vieja
            for (int d = 0; d < 4; d++) {
                int nx = x + dx[d], ny = y + dy[d];
                if (nx < 0 || nx >= gridRows || ny < 0 || ny >= gridCols) continue;
                int nidx = nx * gridCols + ny;
                if (bloqueado[nidx]) continue;
                int ng = gActual + 1;
                if (ng < g[nidx]) {
                    g[nidx] = ng;
                    abiertos.add(((long) (ng + Math.abs(nx - endX) + Math.abs(ny - endY)) << 32) | nidx);
                }
            }
        }
        return Double.POSITIVE_INFINITY;
    }

    public List<Location> calculatePath(Location start, Location end, List<Roadblock> roadblocks, double currentTime) {
        int startX = (int) Math.round(start.getX());
        int startY = (int) Math.round(start.getY());
        int endX = (int) Math.round(end.getX());
        int endY = (int) Math.round(end.getY());

        Set<String> blockedNodes = new HashSet<>();
        for (Roadblock rb : roadblocks) {
            if (currentTime >= rb.startTimeHours && currentTime <= rb.endTimeHours) {
                for (Location loc : rb.path) {
                    blockedNodes.add((int) Math.round(loc.getX()) + "," + (int) Math.round(loc.getY()));
                }
            }
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<String, Double> gScore = new HashMap<>();
        Map<String, String> parentMap = new HashMap<>();

        if (blockedNodes.contains(startX + "," + startY)) return Collections.emptyList();

        Node startNode = new Node(startX, startY, 0, manhattan(startX, startY, endX, endY));
        openSet.add(startNode);
        gScore.put(startX + "," + startY, 0.0);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.x == endX && current.y == endY) {
                List<Location> path = new ArrayList<>();
                String key = endX + "," + endY;
                while (key != null) {
                    String[] coords = key.split(",");
                    path.add(0, new Location("", "", Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Location.LocationType.CUSTOMER));
                    key = parentMap.get(key);
                }
                return path;
            }

            int[][] neighbors = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
            for (int[] n : neighbors) {
                int nextX = current.x + n[0];
                int nextY = current.y + n[1];

                if (nextX >= 0 && nextX < gridRows && nextY >= 0 && nextY < gridCols) {
                    if (blockedNodes.contains(nextX + "," + nextY)) continue;

                    double tentativeG = current.g + 1;
                    String nextKey = nextX + "," + nextY;
                    if (tentativeG < gScore.getOrDefault(nextKey, Double.POSITIVE_INFINITY)) {
                        parentMap.put(nextKey, current.x + "," + current.y);
                        gScore.put(nextKey, tentativeG);
                        openSet.add(new Node(nextX, nextY, tentativeG, manhattan(nextX, nextY, endX, endY)));
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private static double manhattan(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }
}
