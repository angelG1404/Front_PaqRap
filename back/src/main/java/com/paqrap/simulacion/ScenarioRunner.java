package com.paqrap.simulacion;

import com.paqrap.model.*;
import com.paqrap.web.dto.EscenarioActivoDto;
import com.paqrap.web.dto.ParametrosCorridaDto;
import com.paqrap.web.ws.SnapshotPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
public class ScenarioRunner {

    @Autowired
    private SnapshotPublisher publisher;

    private final Map<String, Corrida> activeRunsByType = new ConcurrentHashMap<>();
    private final Map<String, Corrida> runsById = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    @PostConstruct
    public void initDiarioAutomatico() {
        try {
            ParametrosCorridaDto params = new ParametrosCorridaDto();
            params.setAlgoritmo("ALNS");
            params.setFechaInicio(LocalDateTime.now().toString());
            params.setFlota(Map.of("auto", 10, "moto", 15, "bicicleta", 12));
            iniciarCorrida("DIARIO", params, new ArrayList<>());
        } catch (Exception e) {
            // ignore
        }
    }

    public synchronized Corrida iniciarCorrida(String tipo, ParametrosCorridaDto params, List<Order> initialOrders) {
        String upperTipo = tipo.toUpperCase();
        if (activeRunsByType.containsKey(upperTipo)) {
            Corrida existing = activeRunsByType.get(upperTipo);
            if ("EN_CURSO".equals(existing.getEstado())) {
                throw new IllegalStateException("Ya hay una corrida activa para el escenario " + upperTipo);
            }
        }

        String runId = "run-" + upperTipo.toLowerCase() + "-" + System.currentTimeMillis();
        String topic = "/topic/sim/" + runId;

        Corrida corrida = new Corrida();
        corrida.setRunId(runId);
        corrida.setEscenario(upperTipo);
        corrida.setEstado("EN_CURSO");
        corrida.setTopic(topic);
        corrida.setParams(params);
        corrida.setAllOrders(initialOrders != null ? initialOrders : new ArrayList<>());
        corrida.setActiveOrders(new ArrayList<>());

        Location centralLoc = new Location("CENTRAL", "Almacén Central", 27, 14, Location.LocationType.CENTRAL_WAREHOUSE);
        Location norOesteLoc = new Location("INTERMEDIO_NOROESTE", "Intermedio Nor-Oeste", 12, 38, Location.LocationType.INTERMEDIATE_WAREHOUSE);
        Location esteLoc = new Location("INTERMEDIO_ESTE", "Intermedio Este", 57, 27, Location.LocationType.INTERMEDIATE_WAREHOUSE);

        Warehouse central = new Warehouse("CENTRAL", centralLoc, true, Integer.MAX_VALUE);
        Warehouse norOeste = new Warehouse("INTERMEDIO_NOROESTE", norOesteLoc, false, 1000);
        Warehouse este = new Warehouse("INTERMEDIO_ESTE", esteLoc, false, 1000);

        corrida.setWarehouses(List.of(central, norOeste, este));

        List<Vehicle> vehicles = new ArrayList<>();
        Map<String, Integer> flota = params.getFlota() != null ? params.getFlota() : Map.of("auto", 10, "moto", 15, "bicicleta", 12);

        int idx = 1;
        for (int i = 0; i < flota.getOrDefault("auto", 10); i++) {
            vehicles.add(new Vehicle("A-" + String.format("%02d", idx++), VehicleType.AUTO, centralLoc));
        }
        idx = 1;
        for (int i = 0; i < flota.getOrDefault("moto", 15); i++) {
            vehicles.add(new Vehicle("M-" + String.format("%02d", idx++), VehicleType.MOTO, centralLoc));
        }
        idx = 1;
        for (int i = 0; i < flota.getOrDefault("bicicleta", 12); i++) {
            vehicles.add(new Vehicle("B-" + String.format("%02d", idx++), VehicleType.BICI, centralLoc));
        }
        corrida.setVehicles(vehicles);

        double speedFactor = "DIARIO".equals(upperTipo) ? 1.0 : 160.0;
        corrida.setClock(new SimulationClock(480.0, speedFactor));
        try {
            corrida.setBaseRealTime(LocalDateTime.parse(params.getFechaInicio()));
        } catch (Exception e) {
            corrida.setBaseRealTime(LocalDateTime.now());
        }

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> ejecutarTick(corrida), 0, 1, TimeUnit.SECONDS);
        corrida.setTask(task);

        activeRunsByType.put(upperTipo, corrida);
        runsById.put(runId, corrida);
        return corrida;
    }

    private void ejecutarTick(Corrida corrida) {
        if (!"EN_CURSO".equals(corrida.getEstado())) return;

        corrida.getClock().avanzar(1.0);

        double currentMins = corrida.getClock().getCurrentMinutes();
        if (corrida.getAllOrders() != null) {
            for (Order o : corrida.getAllOrders()) {
                if (o.getPlacementTimeHours() * 60 <= currentMins && !corrida.getActiveOrders().contains(o)) {
                    corrida.getActiveOrders().add(o);
                }
            }
        }

        if ("5D".equals(corrida.getEscenario()) && currentMinutesPassed(corrida) >= 120 * 60) {
            corrida.setEstado("FINALIZADO");
            corrida.getTask().cancel(false);
        } else if ("COLAPSO".equals(corrida.getEscenario())) {
            for (Order o : corrida.getActiveOrders()) {
                if (o.getActualDeliveryTimeHours() < 0 && (o.getPlacementTimeHours() + o.getMaxDeliveryWindowHours()) * 60 < currentMins) {
                    corrida.setEstado("COLAPSADO");
                    corrida.setCausa("Saturación logística: Pedido " + o.getId() + " fuera de plazo.");
                    corrida.getTask().cancel(false);
                    break;
                }
            }
        }

        Map<String, Object> snapshot = SnapshotMapper.map(corrida);
        publisher.publicar(corrida.getRunId(), snapshot);
    }

    private double currentMinutesPassed(Corrida corrida) {
        return corrida.getClock().getCurrentMinutes() - 480.0;
    }

    public List<EscenarioActivoDto> getActivos() {
        List<EscenarioActivoDto> list = new ArrayList<>();
        for (Corrida c : activeRunsByType.values()) {
            if ("EN_CURSO".equals(c.getEstado())) {
                list.add(new EscenarioActivoDto(c.getRunId(), c.getEscenario(), c.getEstado(), c.getTopic()));
            }
        }
        return list;
    }

    public Corrida getByRunId(String runId) {
        return runsById.get(runId);
    }

    public Corrida getByType(String tipo) {
        return activeRunsByType.get(tipo.toUpperCase());
    }

    public synchronized void registrarPedidoDiario(Order order) {
        Corrida diario = activeRunsByType.get("DIARIO");
        if (diario == null || !"EN_CURSO".equals(diario.getEstado())) {
            throw new IllegalStateException("La operación diaria no está activa.");
        }
        diario.getAllOrders().add(order);
        diario.getActiveOrders().add(order);
    }
}
