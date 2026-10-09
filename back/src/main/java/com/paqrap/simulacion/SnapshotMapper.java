package com.paqrap.simulacion;

import com.paqrap.model.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;

public class SnapshotMapper {

    public static Map<String, Object> map(Corrida corrida) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("runId", corrida.getRunId());
        snap.put("escenario", corrida.getEscenario());
        snap.put("estado", corrida.getEstado());
        snap.put("simTime", corrida.getClock().formatSimTime());

        if (corrida.getBaseRealTime() != null) {
            double minutesPassed = corrida.getClock().getCurrentMinutes();
            LocalDateTime simTimeIso = corrida.getBaseRealTime().plusMinutes((long) minutesPassed);
            snap.put("simTimeIso", simTimeIso.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } else {
            snap.put("simTimeIso", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        List<Map<String, Object>> vehiculosList = new ArrayList<>();
        if (corrida.getVehicles() != null) {
            for (Vehicle v : corrida.getVehicles()) {
                Map<String, Object> vMap = new LinkedHashMap<>();
                vMap.put("id", v.getId());
                vMap.put("tipo", v.getType() != null ? v.getType().getDisplayName().toUpperCase() : "AUTO");
                double vx = v.getStartDepot() != null ? v.getStartDepot().getX() : 27;
                double vy = v.getStartDepot() != null ? v.getStartDepot().getY() : 14;
                vMap.put("x", Math.round(vx));
                vMap.put("y", Math.round(vy));
                vMap.put("estado", "LIBRE");
                vMap.put("almacenBase", "CENTRAL");
                vMap.put("carga", 0);
                vMap.put("capacidad", v.getCapacity());

                List<List<Integer>> ruta = new ArrayList<>();
                ruta.add(List.of((int) Math.round(vx), (int) Math.round(vy)));
                vMap.put("ruta", ruta);
                vMap.put("proximaParada", null);
                vMap.put("semaforo", "VERDE");
                vehiculosList.add(vMap);
            }
        }
        snap.put("vehiculos", vehiculosList);

        List<Map<String, Object>> pedidosList = new ArrayList<>();
        int entregados = 0, enRuta = 0, pendientes = 0, atrasados = 0;
        if (corrida.getActiveOrders() != null) {
            for (Order o : corrida.getActiveOrders()) {
                Map<String, Object> pMap = new LinkedHashMap<>();
                pMap.put("id", o.getId());
                pMap.put("clienteId", o.getDestination() != null ? o.getDestination().getId() : "c9100");
                pMap.put("x", o.getDestination() != null ? (int) Math.round(o.getDestination().getX()) : 0);
                pMap.put("y", o.getDestination() != null ? (int) Math.round(o.getDestination().getY()) : 0);
                pMap.put("cantidad", o.getQuantity());
                pMap.put("registradoEn", formatMinutes(o.getPlacementTimeHours() * 60));
                pMap.put("deadline", formatMinutes(o.getDeadlineHours() * 60));

                String estado = "PENDIENTE";
                if (o.getActualDeliveryTimeHours() >= 0) {
                    estado = "ENTREGADO";
                    entregados++;
                } else {
                    pendientes++;
                }
                pMap.put("estado", estado);
                pMap.put("vehiculoId", null);
                pMap.put("semaforo", "VERDE");
                pedidosList.add(pMap);
            }
        }
        snap.put("pedidos", pedidosList);

        List<Map<String, Object>> almacenesList = new ArrayList<>();
        if (corrida.getWarehouses() != null) {
            for (Warehouse w : corrida.getWarehouses()) {
                Map<String, Object> wMap = new LinkedHashMap<>();
                wMap.put("id", w.getId());
                wMap.put("x", w.getLocation() != null ? (int) Math.round(w.getLocation().getX()) : 0);
                wMap.put("y", w.getLocation() != null ? (int) Math.round(w.getLocation().getY()) : 0);
                wMap.put("stock", w.isCentral() ? null : w.getCurrentStock());
                almacenesList.add(wMap);
            }
        }
        snap.put("almacenes", almacenesList);

        Map<String, Object> metricas = new LinkedHashMap<>();
        int total = corrida.getAllOrders() != null ? corrida.getAllOrders().size() : pedidosList.size();
        metricas.put("pedidosTotal", total);
        metricas.put("entregados", entregados);
        metricas.put("enRuta", enRuta);
        metricas.put("pendientes", pendientes);
        metricas.put("atrasados", atrasados);
        snap.put("metricas", metricas);

        if (corrida.getCausa() != null) {
            snap.put("causa", corrida.getCausa());
        }

        if ("FINALIZADO".equals(corrida.getEstado()) || "COLAPSADO".equals(corrida.getEstado())) {
            Map<String, Object> resumen = new LinkedHashMap<>();
            resumen.put("pedidosTotal", total);
            resumen.put("entregados", entregados);
            resumen.put("atrasados", atrasados);
            resumen.put("noAsignados", Math.max(0, total - entregados - enRuta - pendientes));
            resumen.put("costoTotal", corrida.getTotalCost());
            snap.put("resumen", resumen);
        }

        return snap;
    }

    private static String formatMinutes(double totalMins) {
        int mins = (int) totalMins;
        int days = mins / (24 * 60);
        int hours = (mins % (24 * 60)) / 60;
        int minutes = mins % 60;
        return String.format("%02dd%02dh%02dm", days, hours, minutes);
    }
}
