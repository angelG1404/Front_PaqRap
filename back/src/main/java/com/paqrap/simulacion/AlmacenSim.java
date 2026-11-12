package com.paqrap.simulacion;

import com.paqrap.config.ConfiguracionCorrida;

/** Almacén de la corrida: el central tiene inventario infinito; los intermedios se recargan a las 23:59:59. */
final class AlmacenSim {

    final String id;
    final String nombre;
    /** Ubicación y capacidad se pueden modificar durante la corrida (bajo el candado del motor). */
    int x;
    int y;
    final boolean central;
    int capacidad;
    int stock;

    AlmacenSim(ConfiguracionCorrida.AlmacenCfg cfg, boolean central) {
        this.id = cfg.id();
        this.nombre = cfg.nombre();
        this.x = cfg.x();
        this.y = cfg.y();
        this.central = central;
        this.capacidad = central ? Integer.MAX_VALUE : cfg.capacidad();
        this.stock = capacidad;
    }

    boolean retirar(int unidades) {
        if (central) return true;
        if (stock < unidades) return false;
        stock -= unidades;
        return true;
    }

    void recargar() {
        stock = capacidad;
    }

    int distanciaManhattan(double px, double py) {
        return (int) Math.round(Math.abs(px - x) + Math.abs(py - y));
    }
}
