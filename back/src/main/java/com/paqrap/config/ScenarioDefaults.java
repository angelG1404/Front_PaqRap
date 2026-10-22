package com.paqrap.config;

import com.paqrap.model.VehicleType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Datos fijos del caso PaqRap: grilla, almacenes, flota y semáforo por defecto (un solo lugar en el back). */
public final class ScenarioDefaults {

    /** Coordenada máxima: la grilla va de (0,0) a (70,50), 1 km entre nodos adyacentes. */
    public static final int GRID_ANCHO = 70;
    public static final int GRID_ALTO = 50;

    public static final int CAPACIDAD_INTERMEDIO = 1000;
    public static final double HORIZONTE_5D_HORAS = 120.0;
    public static final double VERDE_MIN_PCT = 60.0;
    public static final double AMBAR_MIN_PCT = 30.0;

    public record AlmacenDef(String id, String nombre, int x, int y, boolean central) {
    }

    public static final List<AlmacenDef> ALMACENES = List.of(
            new AlmacenDef("CENTRAL", "Almacén Central", 27, 14, true),
            new AlmacenDef("INTERMEDIO_NOROESTE", "Almacén Intermedio Nor-Oeste", 12, 38, false),
            new AlmacenDef("INTERMEDIO_ESTE", "Almacén Intermedio Este", 57, 27, false)
    );

    /** Flota del enunciado: 10 autos, 15 motos y 12 bicicletas. */
    public static final Map<String, Integer> FLOTA = Map.of("auto", 10, "moto", 15, "bicicleta", 12);

    public static final Map<String, VehicleType> TIPOS_FLOTA = Map.of(
            "auto", VehicleType.AUTO, "moto", VehicleType.MOTO, "bicicleta", VehicleType.BICI);

    private ScenarioDefaults() {
    }

    public static Map<String, Object> mapaBase() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("ancho", GRID_ANCHO);
        mapa.put("alto", GRID_ALTO);
        mapa.put("almacenes", ALMACENES.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.id());
            m.put("nombre", a.nombre());
            m.put("x", a.x());
            m.put("y", a.y());
            m.put("tipo", a.central() ? "CENTRAL" : "INTERMEDIO");
            if (!a.central()) m.put("capacidad", CAPACIDAD_INTERMEDIO);
            return m;
        }).toList());
        mapa.put("semaforo", Map.of("verdeMinPct", VERDE_MIN_PCT, "ambarMinPct", AMBAR_MIN_PCT));
        return mapa;
    }
}
