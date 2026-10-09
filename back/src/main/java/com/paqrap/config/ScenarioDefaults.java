package com.paqrap.config;

import java.util.List;
import java.util.Map;

public class ScenarioDefaults {
    public static final int GRID_ANCHO = 70;
    public static final int GRID_ALTO = 50;

    public static final Map<String, Object> MAPA_BASE = Map.of(
            "ancho", GRID_ANCHO,
            "alto", GRID_ALTO,
            "almacenes", List.of(
                    Map.of("id", "CENTRAL", "nombre", "Almacén Central", "x", 27, "y", 14, "tipo", "CENTRAL"),
                    Map.of("id", "INTERMEDIO_NOROESTE", "nombre", "Almacén Intermedio Nor-Oeste", "x", 12, "y", 38, "tipo", "INTERMEDIO", "capacidad", 1000),
                    Map.of("id", "INTERMEDIO_ESTE", "nombre", "Almacén Intermedio Este", "x", 57, "y", 27, "tipo", "INTERMEDIO", "capacidad", 1000)
            ),
            "semaforo", Map.of("verdeMinPct", 60, "ambarMinPct", 30)
    );
}
