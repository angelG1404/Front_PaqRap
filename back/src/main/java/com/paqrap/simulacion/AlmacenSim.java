package com.paqrap.simulacion;

import com.paqrap.config.ScenarioDefaults;

/** Almacén de la corrida: el central tiene inventario infinito; los intermedios se recargan a las 23:59:59. */
final class AlmacenSim {

    final String id;
    final String nombre;
    final int x;
    final int y;
    final boolean central;
    final int capacidad;
    int stock;

    AlmacenSim(ScenarioDefaults.AlmacenDef def) {
        this.id = def.id();
        this.nombre = def.nombre();
        this.x = def.x();
        this.y = def.y();
        this.central = def.central();
        this.capacidad = def.central() ? Integer.MAX_VALUE : ScenarioDefaults.CAPACIDAD_INTERMEDIO;
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
