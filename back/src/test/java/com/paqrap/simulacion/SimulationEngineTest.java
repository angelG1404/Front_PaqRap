package com.paqrap.simulacion;

import com.paqrap.config.ScenarioDefaults;
import com.paqrap.ingesta.OrderRecordParser;
import com.paqrap.model.Order;
import com.paqrap.planificador.AlgorithmSelector;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Corre el motor sin Spring ni WebSocket. El planificador se ejecuta en el mismo hilo para que el
 * resultado no dependa de la velocidad de la máquina. Los archivos vienen de la carpeta data/.
 */
class SimulationEngineTest {

    /** Paso de un tick a 160x con un tick por segundo: 2,67 minutos simulados. */
    private static final double DT_5D = 160.0 / 3600.0;

    @Test
    void simulacion5DEntregaLosPedidosConBloqueos() throws IOException {
        SimulationEngine motor = motor(ScenarioConfig.SIM_5D, "ALNS", pedidos("ventas.txt"), bloqueos("bloqueo.txt"));
        int ticks = 0;
        boolean huboBloqueos = false, huboMovimiento = false;
        while (motor.enCurso() && ticks++ < 10_000) {
            motor.tick(DT_5D);
            huboBloqueos |= !motor.bloqueosVigentes.isEmpty();
            huboMovimiento |= motor.vehiculos.stream().anyMatch(v -> v.estado == VehiculoSim.Estado.EN_RUTA);
        }
        imprimir("5D ALNS", motor);
        assertEquals(SimulationEngine.FINALIZADO, motor.estado);
        assertTrue(huboBloqueos, "Debió haber bloqueos vigentes durante los 5 días");
        assertTrue(huboMovimiento, "Las unidades debieron moverse");
        assertTrue(motor.pedidos.size() > 50, "Se ingresan los pedidos de los 5 primeros días");
        assertTrue(motor.entregados >= motor.pedidos.size() - 5, "Casi todo lo registrado se entrega antes del fin");
        assertEquals(0, motor.entregadosTarde, "Con la demanda de ventas.txt no debería haber entregas tarde");
        assertTrue(motor.costoTotal > 0);
    }

    @Test
    void atsYGreedyTambienPlanifican() throws IOException {
        for (String algoritmo : List.of("ATS", "GREEDY")) {
            SimulationEngine motor = motor(ScenarioConfig.SIM_5D, algoritmo, pedidos("ventas_small.txt"), bloqueos("bloqueo_small.txt"));
            for (int i = 0; i < 1000 && motor.enCurso(); i++) motor.tick(DT_5D);
            imprimir("5D " + algoritmo, motor);
            assertEquals(motor.pedidos.size(), motor.entregados, algoritmo + " debe entregar todos los pedidos de ventas_small.txt");
        }
    }

    @Test
    void averiaReasignaLosPedidosDeLaUnidad() throws IOException {
        SimulationEngine motor = motor(ScenarioConfig.SIM_5D, "ALNS", pedidos("ventas.txt"), List.of());
        VehiculoSim enRuta = null;
        for (int i = 0; i < 3000 && enRuta == null; i++) {
            motor.tick(DT_5D);
            enRuta = motor.vehiculos.stream()
                    .filter(v -> v.estado == VehiculoSim.Estado.EN_RUTA && v.indiceParada == 0).findFirst().orElse(null);
        }
        assertNotNull(enRuta, "Alguna unidad debió salir a repartir");
        VehiculoSim averiado = enRuta;
        List<PedidoSim> suyos = new ArrayList<>(averiado.paradas);

        var info = motor.registrarAveria(averiado.id, 2);
        assertEquals(VehiculoSim.Estado.AVERIADO, averiado.estado);
        assertTrue((int) info.get("pedidosReasignados") >= 1);
        assertTrue(suyos.stream().allMatch(p -> p.estado == PedidoSim.Estado.PENDIENTE));
        assertThrows(IllegalStateException.class, () -> motor.registrarAveria(averiado.id, 1));

        for (int i = 0; i < 95; i++) motor.tick(DT_5D); // ~4,2 h
        assertEquals(VehiculoSim.Estado.AVERIADO, averiado.estado);
        assertEquals(ScenarioDefaults.ALMACENES.get(0).x(), (int) averiado.x, "Tras 4 h la avería tipo 2 se remolca al central");

        for (int i = 0; i < 300; i++) motor.tick(DT_5D);
        assertTrue(suyos.stream().allMatch(p -> p.estado == PedidoSim.Estado.ENTREGADO || !averiado.id.equals(p.vehiculoId)),
                "Los pedidos de la unidad averiada los atienden otras unidades");
    }

    @Test
    void colapsoSeDetieneConElPrimerPedidoFueraDePlazo() throws IOException {
        SimulationEngine motor = motor(ScenarioConfig.COLAPSO, "ALNS", pedidos("pedidos_colapso.txt"), List.of());
        for (int i = 0; i < 20_000 && motor.enCurso(); i++) motor.tick(320.0 / 3600.0);
        imprimir("COLAPSO", motor);
        assertEquals(SimulationEngine.COLAPSADO, motor.estado);
        assertNotNull(motor.causa);
        assertTrue(motor.atrasados() >= 1);
    }

    @Test
    void turnosYAverias() {
        assertEquals(23.0, SimulationEngine.finDelTurnoSiguiente(10.0));      // turno 07-15; el siguiente (15-23) termina a las 23:00
        assertEquals(24 + 7.0, SimulationEngine.finDelTurnoSiguiente(16.0));  // turno 15-23; el siguiente (23-07) termina a las 07:00
        assertEquals(48 + 15.0, SimulationEngine.inicioTurnoTardeTras(10.0 + 48));
    }

    // ------------------------------------------------------------------------------------------

    private static SimulationEngine motor(String tipo, String algoritmo, List<Order> pedidos, List<BloqueoSim> bloqueos) {
        ScenarioConfig config = new ScenarioConfig(tipo, 160, 5, 400, 250, 60, 60, 30);
        return new SimulationEngine(config, AlgorithmSelector.seleccionar(algoritmo, 250), new EjecutorDirecto(),
                LocalDate.of(2026, 1, 1).atStartOfDay(), 0, ScenarioDefaults.FLOTA, pedidos, bloqueos);
    }

    private static List<Order> pedidos(String archivo) throws IOException {
        OrderRecordParser parser = new OrderRecordParser();
        List<Order> lista = new ArrayList<>();
        for (String linea : Files.readAllLines(data(archivo))) if (!linea.isBlank()) lista.add(parser.parse(linea.trim()));
        return lista;
    }

    private static List<BloqueoSim> bloqueos(String archivo) throws IOException {
        List<BloqueoSim> lista = new ArrayList<>();
        int n = 0;
        for (String linea : Files.readAllLines(data(archivo))) {
            if (!linea.isBlank()) lista.add(BloqueoSim.desdeLinea("BLQ-" + (++n), linea));
        }
        return lista;
    }

    private static Path data(String archivo) {
        Path p = Path.of("..", "data", archivo);
        return Files.exists(p) ? p : Path.of("data", archivo);
    }

    private static void imprimir(String titulo, SimulationEngine m) {
        System.out.printf("[%s] estado=%s t=%s pedidos=%d entregados=%d tarde=%d atrasados=%d costo=S/%.2f km=%.1f planes=%d causa=%s%n",
                titulo, m.estado, SimTime.formato(m.ahora), m.pedidos.size(), m.entregados, m.entregadosTarde, m.atrasados(),
                m.costoTotal, m.distanciaTotal, m.planificaciones, m.causa);
    }

    /** Ejecuta cada tarea en el hilo que la envía (planificación síncrona y reproducible). */
    private static final class EjecutorDirecto extends AbstractExecutorService {
        private boolean cerrado;

        @Override public void execute(Runnable command) { command.run(); }
        @Override public void shutdown() { cerrado = true; }
        @Override public List<Runnable> shutdownNow() { cerrado = true; return List.of(); }
        @Override public boolean isShutdown() { return cerrado; }
        @Override public boolean isTerminated() { return cerrado; }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }
    }
}
