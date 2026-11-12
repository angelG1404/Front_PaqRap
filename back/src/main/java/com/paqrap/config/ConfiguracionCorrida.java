package com.paqrap.config;

import com.paqrap.model.VehicleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Parámetros de una corrida que se pueden definir al iniciarla y modificar mientras corre:
 * reglas de los tres tipos de avería, ubicación y capacidad de los almacenes y capacidad de cada tipo de unidad.
 * Todos los campos son opcionales al recibirlos (configuración parcial); {@link #combinar} la completa con
 * la anterior y {@link #validar} comprueba los rangos. Una configuración combinada nunca tiene nulos.
 */
public record ConfiguracionCorrida(AveriasCfg averias, List<AlmacenCfg> almacenes, List<VehiculoCfg> vehiculos) {

    /** Tipo 1: no disponible {@code duracionHoras} h. */
    public record Tipo1(Double duracionHoras) {
    }

    /** Tipo 2: no disponible hasta el fin del {@code turnosEspera}-ésimo turno siguiente; remolque a las {@code horasRemolque} h. */
    public record Tipo2(Integer turnosEspera, Double horasRemolque) {
    }

    /** Tipo 3: no disponible al menos {@code horasMinimas} h y vuelve en el primer turno que inicia a {@code horaRetorno}. */
    public record Tipo3(Double horasMinimas, Integer horaRetorno, Double horasRemolque) {
    }

    public record AveriasCfg(Tipo1 tipo1, Tipo2 tipo2, Tipo3 tipo3) {
    }

    /** {@code nombre} es solo de lectura; {@code capacidad} es null en el almacén central (inventario infinito). */
    public record AlmacenCfg(String id, String nombre, Integer x, Integer y, Integer capacidad) {
    }

    /** {@code velocidadKmH} y {@code costoPorKm} se informan pero no se modifican. */
    public record VehiculoCfg(String tipo, Integer capacidad, Double velocidadKmH, Double costoPorKm) {
    }

    public static final Set<Integer> INICIOS_DE_TURNO = Set.of(7, 15, 23);
    private static final List<String> TIPOS = List.of("auto", "moto", "bicicleta");

    public static ConfiguracionCorrida porDefecto() {
        List<AlmacenCfg> almacenes = new ArrayList<>();
        for (ScenarioDefaults.AlmacenDef a : ScenarioDefaults.ALMACENES) {
            almacenes.add(new AlmacenCfg(a.id(), a.nombre(), a.x(), a.y(), a.central() ? null : ScenarioDefaults.CAPACIDAD_INTERMEDIO));
        }
        List<VehiculoCfg> vehiculos = new ArrayList<>();
        for (String tipo : TIPOS) {
            VehicleType t = ScenarioDefaults.TIPOS_FLOTA.get(tipo);
            vehiculos.add(new VehiculoCfg(tipo, t.getCapacity(), t.getSpeedKmH(), t.getCostPerKm()));
        }
        return new ConfiguracionCorrida(
                new AveriasCfg(new Tipo1(2.0), new Tipo2(1, 4.0), new Tipo3(48.0, 15, 4.0)),
                List.copyOf(almacenes), List.copyOf(vehiculos));
    }

    /** Completa {@code parcial} con los valores de {@code base}: lo que no se envía se conserva. */
    public static ConfiguracionCorrida combinar(ConfiguracionCorrida base, ConfiguracionCorrida parcial) {
        if (parcial == null) return base;
        AveriasCfg ba = base.averias(), pa = parcial.averias();
        AveriasCfg averias = ba;
        if (pa != null) {
            Tipo1 t1 = pa.tipo1() == null ? ba.tipo1() : new Tipo1(or(pa.tipo1().duracionHoras(), ba.tipo1().duracionHoras()));
            Tipo2 t2 = pa.tipo2() == null ? ba.tipo2()
                    : new Tipo2(or(pa.tipo2().turnosEspera(), ba.tipo2().turnosEspera()), or(pa.tipo2().horasRemolque(), ba.tipo2().horasRemolque()));
            Tipo3 t3 = pa.tipo3() == null ? ba.tipo3()
                    : new Tipo3(or(pa.tipo3().horasMinimas(), ba.tipo3().horasMinimas()), or(pa.tipo3().horaRetorno(), ba.tipo3().horaRetorno()),
                    or(pa.tipo3().horasRemolque(), ba.tipo3().horasRemolque()));
            averias = new AveriasCfg(t1, t2, t3);
        }
        List<AlmacenCfg> almacenes = new ArrayList<>();
        for (AlmacenCfg a : base.almacenes()) {
            AlmacenCfg p = parcial.almacenes() == null ? null : parcial.almacenes().stream().filter(x -> a.id().equalsIgnoreCase(x.id())).findFirst().orElse(null);
            almacenes.add(p == null ? a : new AlmacenCfg(a.id(), a.nombre(), or(p.x(), a.x()), or(p.y(), a.y()),
                    a.capacidad() == null ? null : or(p.capacidad(), a.capacidad())));
        }
        if (parcial.almacenes() != null) {
            for (AlmacenCfg p : parcial.almacenes()) {
                if (almacenes.stream().noneMatch(a -> a.id().equalsIgnoreCase(p.id()))) {
                    throw new IllegalArgumentException("Almacén desconocido: " + p.id() + ".");
                }
            }
        }
        List<VehiculoCfg> vehiculos = new ArrayList<>();
        for (VehiculoCfg v : base.vehiculos()) {
            VehiculoCfg p = parcial.vehiculos() == null ? null : parcial.vehiculos().stream().filter(x -> v.tipo().equalsIgnoreCase(x.tipo())).findFirst().orElse(null);
            vehiculos.add(p == null ? v : new VehiculoCfg(v.tipo(), or(p.capacidad(), v.capacidad()), v.velocidadKmH(), v.costoPorKm()));
        }
        if (parcial.vehiculos() != null) {
            for (VehiculoCfg p : parcial.vehiculos()) {
                if (TIPOS.stream().noneMatch(t -> t.equalsIgnoreCase(p.tipo()))) throw new IllegalArgumentException("Tipo de unidad desconocido: " + p.tipo() + ".");
            }
        }
        ConfiguracionCorrida resultado = new ConfiguracionCorrida(averias, List.copyOf(almacenes), List.copyOf(vehiculos));
        resultado.validar();
        return resultado;
    }

    private static <T> T or(T nuevo, T anterior) {
        return nuevo != null ? nuevo : anterior;
    }

    public void validar() {
        Tipo1 t1 = averias.tipo1();
        rango("Avería tipo 1: duración (h)", t1.duracionHoras(), 0.25, 72);
        Tipo2 t2 = averias.tipo2();
        if (t2.turnosEspera() < 1 || t2.turnosEspera() > 3) throw new IllegalArgumentException("Avería tipo 2: los turnos de espera deben ser 1, 2 o 3.");
        rango("Avería tipo 2: horas hasta el remolque", t2.horasRemolque(), 0, 48);
        Tipo3 t3 = averias.tipo3();
        rango("Avería tipo 3: horas mínimas fuera de servicio", t3.horasMinimas(), 1, 240);
        if (!INICIOS_DE_TURNO.contains(t3.horaRetorno())) throw new IllegalArgumentException("Avería tipo 3: el turno de regreso debe iniciar a las 7, 15 o 23 h.");
        rango("Avería tipo 3: horas hasta el remolque", t3.horasRemolque(), 0, 48);

        for (AlmacenCfg a : almacenes) {
            if (a.x() < 0 || a.x() > ScenarioDefaults.GRID_ANCHO) throw new IllegalArgumentException("Almacén " + a.id() + ": X debe estar entre 0 y " + ScenarioDefaults.GRID_ANCHO + ".");
            if (a.y() < 0 || a.y() > ScenarioDefaults.GRID_ALTO) throw new IllegalArgumentException("Almacén " + a.id() + ": Y debe estar entre 0 y " + ScenarioDefaults.GRID_ALTO + ".");
            if (a.capacidad() != null && (a.capacidad() < 1 || a.capacidad() > 1_000_000)) {
                throw new IllegalArgumentException("Almacén " + a.id() + ": la capacidad debe estar entre 1 y 1 000 000 paquetes.");
            }
        }
        for (int i = 0; i < almacenes.size(); i++) {
            for (int j = i + 1; j < almacenes.size(); j++) {
                if (Objects.equals(almacenes.get(i).x(), almacenes.get(j).x()) && Objects.equals(almacenes.get(i).y(), almacenes.get(j).y())) {
                    throw new IllegalArgumentException("Los almacenes " + almacenes.get(i).id() + " y " + almacenes.get(j).id() + " no pueden estar en la misma ubicación.");
                }
            }
        }
        for (VehiculoCfg v : vehiculos) {
            if (v.capacidad() < 1 || v.capacidad() > 1000) throw new IllegalArgumentException("Unidad " + v.tipo() + ": la capacidad debe estar entre 1 y 1000 paquetes.");
        }
    }

    private static void rango(String nombre, double valor, double min, double max) {
        if (Double.isNaN(valor) || valor < min || valor > max) throw new IllegalArgumentException(nombre + " debe estar entre " + min + " y " + max + ".");
    }

    public AlmacenCfg almacen(String id) {
        return almacenes.stream().filter(a -> a.id().equalsIgnoreCase(id)).findFirst().orElseThrow();
    }

    public int capacidadDe(String tipo) {
        return vehiculos.stream().filter(v -> v.tipo().equalsIgnoreCase(tipo)).findFirst().orElseThrow().capacidad();
    }
}
