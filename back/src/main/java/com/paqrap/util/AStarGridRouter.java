package com.paqrap.util;

import com.paqrap.model.Location;
import com.paqrap.model.Roadblock;
import java.util.*;

/**
 * Implementación de A* para navegación en grilla ortogonal con bloqueos viales.
 */
public class AStarGridRouter {

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

    public double calculateDistance(Location start, Location end, List<Roadblock> roadblocks, double currentTime) {
        int startX = (int) Math.round(start.getX());
        int startY = (int) Math.round(start.getY());
        int endX = (int) Math.round(end.getX());
        int endY = (int) Math.round(end.getY());

        // Pre-calcular nodos bloqueados para el currentTime dado
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

        // El nodo inicial no puede estar bloqueado. Si lo está, es un error de escenario o el vehículo no puede empezar ahí.
        if (blockedNodes.contains(startX + "," + startY)) {
            return Double.POSITIVE_INFINITY;
        }

        Node startNode = new Node(startX, startY, 0, manhattan(startX, startY, endX, endY));
        openSet.add(startNode);
        gScore.put(startX + "," + startY, 0.0);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.x == endX && current.y == endY) {
                return current.g;
            }

            int[][] neighbors = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
            for (int[] n : neighbors) {
                int nextX = current.x + n[0];
                int nextY = current.y + n[1];

                if (nextX >= 0 && nextX < gridRows && nextY >= 0 && nextY < gridCols) {
                    // Check if node is blocked
                    if (blockedNodes.contains(nextX + "," + nextY)) continue;

                    double tentativeG = current.g + 1;
                    String key = nextX + "," + nextY;
                    if (tentativeG < gScore.getOrDefault(key, Double.POSITIVE_INFINITY)) {
                        gScore.put(key, tentativeG);
                        openSet.add(new Node(nextX, nextY, tentativeG, manhattan(nextX, nextY, endX, endY)));
                    }
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
