package com.paqrap.simulacion;

import java.util.concurrent.ScheduledFuture;

/** Una ejecución de un escenario: su motor, su reloj, su topic WebSocket y su tarea periódica. */
public class Corrida {

    private final String runId;
    private final String escenario;
    private final String topic;
    private final SimulationEngine motor;
    private final SimulationClock reloj;
    private final long inicioRealMs = System.currentTimeMillis();
    private volatile long finRealMs;
    private ScheduledFuture<?> tarea;

    public Corrida(String runId, String escenario, SimulationEngine motor, SimulationClock reloj) {
        this.runId = runId;
        this.escenario = escenario;
        this.topic = "/topic/sim/" + runId;
        this.motor = motor;
        this.reloj = reloj;
    }

    public String getRunId() { return runId; }

    public String getEscenario() { return escenario; }

    public String getTopic() { return topic; }

    public SimulationEngine getMotor() { return motor; }

    public SimulationClock getReloj() { return reloj; }

    public String getEstado() { return motor.getEstado(); }

    public ScheduledFuture<?> getTarea() { return tarea; }

    public void setTarea(ScheduledFuture<?> tarea) { this.tarea = tarea; }

    public void marcarFin() {
        if (finRealMs == 0) finRealMs = System.currentTimeMillis();
    }

    public long segundosReales() {
        long fin = finRealMs == 0 ? System.currentTimeMillis() : finRealMs;
        return (fin - inicioRealMs) / 1000;
    }
}
