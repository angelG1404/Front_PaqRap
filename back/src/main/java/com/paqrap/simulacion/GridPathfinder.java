package com.paqrap.simulacion;

import com.paqrap.config.ScenarioDefaults;

import java.util.*;

/**
 * Caminos ortogonales sobre la grilla (1 km entre nodos) evitando los nodos bloqueados vigentes.
 * Entre caminos de igual longitud prefiere el de menos giros, para que las rutas dibujadas en el
 * mapa sean legibles (en "L") en lugar de escaleras. El origen y el destino nunca se consideran
 * bloqueados: un vehículo puede salir de una calle que se cerró o entregar en ella.
 * No es seguro para hilos: lo usa solo el motor, bajo su propio candado.
 */
final class GridPathfinder {

    private static final int[][] DIRECCIONES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final double PENALIDAD_GIRO = 1e-3;

    private final int ancho = ScenarioDefaults.GRID_ANCHO + 1;
    private final int alto = ScenarioDefaults.GRID_ALTO + 1;
    private final boolean[] bloqueado = new boolean[ancho * alto];
    private final Map<Long, Integer> cacheDistancias = new HashMap<>();

    void setBloqueados(Collection<int[]> nodos) {
        Arrays.fill(bloqueado, false);
        for (int[] n : nodos) {
            if (dentro(n[0], n[1])) bloqueado[indice(n[0], n[1])] = true;
        }
        cacheDistancias.clear();
    }

    /** Camino de nodos desde el origen hasta el destino, ambos incluidos. */
    List<int[]> camino(int x0, int y0, int x1, int y1) {
        x0 = limitarX(x0); x1 = limitarX(x1); y0 = limitarY(y0); y1 = limitarY(y1);
        if (x0 == x1 && y0 == y1) return List.of(new int[]{x0, y0});
        if (enLLibre(x0, y0, x1, y1, true)) return enL(x0, y0, x1, y1, true);
        if (enLLibre(x0, y0, x1, y1, false)) return enL(x0, y0, x1, y1, false);
        List<int[]> camino = aEstrella(x0, y0, x1, y1);
        // Si los bloqueos encierran el destino, se atraviesan antes que dejar la unidad inmóvil.
        return camino != null ? camino : enL(x0, y0, x1, y1, true);
    }

    /** Longitud en km (nodos recorridos) del camino más corto vigente. */
    int distancia(int x0, int y0, int x1, int y1) {
        x0 = limitarX(x0); x1 = limitarX(x1); y0 = limitarY(y0); y1 = limitarY(y1);
        if (enLLibre(x0, y0, x1, y1, true) || enLLibre(x0, y0, x1, y1, false)) {
            return Math.abs(x0 - x1) + Math.abs(y0 - y1);
        }
        long clave = ((long) indice(x0, y0) << 32) | indice(x1, y1);
        Integer guardada = cacheDistancias.get(clave);
        if (guardada != null) return guardada;
        int d = camino(x0, y0, x1, y1).size() - 1;
        cacheDistancias.put(clave, d);
        return d;
    }

    private boolean enLLibre(int x0, int y0, int x1, int y1, boolean primeroX) {
        for (int[] n : enL(x0, y0, x1, y1, primeroX)) {
            if (bloqueado[indice(n[0], n[1])] && !(n[0] == x0 && n[1] == y0) && !(n[0] == x1 && n[1] == y1)) return false;
        }
        return true;
    }

    private static List<int[]> enL(int x0, int y0, int x1, int y1, boolean primeroX) {
        List<int[]> nodos = new ArrayList<>(Math.abs(x0 - x1) + Math.abs(y0 - y1) + 1);
        int x = x0, y = y0;
        nodos.add(new int[]{x, y});
        if (primeroX) {
            while (x != x1) { x += Integer.signum(x1 - x); nodos.add(new int[]{x, y}); }
            while (y != y1) { y += Integer.signum(y1 - y); nodos.add(new int[]{x, y}); }
        } else {
            while (y != y1) { y += Integer.signum(y1 - y); nodos.add(new int[]{x, y}); }
            while (x != x1) { x += Integer.signum(x1 - x); nodos.add(new int[]{x, y}); }
        }
        return nodos;
    }

    /** A* sobre estados (nodo, dirección de llegada) con una penalidad mínima por giro. */
    private List<int[]> aEstrella(int x0, int y0, int x1, int y1) {
        int estados = ancho * alto * 5;
        double[] costo = new double[estados];
        int[] padre = new int[estados];
        Arrays.fill(costo, Double.POSITIVE_INFINITY);
        Arrays.fill(padre, -1);
        PriorityQueue<double[]> abiertos = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));
        int inicio = indice(x0, y0) * 5 + 4;
        costo[inicio] = 0;
        abiertos.add(new double[]{heuristica(x0, y0, x1, y1), inicio});
        int destino = indice(x1, y1);

        while (!abiertos.isEmpty()) {
            double[] actual = abiertos.poll();
            int estado = (int) actual[1];
            int nodo = estado / 5, dir = estado % 5;
            double g = costo[estado];
            if (actual[0] > g + heuristica(nodo % ancho, nodo / ancho, x1, y1) + 1e-9) continue;
            if (nodo == destino) return reconstruir(padre, estado);
            int x = nodo % ancho, y = nodo / ancho;
            for (int d = 0; d < 4; d++) {
                int nx = x + DIRECCIONES[d][0], ny = y + DIRECCIONES[d][1];
                if (!dentro(nx, ny)) continue;
                int vecino = indice(nx, ny);
                if (bloqueado[vecino] && vecino != destino) continue;
                double ng = g + 1 + (dir != 4 && dir != d ? PENALIDAD_GIRO : 0);
                int siguiente = vecino * 5 + d;
                if (ng < costo[siguiente]) {
                    costo[siguiente] = ng;
                    padre[siguiente] = estado;
                    abiertos.add(new double[]{ng + heuristica(nx, ny, x1, y1), siguiente});
                }
            }
        }
        return null;
    }

    private List<int[]> reconstruir(int[] padre, int estado) {
        LinkedList<int[]> nodos = new LinkedList<>();
        for (int e = estado; e != -1; e = padre[e]) {
            int nodo = e / 5;
            nodos.addFirst(new int[]{nodo % ancho, nodo / ancho});
        }
        return new ArrayList<>(nodos);
    }

    private static double heuristica(int x, int y, int x1, int y1) {
        return Math.abs(x - x1) + Math.abs(y - y1);
    }

    private boolean dentro(int x, int y) {
        return x >= 0 && y >= 0 && x < ancho && y < alto;
    }

    private int indice(int x, int y) {
        return y * ancho + x;
    }

    private int limitarX(int x) {
        return Math.max(0, Math.min(ancho - 1, x));
    }

    private int limitarY(int y) {
        return Math.max(0, Math.min(alto - 1, y));
    }
}
