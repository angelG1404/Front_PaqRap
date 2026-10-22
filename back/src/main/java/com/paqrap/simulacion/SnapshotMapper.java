package com.paqrap.simulacion;

import com.paqrap.model.VehicleType;
import com.paqrap.planificador.PlanResultado;

import java.util.*;

/**
 * Convierte el estado del motor en el Snapshot del contrato (sección 6.1 de la arquitectura) para que
 * el dominio no dependa del JSON. Las rutas se envían como polilíneas comprimidas (solo vértices).
 */
public final class SnapshotMapper {

    private SnapshotMapper() {
    }

    public static Map<String, Object> map(Corrida corrida) {
        SimulationEngine m = corrida.getMotor();
        synchronized (m) {
            Map<String, Object> snap = new LinkedHashMap<>();
            snap.put("runId", corrida.getRunId());
            snap.put("escenario", corrida.getEscenario());
            snap.put("estado", m.estado);
            snap.put("simTime", SimTime.formato(m.ahora));
            snap.put("simTimeIso", SimTime.iso(m.fechaBase, m.ahora));
            snap.put("inicioIso", SimTime.iso(m.fechaBase, m.inicioHoras));
            snap.put("duracionSeg", corrida.segundosReales());
            snap.put("factorVelocidad", m.config.factorVelocidad());
            snap.put("algoritmo", m.algoritmo.nombre());
            snap.put("umbralesSemaforo", Map.of("verdeMinPct", m.config.verdeMinPct(), "ambarMinPct", m.config.ambarMinPct()));

            List<Map<String, Object>> vehiculos = new ArrayList<>();
            for (VehiculoSim v : m.vehiculos) vehiculos.add(vehiculo(m, v));
            snap.put("vehiculos", vehiculos);

            List<Map<String, Object>> pedidos = new ArrayList<>();
            for (PedidoSim p : m.pedidos.values()) {
                if (p.estado != PedidoSim.Estado.ENTREGADO) pedidos.add(pedido(m, p));
            }
            for (PedidoSim p : m.entregadosRecientes) pedidos.add(pedido(m, p));
            snap.put("pedidos", pedidos);

            List<Map<String, Object>> almacenes = new ArrayList<>();
            for (AlmacenSim a : m.almacenes) {
                Map<String, Object> am = new LinkedHashMap<>();
                am.put("id", a.id);
                am.put("x", a.x);
                am.put("y", a.y);
                am.put("stock", a.central ? null : a.stock);
                am.put("capacidad", a.central ? null : a.capacidad);
                almacenes.add(am);
            }
            snap.put("almacenes", almacenes);

            List<Map<String, Object>> bloqueos = new ArrayList<>();
            for (BloqueoSim b : m.bloqueosVigentes) {
                Map<String, Object> bm = new LinkedHashMap<>();
                bm.put("id", b.id);
                bm.put("puntos", b.vertices.stream().map(n -> List.of(n[0], n[1])).toList());
                bm.put("inicio", SimTime.formato(b.inicioHoras()));
                bm.put("fin", SimTime.formato(b.finHoras()));
                bloqueos.add(bm);
            }
            snap.put("bloqueos", bloqueos);

            snap.put("metricas", metricas(m));
            snap.put("planificacion", planificacion(m));
            if (m.causa != null) snap.put("causa", m.causa);
            if (!SimulationEngine.EN_CURSO.equals(m.estado)) snap.put("resumen", resumen(m));
            return snap;
        }
    }

    static Map<String, Object> pedido(SimulationEngine m, PedidoSim p) {
        Map<String, Object> pm = new LinkedHashMap<>();
        pm.put("id", p.id);
        pm.put("clienteId", p.clienteId);
        pm.put("x", p.x);
        pm.put("y", p.y);
        pm.put("cantidad", p.cantidad);
        pm.put("registradoEn", SimTime.formato(p.registroHoras));
        pm.put("deadline", SimTime.formato(p.deadlineHoras));
        pm.put("estado", p.estado.name());
        pm.put("vehiculoId", p.vehiculoId);
        pm.put("semaforo", m.semaforo(p));
        pm.put("eta", p.etaHoras == null ? null : SimTime.formato(p.etaHoras));
        pm.put("entregadoEn", p.estado == PedidoSim.Estado.ENTREGADO && p.llegadaHoras >= 0 ? SimTime.formato(p.llegadaHoras) : null);
        pm.put("atrasado", p.atrasado(m.ahora));
        return pm;
    }

    private static Map<String, Object> vehiculo(SimulationEngine m, VehiculoSim v) {
        Map<String, Object> vm = new LinkedHashMap<>();
        vm.put("id", v.id);
        vm.put("tipo", tipo(v.tipo));
        vm.put("x", redondear(v.x));
        vm.put("y", redondear(v.y));
        vm.put("estado", v.estado.name());
        AlmacenSim base = v.estado == VehiculoSim.Estado.REGRESANDO && v.destinoAlmacen != null ? v.destinoAlmacen : v.almacen;
        vm.put("almacenBase", base.id);
        vm.put("carga", v.carga);
        vm.put("capacidad", v.tipo.getCapacity());
        vm.put("kmRecorridos", redondear(v.kmRecorridos));

        List<PedidoSim> restantes = new ArrayList<>();
        if (v.estado == VehiculoSim.Estado.EN_RUTA || v.estado == VehiculoSim.Estado.ATENDIENDO) {
            restantes.addAll(v.paradas.subList(Math.min(v.indiceParada, v.paradas.size()), v.paradas.size()));
        } else if (v.estado == VehiculoSim.Estado.REGRESANDO) {
            restantes.addAll(v.planPendiente);
        }
        vm.put("ruta", ruta(m, v));
        vm.put("paradas", restantes.stream().map(p -> {
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("pedidoId", p.id);
            pm.put("x", p.x);
            pm.put("y", p.y);
            pm.put("cantidad", p.cantidad);
            pm.put("eta", p.etaHoras == null ? null : SimTime.formato(p.etaHoras));
            return pm;
        }).toList());

        PedidoSim proxima = v.estado == VehiculoSim.Estado.EN_RUTA ? v.paradaActual()
                : v.estado == VehiculoSim.Estado.ATENDIENDO && v.indiceParada + 1 < v.paradas.size() ? v.paradas.get(v.indiceParada + 1)
                : null;
        if (proxima != null) {
            vm.put("proximaParada", Map.of("pedidoId", proxima.id, "x", proxima.x, "y", proxima.y,
                    "eta", proxima.etaHoras == null ? SimTime.formato(m.ahora) : SimTime.formato(proxima.etaHoras)));
        } else {
            vm.put("proximaParada", null);
        }

        String peor = "VERDE";
        for (PedidoSim p : restantes) peor = peor(peor, m.semaforo(p));
        vm.put("semaforo", v.estado == VehiculoSim.Estado.AVERIADO ? "ROJO" : peor);

        if (v.estado == VehiculoSim.Estado.AVERIADO) {
            Map<String, Object> am = new LinkedHashMap<>();
            am.put("tipo", v.averiaTipo);
            am.put("desde", SimTime.formato(v.averiaDesde));
            am.put("hasta", SimTime.formato(v.averiaHasta));
            am.put("remolcado", v.remolqueEn < 0 && v.averiaTipo != 1);
            vm.put("averia", am);
        } else {
            vm.put("averia", null);
        }
        return vm;
    }

    /** Polilínea desde la posición actual por el tramo vigente y las paradas siguientes. */
    private static List<List<Double>> ruta(SimulationEngine m, VehiculoSim v) {
        List<double[]> puntos = new ArrayList<>();
        puntos.add(new double[]{v.x, v.y});
        for (int[] n : v.camino) puntos.add(new double[]{n[0], n[1]});

        int[] previo = null;
        List<PedidoSim> siguientes = List.of();
        if (v.estado == VehiculoSim.Estado.EN_RUTA || v.estado == VehiculoSim.Estado.ATENDIENDO) {
            PedidoSim actual = v.paradaActual();
            if (actual != null) {
                previo = new int[]{actual.x, actual.y};
                siguientes = v.paradas.subList(v.indiceParada + 1, v.paradas.size());
            }
        } else if (v.estado == VehiculoSim.Estado.REGRESANDO && v.destinoAlmacen != null) {
            previo = new int[]{v.destinoAlmacen.x, v.destinoAlmacen.y};
            siguientes = v.planPendiente;
        }
        if (previo != null) {
            for (PedidoSim p : siguientes) {
                List<int[]> tramo = m.caminoEntre(previo[0], previo[1], p.x, p.y);
                for (int i = 1; i < tramo.size(); i++) puntos.add(new double[]{tramo.get(i)[0], tramo.get(i)[1]});
                previo = new int[]{p.x, p.y};
            }
        }

        List<List<Double>> comprimida = new ArrayList<>();
        for (int i = 0; i < puntos.size(); i++) {
            double[] p = puntos.get(i);
            if (i > 0 && i < puntos.size() - 1) {
                double[] a = puntos.get(i - 1), c = puntos.get(i + 1);
                boolean mismaRecta = (a[0] == p[0] && p[0] == c[0]) || (a[1] == p[1] && p[1] == c[1]);
                boolean sinRetroceso = (p[0] - a[0]) * (c[0] - p[0]) >= 0 && (p[1] - a[1]) * (c[1] - p[1]) >= 0;
                if (mismaRecta && sinRetroceso) continue;
            }
            if (!comprimida.isEmpty()) {
                List<Double> u = comprimida.get(comprimida.size() - 1);
                if (u.get(0) == redondear(p[0]) && u.get(1) == redondear(p[1])) continue;
            }
            comprimida.add(List.of(redondear(p[0]), redondear(p[1])));
        }
        return comprimida;
    }

    private static Map<String, Object> metricas(SimulationEngine m) {
        int enRuta = 0, pendientes = 0;
        for (PedidoSim p : m.pedidos.values()) {
            if (p.estado == PedidoSim.Estado.EN_RUTA) enRuta++;
            else if (p.estado == PedidoSim.Estado.PENDIENTE || p.estado == PedidoSim.Estado.PLANIFICADO) pendientes++;
        }
        Map<String, Integer> flota = new LinkedHashMap<>();
        flota.put("auto", 0);
        flota.put("moto", 0);
        flota.put("bicicleta", 0);
        int averiados = 0, enMovimiento = 0;
        for (VehiculoSim v : m.vehiculos) {
            String clave = v.tipo == VehicleType.AUTO ? "auto" : v.tipo == VehicleType.MOTO ? "moto" : "bicicleta";
            flota.merge(clave, 1, Integer::sum);
            if (v.estado == VehiculoSim.Estado.AVERIADO) averiados++;
            if (v.estado != VehiculoSim.Estado.LIBRE && v.estado != VehiculoSim.Estado.AVERIADO) enMovimiento++;
        }
        Map<String, Object> mm = new LinkedHashMap<>();
        mm.put("pedidosTotal", m.pedidos.size());
        mm.put("entregados", m.entregados);
        mm.put("enRuta", enRuta);
        mm.put("pendientes", pendientes);
        mm.put("atrasados", m.atrasados());
        mm.put("costoTotal", Math.round(m.costoTotal * 100) / 100.0);
        mm.put("distanciaKm", Math.round(m.distanciaTotal * 10) / 10.0);
        mm.put("flota", flota);
        mm.put("vehiculosOperando", enMovimiento);
        mm.put("vehiculosAveriados", averiados);
        mm.put("pedidosPorIngresar", m.pedidosPorIngresar());
        return mm;
    }

    private static Map<String, Object> planificacion(SimulationEngine m) {
        Map<String, Object> pm = new LinkedHashMap<>();
        pm.put("algoritmo", m.algoritmo.nombre());
        pm.put("enCurso", m.planificando());
        pm.put("ejecuciones", m.planificaciones);
        PlanResultado ultimo = m.ultimoPlan;
        if (ultimo != null) {
            int asignados = ultimo.rutas().values().stream().mapToInt(List::size).sum();
            pm.put("ultima", SimTime.formato(ultimo.planificadoEnHoras()));
            pm.put("duracionMs", ultimo.duracionMs());
            pm.put("pedidosAsignados", asignados);
            pm.put("noAsignados", ultimo.noAsignados().size());
        }
        return pm;
    }

    private static Map<String, Object> resumen(SimulationEngine m) {
        int noAsignados = 0;
        for (PedidoSim p : m.pedidos.values()) if (p.estado == PedidoSim.Estado.PENDIENTE) noAsignados++;
        int atrasados = m.atrasados();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("pedidosTotal", m.pedidos.size());
        r.put("entregados", m.entregados);
        r.put("atrasados", atrasados);
        r.put("noAsignados", noAsignados);
        r.put("costoTotal", Math.round(m.costoTotal * 100) / 100.0);
        r.put("distanciaKm", Math.round(m.distanciaTotal * 10) / 10.0);
        r.put("cumplimientoPct", m.pedidos.isEmpty() ? 100.0
                : Math.round((m.pedidos.size() - atrasados) * 1000.0 / m.pedidos.size()) / 10.0);
        r.put("simTimeFinal", SimTime.formato(m.ahora));
        return r;
    }

    private static String tipo(VehicleType t) {
        return t == VehicleType.AUTO ? "AUTO" : t == VehicleType.MOTO ? "MOTO" : "BICICLETA";
    }

    private static String peor(String a, String b) {
        int ra = "ROJO".equals(a) ? 2 : "AMBAR".equals(a) ? 1 : 0;
        int rb = "ROJO".equals(b) ? 2 : "AMBAR".equals(b) ? 1 : 0;
        return ra >= rb ? a : b;
    }

    private static double redondear(double v) {
        return Math.round(v * 1000) / 1000.0;
    }
}
