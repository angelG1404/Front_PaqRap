package com.paqrap.simulacion;

import com.paqrap.config.ConfiguracionCorrida;
import com.paqrap.config.ConfiguracionCorrida.*;
import com.paqrap.ingesta.OrderRecordParser;
import com.paqrap.model.Order;
import com.paqrap.planificador.AlgorithmSelector;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Requisitos: reglas de avería, ubicación y capacidad de almacenes y capacidad por tipo de unidad son configurables. */
class ConfiguracionCorridaTest {

    private static final double DT = 160.0 / 3600.0;

    private static SimulationEngine motor(ConfiguracionCorrida cfg, List<Order> pedidos) {
        ScenarioConfig config = new ScenarioConfig(ScenarioConfig.SIM_5D, 160, 5, 300, 100, 60, 60, 30);
        return new SimulationEngine(config, AlgorithmSelector.seleccionar("GREEDY", 100), new Directo(),
                LocalDate.of(2026, 1, 1).atStartOfDay(), 0, null, pedidos, List.of(), cfg);
    }

    private static List<Order> pedidos(String... lineas) {
        OrderRecordParser parser = new OrderRecordParser();
        return java.util.Arrays.stream(lineas).map(parser::parse).toList();
    }

    @Test
    void valoresPorDefectoSonLosDelEnunciado() {
        ConfiguracionCorrida c = ConfiguracionCorrida.porDefecto();
        assertEquals(2.0, c.averias().tipo1().duracionHoras());
        assertEquals(1, c.averias().tipo2().turnosEspera());
        assertEquals(4.0, c.averias().tipo2().horasRemolque());
        assertEquals(48.0, c.averias().tipo3().horasMinimas());
        assertEquals(15, c.averias().tipo3().horaRetorno());
        assertEquals(24, c.capacidadDe("auto"));
        assertEquals(8, c.capacidadDe("moto"));
        assertEquals(4, c.capacidadDe("bicicleta"));
        assertEquals(1000, c.almacen("INTERMEDIO_ESTE").capacidad());
        assertNull(c.almacen("CENTRAL").capacidad());
    }

    @Test
    void combinarConservaLoNoEnviadoYValida() {
        ConfiguracionCorrida base = ConfiguracionCorrida.porDefecto();
        ConfiguracionCorrida c = ConfiguracionCorrida.combinar(base, new ConfiguracionCorrida(
                new AveriasCfg(new Tipo1(3.5), null, null), null, List.of(new VehiculoCfg("moto", 12, null, null))));
        assertEquals(3.5, c.averias().tipo1().duracionHoras());
        assertEquals(4.0, c.averias().tipo2().horasRemolque(), "lo no enviado se conserva");
        assertEquals(12, c.capacidadDe("moto"));
        assertEquals(24, c.capacidadDe("auto"));

        assertThrows(IllegalArgumentException.class, () -> ConfiguracionCorrida.combinar(base,
                new ConfiguracionCorrida(new AveriasCfg(new Tipo1(0.0), null, null), null, null)));
        assertThrows(IllegalArgumentException.class, () -> ConfiguracionCorrida.combinar(base,
                new ConfiguracionCorrida(new AveriasCfg(null, null, new Tipo3(null, 10, null)), null, null)), "turno de regreso inválido");
        assertThrows(IllegalArgumentException.class, () -> ConfiguracionCorrida.combinar(base,
                new ConfiguracionCorrida(null, List.of(new AlmacenCfg("CENTRAL", null, 99, 0, null)), null)), "fuera de la grilla");
        assertThrows(IllegalArgumentException.class, () -> ConfiguracionCorrida.combinar(base,
                new ConfiguracionCorrida(null, List.of(new AlmacenCfg("INTERMEDIO_ESTE", null, 12, 38, null)), null)), "misma ubicación que otro almacén");
        assertThrows(IllegalArgumentException.class, () -> ConfiguracionCorrida.combinar(base,
                new ConfiguracionCorrida(null, List.of(new AlmacenCfg("NO_EXISTE", null, 1, 1, null)), null)));
        assertThrows(IllegalArgumentException.class, () -> ConfiguracionCorrida.combinar(base,
                new ConfiguracionCorrida(null, null, List.of(new VehiculoCfg("auto", 0, null, null)))));
    }

    @Test
    void lasReglasDeAveriaConfiguradasFijanFinYRemolque() {
        ConfiguracionCorrida cfg = ConfiguracionCorrida.combinar(ConfiguracionCorrida.porDefecto(), new ConfiguracionCorrida(
                new AveriasCfg(new Tipo1(5.0), new Tipo2(2, 1.5), new Tipo3(72.0, 7, 0.5)), null, null));
        SimulationEngine m = motor(cfg, List.of());
        for (int i = 0; i < 40; i++) m.tick(DT);
        double t = m.getAhora();

        m.registrarAveria("TA01", 1);
        m.registrarAveria("TA02", 2);
        m.registrarAveria("TA03", 3);
        AveriaSim a1 = m.averias.get(0), a2 = m.averias.get(1), a3 = m.averias.get(2);
        assertEquals(t + 5.0, a1.finHoras, 1e-9);
        assertEquals(-1, a1.remolqueHoras, "el tipo 1 no se remolca");
        assertEquals(SimulationEngine.finDelTurnoSiguiente(t, 2), a2.finHoras, 1e-9);
        assertEquals(SimulationEngine.finDelTurnoSiguiente(t) + 8, a2.finHoras, 1e-9, "dos turnos de espera = un turno más");
        assertEquals(t + 1.5, a2.remolqueHoras, 1e-9);
        assertEquals(SimulationEngine.inicioTurnoTardeTras(t + 72, 7), a3.finHoras, 1e-9);
        assertEquals(t + 0.5, a3.remolqueHoras, 1e-9);
    }

    @Test
    void sinRemolqueSiLaUnidadVuelveAntes() {
        ConfiguracionCorrida cfg = ConfiguracionCorrida.combinar(ConfiguracionCorrida.porDefecto(), new ConfiguracionCorrida(
                new AveriasCfg(null, new Tipo2(1, 48.0), null), null, null));
        SimulationEngine m = motor(cfg, List.of());
        m.tick(DT);
        m.registrarAveria("TA01", 2);
        assertEquals(-1, m.averias.get(0).remolqueHoras, "remolque posterior al fin de la avería: no se remolca");
    }

    @Test
    void cambiarReglasEnCalienteSoloAfectaALasAveriasNuevas() {
        SimulationEngine m = motor(null, List.of());
        m.tick(DT);
        m.registrarAveria("TA01", 1);
        double finAnterior = m.averias.get(0).finHoras;
        m.aplicarConfiguracion(new ConfiguracionCorrida(new AveriasCfg(new Tipo1(6.0), null, null), null, null));
        assertEquals(finAnterior, m.averias.get(0).finHoras, "la avería ya registrada no cambia");
        m.registrarAveria("TA02", 1);
        assertEquals(m.averias.get(1).ocurrenciaHoras + 6.0, m.averias.get(1).finHoras, 1e-9);
        assertEquals(6.0, m.configuracion().averias().tipo1().duracionHoras());
    }

    @Test
    void moverUnAlmacenMueveSusUnidadesYCambiarSuCapacidadAjustaElStock() {
        SimulationEngine m = motor(null, List.of());
        AlmacenSim este = m.almacenes.stream().filter(a -> a.id.equals("INTERMEDIO_ESTE")).findFirst().orElseThrow();
        List<VehiculoSim> alli = m.vehiculos.stream().filter(v -> v.almacen == este).toList();
        assertFalse(alli.isEmpty());

        m.aplicarConfiguracion(new ConfiguracionCorrida(null, List.of(new AlmacenCfg("INTERMEDIO_ESTE", null, 40, 20, 300)), null));
        assertEquals(40, este.x);
        assertEquals(20, este.y);
        assertEquals(300, este.capacidad);
        assertEquals(300, este.stock, "el stock no puede superar la nueva capacidad");
        for (VehiculoSim v : alli) {
            assertEquals(40, v.nodoX(), "las unidades estacionadas se mueven con el almacén");
            assertEquals(20, v.nodoY());
        }
        // Subir la capacidad no regala stock: se completa en la próxima recarga (23:59:59).
        m.aplicarConfiguracion(new ConfiguracionCorrida(null, List.of(new AlmacenCfg("INTERMEDIO_ESTE", null, null, null, 500)), null));
        assertEquals(500, este.capacidad);
        assertEquals(300, este.stock);
        assertEquals(40, este.x, "lo no enviado se conserva");
    }

    @Test
    void lasCapacidadesPorTipoRigenParaLasRutasNuevas() {
        // Cada bicicleta cargaba 4: con capacidad 9 puede llevar un pedido de 9 en una sola ruta.
        ConfiguracionCorrida cfg = ConfiguracionCorrida.combinar(ConfiguracionCorrida.porDefecto(), new ConfiguracionCorrida(
                null, null, List.of(new VehiculoCfg("auto", 5, null, null), new VehiculoCfg("moto", 5, null, null), new VehiculoCfg("bicicleta", 9, null, null))));
        SimulationEngine m = motor(cfg, pedidos("01d00h00m:30,20,c1,09,36"));
        for (int i = 0; i < 200; i++) m.tick(DT);
        assertEquals(1, m.pedidos.size(), "9 caben en la unidad más grande configurada: sin fraccionar");
        assertEquals(9, m.capacidadDe(com.paqrap.model.VehicleType.BICI));
        assertTrue(m.vehiculos.stream().filter(v -> v.tipo == com.paqrap.model.VehicleType.BICI).allMatch(v -> v.tipo.getCapacity() == 4),
                "el enum base no cambia; la configuración es de la corrida");

        // Reducir las capacidades fracciona un pedido pendiente que ya no cabe.
        SimulationEngine n = motor(null, pedidos("01d00h00m:30,20,c2,20,36"));
        n.tick(DT); // el pedido ingresa y queda pendiente
        assertEquals(1, n.pedidos.size());
        n.aplicarConfiguracion(new ConfiguracionCorrida(null, null, List.of(new VehiculoCfg("auto", 10, null, null))));
        assertEquals(10, n.capacidadDe(com.paqrap.model.VehicleType.AUTO));
        assertEquals(2, n.pedidos.size(), "el pedido de 20 ya no cabe en ninguna unidad (máx. 10) y se fracciona");
        assertEquals(20, n.pedidos.values().stream().mapToInt(p -> p.cantidad).sum());
        for (int i = 0; i < 100; i++) n.tick(DT);
        assertTrue(n.pedidos.values().stream().allMatch(p -> p.estado != PedidoSim.Estado.PENDIENTE), "las partes se planifican");
    }

    @Test
    void snapshotMuestraLaCapacidadConfigurada() {
        SimulationEngine m = motor(null, List.of());
        m.aplicarConfiguracion(new ConfiguracionCorrida(null, null, List.of(new VehiculoCfg("auto", 30, null, null))));
        com.paqrap.simulacion.Corrida c = new com.paqrap.simulacion.Corrida("r", "5D", m, new SimulationClock(160));
        Map<String, Object> snap = SnapshotMapper.map(c);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> vehiculos = (List<Map<String, Object>>) snap.get("vehiculos");
        assertEquals(30, vehiculos.stream().filter(v -> "AUTO".equals(v.get("tipo"))).findFirst().orElseThrow().get("capacidad"));
        assertEquals(8, vehiculos.stream().filter(v -> "MOTO".equals(v.get("tipo"))).findFirst().orElseThrow().get("capacidad"));
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
