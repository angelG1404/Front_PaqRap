package com.paqrap.simulacion;

/**
 * Único lugar donde se convierte tiempo real en tiempo simulado. Usa el tiempo real transcurrido
 * (no la cantidad de ticks), así una corrida 5D dura lo mismo aunque algún tick se retrase.
 */
public class SimulationClock {

    private final double speedFactor;
    private long ultimoNanos;

    public SimulationClock(double speedFactor) {
        this.speedFactor = speedFactor;
        this.ultimoNanos = System.nanoTime();
    }

    /** Horas simuladas transcurridas desde la llamada anterior. */
    public synchronized double tomarDeltaHoras() {
        long ahora = System.nanoTime();
        double segundosReales = (ahora - ultimoNanos) / 1e9;
        ultimoNanos = ahora;
        return segundosReales * speedFactor / 3600.0;
    }

    public double getSpeedFactor() {
        return speedFactor;
    }
}
