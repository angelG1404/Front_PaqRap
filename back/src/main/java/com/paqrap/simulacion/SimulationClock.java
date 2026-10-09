package com.paqrap.simulacion;

public class SimulationClock {
    private double currentMinutes;
    private final double speedFactor;

    public SimulationClock(double startMinutes, double speedFactor) {
        this.currentMinutes = startMinutes;
        this.speedFactor = speedFactor;
    }

    public synchronized void avanzar(double realSecondsElapsed) {
        this.currentMinutes += (realSecondsElapsed / 60.0) * speedFactor;
    }

    public synchronized double getCurrentMinutes() {
        return currentMinutes;
    }

    public String formatSimTime() {
        int totalMinutes = (int) currentMinutes;
        int days = totalMinutes / (24 * 60);
        int hours = (totalMinutes % (24 * 60)) / 60;
        int minutes = totalMinutes % 60;
        return String.format("%02dd%02dh%02dm", days, hours, minutes);
    }
}
