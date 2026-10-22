package com.paqrap.simulacion;

import com.paqrap.model.VehicleType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Estado vivo de una unidad de la flota: posición (puede ser fraccionaria dentro de un tramo), camino y paradas. */
final class VehiculoSim {

    enum Estado { LIBRE, EN_RUTA, ATENDIENDO, REGRESANDO, AVERIADO }

    final String id;
    final VehicleType tipo;
    double x;
    double y;
    Estado estado = Estado.LIBRE;

    /** Almacén donde está estacionada (LIBRE) o el último en el que cargó. */
    AlmacenSim almacen;
    /** Almacén al que se dirige cuando está REGRESANDO. */
    AlmacenSim destinoAlmacen;

    /** Nodos que faltan recorrer en el tramo actual (el primero es el siguiente nodo). */
    final ArrayDeque<int[]> camino = new ArrayDeque<>();
    /** Paradas de la ruta en ejecución; {@code indiceParada} apunta a la actual. */
    List<PedidoSim> paradas = new ArrayList<>();
    int indiceParada;
    /** Pedidos asignados mientras regresa: saldrá con ellos al llegar al almacén. */
    final List<PedidoSim> planPendiente = new ArrayList<>();

    int carga;
    double atendiendoHasta;
    double kmRecorridos;

    int averiaTipo;
    double averiaDesde;
    double averiaHasta;
    /** Hora en que la unidad averiada se remolca al almacén central; negativo si no aplica. */
    double remolqueEn = -1;

    VehiculoSim(String id, VehicleType tipo, AlmacenSim almacen) {
        this.id = id;
        this.tipo = tipo;
        this.almacen = almacen;
        this.x = almacen.x;
        this.y = almacen.y;
    }

    PedidoSim paradaActual() {
        return indiceParada < paradas.size() ? paradas.get(indiceParada) : null;
    }

    double kmRestantesTramo() {
        double km = 0;
        double px = x, py = y;
        for (int[] n : camino) {
            km += Math.abs(n[0] - px) + Math.abs(n[1] - py);
            px = n[0];
            py = n[1];
        }
        return km;
    }

    int nodoX() {
        return (int) Math.round(x);
    }

    int nodoY() {
        return (int) Math.round(y);
    }
}
