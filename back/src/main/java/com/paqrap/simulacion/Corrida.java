package com.paqrap.simulacion;

import com.paqrap.model.*;
import com.paqrap.web.dto.ParametrosCorridaDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class Corrida {
    private String runId;
    private String escenario;
    private String estado;
    private String topic;
    private SimulationClock clock;
    private LocalDateTime baseRealTime;
    private List<Vehicle> vehicles;
    private List<Order> allOrders;
    private List<Order> activeOrders;
    private List<Warehouse> warehouses;
    private ParametrosCorridaDto params;
    private ScheduledFuture<?> task;
    private String causa;
    private Solution lastSolution;
    private double totalCost = 0.0;

    public Corrida() {}

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public String getEscenario() { return escenario; }
    public void setEscenario(String escenario) { this.escenario = escenario; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public SimulationClock getClock() { return clock; }
    public void setClock(SimulationClock clock) { this.clock = clock; }

    public LocalDateTime getBaseRealTime() { return baseRealTime; }
    public void setBaseRealTime(LocalDateTime baseRealTime) { this.baseRealTime = baseRealTime; }

    public List<Vehicle> getVehicles() { return vehicles; }
    public void setVehicles(List<Vehicle> vehicles) { this.vehicles = vehicles; }

    public List<Order> getAllOrders() { return allOrders; }
    public void setAllOrders(List<Order> allOrders) { this.allOrders = allOrders; }

    public List<Order> getActiveOrders() { return activeOrders; }
    public void setActiveOrders(List<Order> activeOrders) { this.activeOrders = activeOrders; }

    public List<Warehouse> getWarehouses() { return warehouses; }
    public void setWarehouses(List<Warehouse> warehouses) { this.warehouses = warehouses; }

    public ParametrosCorridaDto getParams() { return params; }
    public void setParams(ParametrosCorridaDto params) { this.params = params; }

    public ScheduledFuture<?> getTask() { return task; }
    public void setTask(ScheduledFuture<?> task) { this.task = task; }

    public String getCausa() { return causa; }
    public void setCausa(String causa) { this.causa = causa; }

    public Solution getLastSolution() { return lastSolution; }
    public void setLastSolution(Solution lastSolution) { this.lastSolution = lastSolution; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
}
