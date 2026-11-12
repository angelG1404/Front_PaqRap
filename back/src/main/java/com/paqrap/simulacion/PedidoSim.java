package com.paqrap.simulacion;

import com.paqrap.model.Order;

/** Estado vivo de un pedido dentro de una corrida (el {@link Order} queda inmutable para el algoritmo). */
final class PedidoSim {

    enum Estado { PENDIENTE, PLANIFICADO, EN_RUTA, ENTREGADO }

    final Order order;
    final String id;
    final String clienteId;
    final int x;
    final int y;
    final int cantidad;
    final double registroHoras;
    final double deadlineHoras;

    Estado estado = Estado.PENDIENTE;
    String vehiculoId;
    /** Almacén del despacho efectivo; se limpia si el pedido vuelve a estar pendiente. */
    String almacenDespacho;
    /** Hora en que el pedido salió del almacén; negativa mientras no ha salido. */
    double despachoHoras = -1;
    /** Hora en que terminó la entrega al cliente; negativa mientras no se entrega. */
    double entregadoHoras = -1;
    /** Llegada estimada al cliente; null si aún no tiene vehículo. */
    Double etaHoras;
    /** Llegada real al cliente (inicio de la entrega); negativo mientras no llega. */
    double llegadaHoras = -1;

    PedidoSim(Order order, String clienteId) {
        this.order = order;
        this.id = order.getId();
        this.clienteId = clienteId;
        this.x = (int) Math.round(order.getDestination().getX());
        this.y = (int) Math.round(order.getDestination().getY());
        this.cantidad = order.getQuantity();
        this.registroHoras = order.getPlacementTimeHours();
        this.deadlineHoras = order.getDeadlineHours();
    }

    boolean atrasado(double ahoraHoras) {
        if (llegadaHoras >= 0) return llegadaHoras > deadlineHoras + 1e-6;
        return ahoraHoras > deadlineHoras + 1e-6;
    }

    void liberar() {
        estado = Estado.PENDIENTE;
        vehiculoId = null;
        almacenDespacho = null;
        despachoHoras = -1;
        etaHoras = null;
    }
}
