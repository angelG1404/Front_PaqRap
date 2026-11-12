package com.paqrap.simulacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paqrap.config.ScenarioDefaults;
import com.paqrap.ingesta.OrderRecordParser;
import com.paqrap.model.Order;
import com.paqrap.planificador.AlgorithmSelector;
import com.paqrap.planificador.InstanciaPlanificacion;
import com.paqrap.planificador.PlanResultado;
import com.paqrap.planificador.PlanningAlgorithm;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mide lo que paga el hilo del tick en la carga más alta (colapso): avance del motor, armado del snapshot y su tamaño en
 * JSON. El algoritmo se mide aparte porque en producción corre en otro hilo. Imprime tiempos; solo falla si el tick
 * (sin algoritmo) se acerca al segundo que dura el período real.
 */
class RendimientoMotorTest {

    private static final double DT = 480.0 / 3600.0;

    @Test
    void elTickYElSnapshotCaben_holgadosEnUnSegundoConCargaDeColapso() throws IOException {
        long[] planNanos = {0};
        InstanciaPlanificacion[] masGrande = {null};
        PlanningAlgorithm medido = new PlanningAlgorithm() {
            final PlanningAlgorithm real = AlgorithmSelector.seleccionar("ALNS", 250);
            @Override public String nombre() { return real.nombre(); }
            @Override public PlanResultado planificar(InstanciaPlanificacion i, long limiteMs) {
                long t = System.nanoTime();
                if (masGrande[0] == null || i.pedidos().size() > masGrande[0].pedidos().size()) masGrande[0] = i;
                try { return real.planificar(i, limiteMs); } finally { planNanos[0] += System.nanoTime() - t; }
            }
        };
        List<Order> pedidos = new ArrayList<>();
        OrderRecordParser parser = new OrderRecordParser();
        for (String l : Files.readAllLines(archivo("pedidos_colapso.txt"))) if (!l.isBlank()) pedidos.add(parser.parse(l.trim()));
        ScenarioConfig config = new ScenarioConfig(ScenarioConfig.COLAPSO, 480, 5, 1500, 250, 60, 60, 30);
        SimulationEngine motor = new SimulationEngine(config, medido, new Directo(), LocalDate.of(2026, 1, 1).atStartOfDay(), 0,
                ScenarioDefaults.FLOTA, pedidos, bloqueos());
        Corrida corrida = new Corrida("bench", ScenarioConfig.COLAPSO, motor, new SimulationClock(480));
        ObjectMapper json = new ObjectMapper();

        long tickNanos = 0, mapNanos = 0, jsonNanos = 0, maxTick = 0, maxMap = 0, bytesMax = 0, bytesTotal = 0;
        int ticks = 0;
        while (motor.enCurso() && ticks < 40) {
            long antesPlan = planNanos[0];
            long t0 = System.nanoTime();
            motor.tick(DT);
            long tick = System.nanoTime() - t0 - (planNanos[0] - antesPlan);
            long t1 = System.nanoTime();
            var snap = SnapshotMapper.map(corrida);
            long map = System.nanoTime() - t1;
            long t2 = System.nanoTime();
            int bytes = json.writeValueAsBytes(snap).length;
            jsonNanos += System.nanoTime() - t2;
            tickNanos += tick; mapNanos += map;
            maxTick = Math.max(maxTick, tick); maxMap = Math.max(maxMap, map);
            bytesMax = Math.max(bytesMax, bytes); bytesTotal += bytes;
            ticks++;
        }
        System.out.printf("[RENDIMIENTO] %d ticks hasta %s | tick(sin plan) medio %.2f ms, máx %.2f ms | snapshot medio %.2f ms, máx %.2f ms | JSON %.2f ms, medio %d KB, máx %d KB | planificador %.0f ms en total%n",
                ticks, SimTime.formato(motor.getAhora()), tickNanos / 1e6 / ticks, maxTick / 1e6, mapNanos / 1e6 / ticks, maxMap / 1e6,
                jsonNanos / 1e6 / ticks, bytesTotal / ticks / 1024, bytesMax / 1024, planNanos[0] / 1e6);
        assertTrue((tickNanos + mapNanos) / 1e6 / ticks < 500, "el trabajo del hilo del tick debe caber muy por debajo de 1 s");

        // Costo de una ejecución completa de ALNS (250 iteraciones, sin límite de tiempo) sobre el lote más grande:
        // lo que cuesta cada evaluación de ruta (A* con bloqueos) decide cuántas iteraciones caben en el presupuesto de 1,5 s.
        InstanciaPlanificacion lote = loteFijo();
        PlanningAlgorithm alns = AlgorithmSelector.seleccionar("ALNS", 250);
        alns.planificar(lote, 600_000); // calentamiento de la JVM
        long mejor = Long.MAX_VALUE;
        for (int i = 0; i < 2; i++) {
            long t = System.nanoTime();
            alns.planificar(lote, 600_000);
            mejor = Math.min(mejor, System.nanoTime() - t);
        }
        System.out.printf("[RENDIMIENTO] ALNS 250 iteraciones sobre %d pedidos, %d unidades y %d bloqueos vigentes: %.0f ms%n",
                lote.pedidos().size(), lote.vehiculos().size(), lote.bloqueos().size(), mejor / 1e6);
    }

    /** 40 pedidos repartidos por la grilla y la flota completa, con los bloqueos que están vigentes a las 30 h. */
    static InstanciaPlanificacion loteFijo() throws IOException {
        java.util.Random azar = new java.util.Random(7);
        List<Order> pedidos = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            com.paqrap.model.Location destino = new com.paqrap.model.Location("c" + i, "c" + i, azar.nextInt(71), azar.nextInt(51), com.paqrap.model.Location.LocationType.CUSTOMER);
            pedidos.add(new Order("P" + i, destino, 1 + azar.nextInt(8), 30, new double[]{4, 8, 12, 18, 36}[azar.nextInt(5)]));
        }
        List<com.paqrap.planificador.VehiculoDisponible> flota = new ArrayList<>();
        int n = 0;
        for (var def : ScenarioDefaults.ALMACENES) {
            for (var tipo : com.paqrap.model.VehicleType.values()) {
                for (int k = 0; k < 4; k++) flota.add(new com.paqrap.planificador.VehiculoDisponible("U" + (n++), tipo, def.id(), def.x(), def.y(), 30));
            }
        }
        List<com.paqrap.model.Roadblock> vigentes = new ArrayList<>();
        for (BloqueoSim b : bloqueos()) if (b.finHoras() > 30 && b.inicioHoras() < 42) vigentes.add(b.roadblock);
        return new InstanciaPlanificacion(30, flota, pedidos, vigentes);
    }

    private static List<BloqueoSim> bloqueos() throws IOException {
        List<BloqueoSim> lista = new ArrayList<>();
        int n = 0;
        for (String l : Files.readAllLines(archivo("bloqueo.txt"))) if (!l.isBlank()) lista.add(BloqueoSim.desdeLinea("B" + (++n), l.trim()));
        return lista;
    }

    private static Path archivo(String nombre) {
        Path p = Path.of("..", "data", nombre);
        return Files.exists(p) ? p : Path.of("data", nombre);
    }

    private static final class Directo extends AbstractExecutorService {
        private boolean cerrado;
        @Override public void execute(Runnable command) { command.run(); }
        @Override public void shutdown() { cerrado = true; }
        @Override public List<Runnable> shutdownNow() { cerrado = true; return List.of(); }
        @Override public boolean isShutdown() { return cerrado; }
        @Override public boolean isTerminated() { return cerrado; }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }
    }
}
