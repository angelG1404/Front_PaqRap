package com.paqrap.simulacion;

import com.paqrap.config.ScenarioDefaults;
import com.paqrap.model.Order;
import com.paqrap.planificador.AlgorithmSelector;
import com.paqrap.web.dto.EscenarioActivoDto;
import com.paqrap.web.dto.ParametrosCorridaDto;
import com.paqrap.web.ws.SnapshotPublisher;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

/** Crea, consulta y detiene corridas: una por tipo de escenario, cada una con su motor y su topic. */
@Service
public class ScenarioRunner {

    private static final Logger log = LoggerFactory.getLogger(ScenarioRunner.class);
    private static final Set<String> TIPOS = Set.of(ScenarioConfig.DIARIO, ScenarioConfig.SIM_5D, ScenarioConfig.COLAPSO);

    @Autowired
    private SnapshotPublisher publisher;

    @Value("${paqrap.sim.tick-ms:1000}")
    private long tickMs;
    @Value("${paqrap.sim.factor.diario:1}")
    private double factorDiario;
    @Value("${paqrap.sim.factor.5d:160}")
    private double factor5d;
    @Value("${paqrap.sim.factor.colapso:480}")
    private double factorColapso;
    @Value("${paqrap.plan.intervalo-min.diario:1}")
    private double intervaloDiario;
    @Value("${paqrap.plan.intervalo-min.simulacion:5}")
    private double intervaloSimulacion;
    @Value("${paqrap.plan.limite-ms:1500}")
    private long limitePlanMs;
    @Value("${paqrap.plan.max-iteraciones:250}")
    private int maxIteraciones;
    @Value("${paqrap.plan.max-pedidos:60}")
    private int maxPedidosPorPlan;
    @Value("${paqrap.diario.auto-iniciar:true}")
    private boolean diarioAutomatico;
    @Value("${paqrap.diario.algoritmo:ALNS}")
    private String algoritmoDiario;

    private final Map<String, Corrida> activasPorTipo = new ConcurrentHashMap<>();
    private final Map<String, Corrida> porRunId = new ConcurrentHashMap<>();
    private final ScheduledExecutorService ticks = Executors.newScheduledThreadPool(3, hilo("paqrap-tick"));
    private final ExecutorService planificador = Executors.newFixedThreadPool(2, hilo("paqrap-plan"));

    @EventListener(ApplicationReadyEvent.class)
    public void iniciarDiarioAutomatico() {
        if (!diarioAutomatico) return;
        ParametrosCorridaDto params = new ParametrosCorridaDto();
        params.setAlgoritmo(algoritmoDiario);
        try {
            iniciarCorrida(ScenarioConfig.DIARIO, params, List.of(), List.of());
            log.info("Operación diaria iniciada automáticamente.");
        } catch (RuntimeException e) {
            log.warn("No se pudo iniciar la operación diaria automática: {}", e.getMessage());
        }
    }

    public synchronized Corrida iniciarCorrida(String tipo, ParametrosCorridaDto params, List<Order> pedidos, List<String> lineasBloqueos) {
        String clave = tipo == null ? "" : tipo.trim().toUpperCase();
        if (!TIPOS.contains(clave)) throw new IllegalArgumentException("Escenario desconocido: " + tipo + ". Usa DIARIO, 5D o COLAPSO.");
        Corrida existente = activasPorTipo.get(clave);
        if (existente != null && existente.getMotor().enCurso()) {
            throw new IllegalStateException("Ya hay una corrida activa para el escenario " + clave + ".");
        }
        boolean diario = ScenarioConfig.DIARIO.equals(clave);
        if (!diario && (pedidos == null || pedidos.isEmpty())) {
            throw new IllegalArgumentException("Sube un archivo de pedidos válido antes de iniciar la simulación.");
        }

        Map<String, Integer> flota = validarFlota(params.getFlota());
        double verde = umbral(params.getUmbralesSemaforo(), "verdeMinPct", ScenarioDefaults.VERDE_MIN_PCT);
        double ambar = umbral(params.getUmbralesSemaforo(), "ambarMinPct", ScenarioDefaults.AMBAR_MIN_PCT);
        if (ambar < 0 || verde > 100 || ambar >= verde) {
            throw new IllegalArgumentException("Umbrales de semáforo inválidos: se requiere 0 ≤ ámbar < verde ≤ 100.");
        }

        double factor = diario ? factorDiario : ScenarioConfig.SIM_5D.equals(clave) ? factor5d : factorColapso;
        ScenarioConfig config = new ScenarioConfig(clave, factor, diario ? intervaloDiario : intervaloSimulacion,
                limitePlanMs, maxIteraciones, maxPedidosPorPlan, verde, ambar);

        // En DIARIO el reloj es el real; en las simulaciones el día 01 del archivo es la fecha de inicio.
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime base = diario ? ahora.toLocalDate().atStartOfDay() : fechaInicio(params.getFechaInicio()).atStartOfDay();
        double inicioHoras = diario ? ChronoUnit.MILLIS.between(base, ahora) / 3_600_000.0 : 0.0;

        String runId = "run-" + clave.toLowerCase() + "-" + System.currentTimeMillis();
        List<BloqueoSim> bloqueos = new ArrayList<>();
        SimulationEngine motor = new SimulationEngine(config, AlgorithmSelector.seleccionar(params.getAlgoritmo(), maxIteraciones),
                planificador, base, inicioHoras, flota, pedidos, bloqueos);
        if (lineasBloqueos != null && !lineasBloqueos.isEmpty()) agregarBloqueos(motor, lineasBloqueos);

        Corrida corrida = new Corrida(runId, clave, motor, new SimulationClock(factor));
        corrida.setTarea(ticks.scheduleAtFixedRate(() -> ejecutarTick(corrida), 0, tickMs, TimeUnit.MILLISECONDS));
        activasPorTipo.put(clave, corrida);
        porRunId.put(runId, corrida);
        log.info("Corrida {} iniciada ({}, algoritmo {}, factor {}x, {} pedidos, {} bloqueos).",
                runId, clave, motor.algoritmo.nombre(), factor, pedidos == null ? 0 : pedidos.size(),
                lineasBloqueos == null ? 0 : lineasBloqueos.size());
        return corrida;
    }

    private void ejecutarTick(Corrida corrida) {
        try {
            SimulationEngine motor = corrida.getMotor();
            motor.tick(corrida.getReloj().tomarDeltaHoras());
            if (!motor.enCurso()) corrida.marcarFin();
            publisher.publicar(corrida.getRunId(), SnapshotMapper.map(corrida));
            if (!motor.enCurso() && corrida.getTarea() != null) corrida.getTarea().cancel(false);
        } catch (Exception e) {
            // Una excepción no capturada cancelaría silenciosamente la tarea periódica.
            log.error("Error en el tick de {}: {}", corrida.getRunId(), e.getMessage(), e);
        }
    }

    public Corrida detener(String runId) {
        Corrida corrida = porRunId.get(runId);
        if (corrida == null) return null;
        corrida.getMotor().detener("Corrida detenida por el operador.");
        corrida.marcarFin();
        if (corrida.getTarea() != null) corrida.getTarea().cancel(false);
        publisher.publicar(runId, SnapshotMapper.map(corrida));
        return corrida;
    }

    public int agregarBloqueos(SimulationEngine motor, List<String> lineas) {
        List<BloqueoSim> nuevos = new ArrayList<>();
        for (String linea : lineas) nuevos.add(BloqueoSim.desdeLinea(motor.siguienteIdBloqueo(), linea));
        return motor.agregarBloqueos(nuevos);
    }

    public List<EscenarioActivoDto> getActivos() {
        List<EscenarioActivoDto> lista = new ArrayList<>();
        for (Corrida c : activasPorTipo.values()) {
            if (c.getMotor().enCurso()) lista.add(new EscenarioActivoDto(c.getRunId(), c.getEscenario(), c.getEstado(), c.getTopic()));
        }
        lista.sort(Comparator.comparing(EscenarioActivoDto::getEscenario));
        return lista;
    }

    public Corrida getByRunId(String runId) {
        return porRunId.get(runId);
    }

    public Corrida getByType(String tipo) {
        return activasPorTipo.get(tipo.toUpperCase());
    }

    public Collection<Corrida> todas() {
        return porRunId.values();
    }

    private static Map<String, Integer> validarFlota(Map<String, Integer> flota) {
        if (flota == null || flota.isEmpty()) return ScenarioDefaults.FLOTA;
        Map<String, Integer> resultado = new LinkedHashMap<>();
        int total = 0;
        for (String tipo : List.of("auto", "moto", "bicicleta")) {
            int n = flota.getOrDefault(tipo, 0) == null ? 0 : flota.getOrDefault(tipo, 0);
            if (n < 0) throw new IllegalArgumentException("La cantidad de " + tipo + " no puede ser negativa.");
            resultado.put(tipo, n);
            total += n;
        }
        if (total == 0) throw new IllegalArgumentException("La flota debe tener al menos una unidad.");
        return resultado;
    }

    private static double umbral(Map<String, Double> umbrales, String clave, double defecto) {
        if (umbrales == null || umbrales.get(clave) == null) return defecto;
        return umbrales.get(clave);
    }

    private static LocalDate fechaInicio(String texto) {
        if (texto == null || texto.isBlank()) return LocalDate.now();
        try {
            return LocalDateTime.parse(texto).toLocalDate();
        } catch (RuntimeException e) {
            try {
                return LocalDate.parse(texto.length() >= 10 ? texto.substring(0, 10) : texto);
            } catch (RuntimeException ignored) {
                return LocalDate.now();
            }
        }
    }

    private static ThreadFactory hilo(String prefijo) {
        return new ThreadFactory() {
            private int n;

            @Override
            public synchronized Thread newThread(Runnable r) {
                Thread t = new Thread(r, prefijo + "-" + (++n));
                t.setDaemon(true);
                return t;
            }
        };
    }

    @PreDestroy
    public void cerrar() {
        ticks.shutdownNow();
        planificador.shutdownNow();
    }
}
