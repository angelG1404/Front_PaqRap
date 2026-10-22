package com.paqrap.simulacion;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Formato de tiempo del contrato: DDdHHhMMm, donde el día 01 es el primer día de la corrida. */
public final class SimTime {

    private SimTime() {
    }

    public static String formato(double horas) {
        int minutos = (int) Math.floor(Math.max(0, horas) * 60 + 1e-6);
        return String.format("%02dd%02dh%02dm", minutos / 1440 + 1, (minutos % 1440) / 60, minutos % 60);
    }

    public static String iso(LocalDateTime base, double horas) {
        return base.plusSeconds(Math.round(horas * 3600)).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
