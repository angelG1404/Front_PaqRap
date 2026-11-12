package com.paqrap.util;

import com.paqrap.model.Location;
import com.paqrap.model.Roadblock;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** La versión optimizada de calculateDistance debe dar exactamente lo mismo que el camino que construye calculatePath. */
class AStarGridRouterTest {

    private static Location punto(int x, int y) {
        return new Location("p", "p", x, y, Location.LocationType.CUSTOMER);
    }

    @Test
    void laDistanciaCoincideConElCaminoEnMilesDeCasosAleatorios() {
        Random azar = new Random(20261);
        AStarGridRouter router = new AStarGridRouter(71, 51);
        int conBloqueoEnElCamino = 0;
        for (int caso = 0; caso < 3000; caso++) {
            List<Roadblock> bloqueos = new ArrayList<>();
            int cantidad = azar.nextInt(10);
            for (int b = 0; b < cantidad; b++) {
                int x = azar.nextInt(71), y = azar.nextInt(51);
                boolean horizontal = azar.nextBoolean();
                int largo = 2 + azar.nextInt(25);
                List<Location> vertices = List.of(punto(x, y), horizontal ? punto(Math.min(70, x + largo), y) : punto(x, Math.min(50, y + largo)));
                double desde = azar.nextInt(10), hasta = desde + 1 + azar.nextInt(10);
                bloqueos.add(new Roadblock(vertices, desde, hasta));
            }
            Location a = punto(azar.nextInt(71), azar.nextInt(51)), b = punto(azar.nextInt(71), azar.nextInt(51));
            double t = azar.nextInt(14);
            double rapido = router.calculateDistance(a, b, bloqueos, t);
            List<Location> camino = router.calculatePath(a, b, bloqueos, t);
            double esperado = camino.isEmpty() ? Double.POSITIVE_INFINITY : camino.size() - 1;
            assertEquals(esperado, rapido, "caso " + caso);
            if (rapido > Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY())) conBloqueoEnElCamino++;
        }
        // El generador debe haber puesto a prueba también los rodeos (no solo el atajo Manhattan).
        org.junit.jupiter.api.Assertions.assertTrue(conBloqueoEnElCamino > 100, "casos con rodeo: " + conBloqueoEnElCamino);
    }
}
