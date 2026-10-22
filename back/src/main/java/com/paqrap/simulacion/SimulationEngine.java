package com.paqrap.simulacion;

import com.paqrap.config.ScenarioDefaults;
import com.paqrap.model.Location;
import com.paqrap.model.Order;
import com.paqrap.model.Roadblock;
import com.paqrap.model.VehicleType;
import com.paqrap.planificador.InstanciaPlanificacion;
import com.paqrap.planificador.PlanResultado;
import com.paqrap.planificador.PlanningAlgorithm;
import com.paqrap.planificador.VehiculoDisponible;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Motor de una corrida (SimulationEngine de la arquitectura). En cada tick avanza el reloj, ingresa
 * pedidos, mueve las unidades por la grilla, aplica la última planificación, lanza la siguiente en
 * un hilo aparte y evalúa la condición de parada. Todo el estado vivo se protege con el candado del
 * propio motor; el algoritmo trabaja sobre una copia inmutable.
 * El tiempo se mide en horas desde el día 01 a las 00:00 de la corrida.
 */
public class SimulationEngine {

    private static final Logger log = LoggerFactory.getLogger(SimulationEngine.class);
    private static final double HORAS_ENTREGA = 1.0;
    private static final double HORAS_REMOLQUE = 4.0;
    private static final double HORIZONTE_BLOQUEOS_PLAN = 12.0;
    private static final int MAX_ENTREGADOS_EN_SNAPSHOT = 80;

    public static final String EN_CURSO = "EN_CURSO";
    public static final String FINALIZADO = "FINALIZADO";
    public static final String COLAPSADO = "COLAPSADO";
    public static final String DETENIDA = "DETENIDA";

    final ScenarioConfig config;
    final PlanningAlgorithm algoritmo;
    private final ExecutorService planificador;
    final LocalDateTime fechaBase;
    final double inicioHoras;
    double ahora;

    final List<AlmacenSim> almacenes = new ArrayList<>();
    final List<VehiculoSim> vehiculos = new ArrayList<>();
    final Map<String, PedidoSim> pedidos = new LinkedHashMap<>();
    final ArrayDeque<PedidoSim> entregadosRecientes = new ArrayDeque<>();
    private final List<Order> pedidosFuturos;
    private int siguienteFuturo;
    final List<BloqueoSim> bloqueos = new ArrayList<>();
    List<BloqueoSim> bloqueosVigentes = List.of();
    private String firmaBloqueos = "";
    private final GridPathfinder rutas = new GridPathfinder();
    private final int capacidadMaxima;

    private Future<PlanResultado> planEnCurso;
    private List<String> vehiculosDelPlan = List.of();
    private double ultimaPlanificacion = Double.NEGATIVE_INFINITY;
    private boolean replanificarYa;
    PlanResultado ultimoPlan;
    int planificaciones;

    double costoTotal;
    double distanciaTotal;
    int entregados;
    int entregadosTarde;
    private int contadorPedidos;
    private int contadorBloqueos;

    String estado = EN_CURSO;
    String causa;

    public SimulationEngine(ScenarioConfig config, PlanningAlgorithm algoritmo, ExecutorService planificador,
                            LocalDateTime fechaBase, double inicioHoras, Map<String, Integer> flota,
                            List<Order> pedidosArchivo, List<BloqueoSim> bloqueosIniciales) {
        this.config = config;
        this.algoritmo = algoritmo;
        this.planificador = planificador;
        this.fechaBase = fechaBase;
        this.inicioHoras = inicioHoras;
        this.ahora = inicioHoras;

        for (ScenarioDefaults.AlmacenDef def : ScenarioDefaults.ALMACENES) almacenes.add(new AlmacenSim(def));
        crearFlota(flota == null || flota.isEmpty() ? ScenarioDefaults.FLOTA : flota);
        this.capacidadMaxima = vehiculos.stream().mapToInt(v -> v.tipo.getCapacity()).max().orElse(VehicleType.AUTO.getCapacity());

        List<Order> futuros = new ArrayList<>(pedidosArchivo == null ? List.of() : pedidosArchivo);
        futuros.sort(Comparator.comparingDouble(Order::getPlacementTimeHours));
        this.pedidosFuturos = futuros;
        if (bloqueosIniciales != null) bloqueos.addAll(bloqueosIniciales);
        actualizarBloqueos();
    }

    /** Unidades repartidas entre los tres almacenes (TA = auto, TM = moto, TB = bicicleta). */
    private void crearFlota(Map<String, Integer> flota) {
        String[][] tipos = {{"auto", "TA"}, {"moto", "TM"}, {"bicicleta", "TB"}};
        for (String[] t : tipos) {
            int cantidad = Math.max(0, flota.getOrDefault(t[0], 0));
            VehicleType tipo = ScenarioDefaults.TIPOS_FLOTA.get(t[0]);
            for (int i = 0; i < cantidad; i++) {
                AlmacenSim base = almacenes.get(i % almacenes.size());
                vehiculos.add(new VehiculoSim(String.format("%s%02d", t[1], i + 1), tipo, base));
            }
        }
    }

    // =====================================================================================
    // Ciclo del motor
    // =====================================================================================

    public synchronized void tick(double dtHoras) {
        if (!EN_CURSO.equals(estado)) return;
        double t0 = ahora;
        double t1 = ahora + Math.max(0, dtHoras);
        if (config.es5D()) t1 = Math.min(t1, inicioHoras + ScenarioDefaults.HORIZONTE_5D_HORAS);
        if (Math.floor(t0 / 24) < Math.floor(t1 / 24)) almacenes.forEach(AlmacenSim::recargar);
        ahora = t1;

        ingresarPedidos();
        actualizarBloqueos();
        for (VehiculoSim v : vehiculos) avanzar(v, t0, t1);
        aplicarPlanSiEstaListo();
        lanzarPlanificacionSiCorresponde();
        actualizarEtas();
        evaluarParada();
    }

    private void ingresarPedidos() {
        double limite5D = inicioHoras + ScenarioDefaults.HORIZONTE_5D_HORAS;
        while (siguienteFuturo < pedidosFuturos.size()) {
            Order o = pedidosFuturos.get(siguienteFuturo);
            if (o.getPlacementTimeHours() > ahora) break;
            if (config.es5D() && o.getPlacementTimeHours() >= limite5D) break;
            siguienteFuturo++;
            agregarPedido(o, o.getDestination().getId());
        }
    }

    /** Un pedido mayor que la unidad más grande se fracciona en partes que sí caben (entrega fraccionada). */
    private List<PedidoSim> agregarPedido(Order o, String clienteId) {
        List<PedidoSim> partes = new ArrayList<>();
        if (o.getQuantity() <= capacidadMaxima) {
            partes.add(new PedidoSim(o, clienteId));
        } else {
            int restante = o.getQuantity(), k = 1;
            while (restante > 0) {
                int cantidad = Math.min(capacidadMaxima, restante);
                Order parte = new Order(o.getId() + "-" + k++, o.getDestination(), cantidad,
                        o.getPlacementTimeHours(), o.getMaxDeliveryWindowHours());
                partes.add(new PedidoSim(parte, clienteId));
                restante -= cantidad;
            }
        }
        for (PedidoSim p : partes) pedidos.put(p.id, p);
        return partes;
    }

    private void actualizarBloqueos() {
        List<BloqueoSim> vigentes = new ArrayList<>();
        StringBuilder firma = new StringBuilder();
        for (BloqueoSim b : bloqueos) {
            if (b.vigente(ahora)) {
                vigentes.add(b);
                firma.append(b.id).append(';');
            }
        }
        if (firma.toString().equals(firmaBloqueos)) return;
        firmaBloqueos = firma.toString();
        bloqueosVigentes = List.copyOf(vigentes);
        List<int[]> nodos = new ArrayList<>();
        for (BloqueoSim b : vigentes) nodos.addAll(b.nodos);
        rutas.setBloqueados(nodos);
        // Re-ruteo en caliente: las unidades en movimiento rodean los tramos que se cerraron.
        for (VehiculoSim v : vehiculos) {
            if (v.estado == VehiculoSim.Estado.EN_RUTA || v.estado == VehiculoSim.Estado.REGRESANDO) {
                int[] destino = destinoTramo(v);
                if (destino != null) trazarCamino(v, destino[0], destino[1]);
            }
        }
        replanificarYa = true;
    }

    // =====================================================================================
    // Movimiento de unidades
    // =====================================================================================

    private void avanzar(VehiculoSim v, double t0, double t1) {
        double t = t0;
        int guarda = 0;
        while (t < t1 - 1e-9 && guarda++ < 1000) {
            switch (v.estado) {
                case LIBRE:
                    return;
                case AVERIADO:
                    if (v.remolqueEn >= 0 && v.remolqueEn <= t1) remolcarAlCentral(v);
                    if (v.averiaHasta <= t1) {
                        t = Math.max(t, v.averiaHasta);
                        terminarAveria(v, t);
                        continue;
                    }
                    return;
                case ATENDIENDO:
                    if (v.atendiendoHasta <= t1) {
                        t = Math.max(t, v.atendiendoHasta);
                        completarEntrega(v, t);
                        continue;
                    }
                    return;
                case EN_RUTA:
                case REGRESANDO:
                    double disponibles = (t1 - t) * v.tipo.getSpeedKmH();
                    double recorridos = mover(v, disponibles);
                    v.kmRecorridos += recorridos;
                    distanciaTotal += recorridos;
                    costoTotal += recorridos * v.tipo.getCostPerKm();
                    t += recorridos / v.tipo.getSpeedKmH();
                    if (v.camino.isEmpty()) {
                        llegar(v, t);
                        continue;
                    }
                    return;
            }
        }
    }

    private double mover(VehiculoSim v, double kmDisponibles) {
        double usados = 0;
        while (!v.camino.isEmpty() && usados < kmDisponibles - 1e-12) {
            int[] siguiente = v.camino.peekFirst();
            double dx = siguiente[0] - v.x, dy = siguiente[1] - v.y;
            double d = Math.abs(dx) + Math.abs(dy);
            double paso = Math.min(d, kmDisponibles - usados);
            if (d > 0) {
                // Primero el eje X y luego el Y: el desplazamiento siempre es ortogonal.
                double px = Math.min(Math.abs(dx), paso);
                v.x += Math.signum(dx) * px;
                v.y += Math.signum(dy) * (paso - px);
            }
            usados += paso;
            if (paso >= d - 1e-9) {
                v.x = siguiente[0];
                v.y = siguiente[1];
                v.camino.pollFirst();
            }
        }
        return usados;
    }

    private void llegar(VehiculoSim v, double t) {
        if (v.estado == VehiculoSim.Estado.EN_RUTA) {
            PedidoSim p = v.paradaActual();
            if (p == null) {
                iniciarRegreso(v, almacenMasCercano(v.x, v.y));
                return;
            }
            p.llegadaHoras = t;
            p.etaHoras = t;
            v.estado = VehiculoSim.Estado.ATENDIENDO;
            v.atendiendoHasta = t + HORAS_ENTREGA;
        } else if (v.estado == VehiculoSim.Estado.REGRESANDO) {
            AlmacenSim almacen = v.destinoAlmacen != null ? v.destinoAlmacen : almacenMasCercano(v.x, v.y);
            v.almacen = almacen;
            v.destinoAlmacen = null;
            v.carga = 0;
            v.paradas = new ArrayList<>();
            v.indiceParada = 0;
            if (!v.planPendiente.isEmpty()) {
                List<PedidoSim> plan = new ArrayList<>(v.planPendiente);
                v.planPendiente.clear();
                despachar(v, plan, t);
            } else {
                v.estado = VehiculoSim.Estado.LIBRE;
            }
        }
    }

    private void completarEntrega(VehiculoSim v, double t) {
        PedidoSim p = v.paradaActual();
        if (p != null) {
            p.estado = PedidoSim.Estado.ENTREGADO;
            v.carga = Math.max(0, v.carga - p.cantidad);
            entregados++;
            if (p.atrasado(t)) entregadosTarde++;
            entregadosRecientes.addFirst(p);
            while (entregadosRecientes.size() > MAX_ENTREGADOS_EN_SNAPSHOT) entregadosRecientes.removeLast();
        }
        v.indiceParada++;
        PedidoSim siguiente = v.paradaActual();
        if (siguiente != null) {
            v.estado = VehiculoSim.Estado.EN_RUTA;
            trazarCamino(v, siguiente.x, siguiente.y);
        } else {
            iniciarRegreso(v, almacenMasCercano(v.x, v.y));
        }
    }

    private void iniciarRegreso(VehiculoSim v, AlmacenSim destino) {
        v.estado = VehiculoSim.Estado.REGRESANDO;
        v.destinoAlmacen = destino;
        trazarCamino(v, destino.x, destino.y);
    }

    /** Sale del almacén donde está con los pedidos indicados; si no hay stock, va a cargar al central. */
    private void despachar(VehiculoSim v, List<PedidoSim> plan, double t) {
        int carga = plan.stream().mapToInt(p -> p.cantidad).sum();
        if (!v.almacen.retirar(carga)) {
            AlmacenSim central = central();
            for (PedidoSim p : plan) {
                p.estado = PedidoSim.Estado.PLANIFICADO;
                p.vehiculoId = v.id;
            }
            v.planPendiente.addAll(plan);
            iniciarRegreso(v, central);
            return;
        }
        v.paradas = new ArrayList<>(plan);
        v.indiceParada = 0;
        v.carga = carga;
        for (PedidoSim p : plan) {
            p.estado = PedidoSim.Estado.EN_RUTA;
            p.vehiculoId = v.id;
        }
        v.estado = VehiculoSim.Estado.EN_RUTA;
        PedidoSim primero = plan.get(0);
        trazarCamino(v, primero.x, primero.y);
    }

    private void trazarCamino(VehiculoSim v, int x, int y) {
        int[] desde = v.camino.isEmpty() ? new int[]{v.nodoX(), v.nodoY()} : v.camino.peekFirst();
        List<int[]> nodos = rutas.camino(desde[0], desde[1], x, y);
        v.camino.clear();
        v.camino.addAll(nodos);
    }

    private int[] destinoTramo(VehiculoSim v) {
        if (v.estado == VehiculoSim.Estado.EN_RUTA) {
            PedidoSim p = v.paradaActual();
            return p == null ? null : new int[]{p.x, p.y};
        }
        if (v.estado == VehiculoSim.Estado.REGRESANDO && v.destinoAlmacen != null) {
            return new int[]{v.destinoAlmacen.x, v.destinoAlmacen.y};
        }
        return null;
    }

    AlmacenSim almacenMasCercano(double x, double y) {
        return almacenes.stream().min(Comparator.comparingInt(a -> a.distanciaManhattan(x, y))).orElseThrow();
    }

    private AlmacenSim central() {
        return almacenes.stream().filter(a -> a.central).findFirst().orElse(almacenes.get(0));
    }

    // =====================================================================================
    // Averías (tipo 1: 2 h; tipo 2: hasta el fin del turno siguiente; tipo 3: 48 h y vuelve en el turno 15:00)
    // =====================================================================================

    public synchronized Map<String, Object> registrarAveria(String vehiculoId, int tipo) {
        if (!EN_CURSO.equals(estado)) throw new IllegalStateException("La corrida no está en curso.");
        if (tipo < 1 || tipo > 3) throw new IllegalArgumentException("El tipo de avería debe ser 1, 2 o 3.");
        VehiculoSim v = vehiculos.stream().filter(u -> u.id.equalsIgnoreCase(vehiculoId)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("No existe el vehículo " + vehiculoId + "."));
        if (v.estado == VehiculoSim.Estado.AVERIADO) {
            throw new IllegalStateException("El vehículo " + v.id + " ya tiene una avería activa.");
        }

        int reasignados = 0;
        if (v.estado == VehiculoSim.Estado.ATENDIENDO) {
            // La entrega en curso ya llegó al cliente: se completa; el resto se reasigna.
            completarEntregaPorAveria(v);
        }
        for (int i = v.indiceParada; i < v.paradas.size(); i++) {
            PedidoSim p = v.paradas.get(i);
            if (p.estado != PedidoSim.Estado.ENTREGADO) {
                p.liberar();
                reasignados++;
            }
        }
        for (PedidoSim p : v.planPendiente) {
            p.liberar();
            reasignados++;
        }
        v.planPendiente.clear();
        v.paradas = new ArrayList<>();
        v.indiceParada = 0;
        v.camino.clear();
        v.carga = 0;
        v.destinoAlmacen = null;

        v.estado = VehiculoSim.Estado.AVERIADO;
        v.averiaTipo = tipo;
        v.averiaDesde = ahora;
        v.averiaHasta = switch (tipo) {
            case 1 -> ahora + 2;
            case 2 -> finDelTurnoSiguiente(ahora);
            default -> inicioTurnoTardeTras(ahora + 48);
        };
        v.remolqueEn = tipo == 1 ? -1 : ahora + HORAS_REMOLQUE;
        replanificarYa = true;

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("vehiculoId", v.id);
        r.put("tipo", tipo);
        r.put("desde", SimTime.formato(v.averiaDesde));
        r.put("hasta", SimTime.formato(v.averiaHasta));
        r.put("hastaIso", SimTime.iso(fechaBase, v.averiaHasta));
        r.put("pedidosReasignados", reasignados);
        r.put("remolqueAlCentral", tipo != 1);
        return r;
    }

    private void completarEntregaPorAveria(VehiculoSim v) {
        PedidoSim p = v.paradaActual();
        if (p == null) return;
        p.estado = PedidoSim.Estado.ENTREGADO;
        entregados++;
        if (p.atrasado(p.llegadaHoras)) entregadosTarde++;
        entregadosRecientes.addFirst(p);
        v.indiceParada++;
    }

    private void remolcarAlCentral(VehiculoSim v) {
        AlmacenSim central = central();
        v.x = central.x;
        v.y = central.y;
        v.almacen = central;
        v.remolqueEn = -1;
    }

    private void terminarAveria(VehiculoSim v, double t) {
        v.averiaTipo = 0;
        v.remolqueEn = -1;
        AlmacenSim aqui = almacenes.stream().filter(a -> a.x == v.x && a.y == v.y).findFirst().orElse(null);
        if (aqui != null) {
            v.almacen = aqui;
            v.estado = VehiculoSim.Estado.LIBRE;
        } else {
            iniciarRegreso(v, almacenMasCercano(v.x, v.y));
        }
        replanificarYa = true;
    }

    /** Turnos de 8 h que cambian a las 07:00, 15:00 y 23:00. */
    static double finDelTurnoSiguiente(double t) {
        double dia = Math.floor(t / 24) * 24;
        double h = t - dia;
        double finActual = h < 7 ? dia + 7 : h < 15 ? dia + 15 : h < 23 ? dia + 23 : dia + 31;
        return finActual + 8;
    }

    static double inicioTurnoTardeTras(double t) {
        double candidato = Math.floor(t / 24) * 24 + 15;
        return candidato < t ? candidato + 24 : candidato;
    }

    // =====================================================================================
    // Planificación por lotes (el algoritmo corre en otro hilo sobre una copia inmutable)
    // =====================================================================================

    private void lanzarPlanificacionSiCorresponde() {
        if (planEnCurso != null) return;
        boolean hayPendientes = pedidos.values().stream().anyMatch(p -> p.estado == PedidoSim.Estado.PENDIENTE);
        if (!hayPendientes) return;
        if (!replanificarYa && (ahora - ultimaPlanificacion) * 60 < config.intervaloPlanMin()) return;

        List<VehiculoDisponible> disponibles = new ArrayList<>();
        for (VehiculoSim v : vehiculos) {
            if (v.estado == VehiculoSim.Estado.LIBRE) {
                disponibles.add(new VehiculoDisponible(v.id, v.tipo, v.almacen.id, v.almacen.x, v.almacen.y, ahora));
            } else if (v.estado == VehiculoSim.Estado.REGRESANDO && v.destinoAlmacen != null) {
                double llegada = ahora + v.kmRestantesTramo() / v.tipo.getSpeedKmH();
                AlmacenSim a = v.destinoAlmacen;
                disponibles.add(new VehiculoDisponible(v.id, v.tipo, a.id, a.x, a.y, llegada));
            }
        }
        if (disponibles.isEmpty()) return;

        // Se replanifica lo pendiente y lo planificado que aún no salió (lo que va en ruta no se toca).
        List<PedidoSim> candidatos = pedidos.values().stream()
                .filter(p -> p.estado == PedidoSim.Estado.PENDIENTE || p.estado == PedidoSim.Estado.PLANIFICADO)
                .sorted(Comparator.comparingDouble(p -> p.deadlineHoras))
                .limit(config.maxPedidosPorPlan())
                .toList();
        List<Roadblock> bloqueosPlan = bloqueos.stream()
                .filter(b -> b.finHoras() > ahora && b.inicioHoras() < ahora + HORIZONTE_BLOQUEOS_PLAN)
                .map(b -> b.roadblock).toList();

        InstanciaPlanificacion instancia = new InstanciaPlanificacion(ahora, disponibles,
                candidatos.stream().map(p -> p.order).toList(), bloqueosPlan);
        vehiculosDelPlan = disponibles.stream().map(VehiculoDisponible::id).toList();
        ultimaPlanificacion = ahora;
        replanificarYa = false;
        long limite = config.limitePlanMs();
        planEnCurso = planificador.submit(() -> algoritmo.planificar(instancia, limite));
    }

    private void aplicarPlanSiEstaListo() {
        if (planEnCurso == null || !planEnCurso.isDone()) return;
        PlanResultado resultado;
        try {
            resultado = planEnCurso.get();
        } catch (Exception e) {
            log.warn("La planificación falló: {}", e.getMessage(), e);
            planEnCurso = null;
            return;
        }
        planEnCurso = null;
        ultimoPlan = resultado;
        planificaciones++;

        for (String vehiculoId : vehiculosDelPlan) {
            VehiculoSim v = buscarVehiculo(vehiculoId);
            if (v == null || (v.estado != VehiculoSim.Estado.LIBRE && v.estado != VehiculoSim.Estado.REGRESANDO)) continue;

            List<PedidoSim> nuevos = new ArrayList<>();
            int carga = 0;
            for (Order o : resultado.rutas().getOrDefault(vehiculoId, List.of())) {
                PedidoSim p = pedidos.get(o.getId());
                if (p == null || (p.estado != PedidoSim.Estado.PENDIENTE && p.estado != PedidoSim.Estado.PLANIFICADO)) continue;
                // La capacidad se valida con el tipo real de la unidad (ATS puede proponer otro tipo).
                if (carga + p.cantidad > v.tipo.getCapacity()) continue;
                if (p.estado == PedidoSim.Estado.PLANIFICADO && p.vehiculoId != null && !p.vehiculoId.equals(vehiculoId)) {
                    VehiculoSim anterior = buscarVehiculo(p.vehiculoId);
                    if (anterior != null) anterior.planPendiente.remove(p);
                }
                carga += p.cantidad;
                nuevos.add(p);
            }
            for (PedidoSim p : v.planPendiente) {
                if (!nuevos.contains(p) && vehiculoId.equals(p.vehiculoId) && p.estado == PedidoSim.Estado.PLANIFICADO) p.liberar();
            }
            v.planPendiente.clear();
            if (nuevos.isEmpty()) continue;

            if (v.estado == VehiculoSim.Estado.LIBRE) {
                despachar(v, nuevos, ahora);
            } else {
                for (PedidoSim p : nuevos) {
                    p.estado = PedidoSim.Estado.PLANIFICADO;
                    p.vehiculoId = vehiculoId;
                }
                v.planPendiente.addAll(nuevos);
            }
        }
    }

    // =====================================================================================
    // ETA, semáforo y condición de parada
    // =====================================================================================

    private void actualizarEtas() {
        for (VehiculoSim v : vehiculos) {
            double velocidad = v.tipo.getSpeedKmH();
            switch (v.estado) {
                case EN_RUTA -> {
                    double t = ahora + v.kmRestantesTramo() / velocidad;
                    estimarParadas(v.paradas, v.indiceParada, t, null, velocidad);
                }
                case ATENDIENDO -> {
                    PedidoSim actual = v.paradaActual();
                    if (actual != null) {
                        estimarParadas(v.paradas, v.indiceParada + 1, v.atendiendoHasta, new int[]{actual.x, actual.y}, velocidad);
                    }
                }
                case REGRESANDO -> {
                    if (!v.planPendiente.isEmpty() && v.destinoAlmacen != null) {
                        double t = ahora + v.kmRestantesTramo() / velocidad;
                        estimarParadas(v.planPendiente, 0, t, new int[]{v.destinoAlmacen.x, v.destinoAlmacen.y}, velocidad);
                    }
                }
                default -> {
                }
            }
        }
    }

    /**
     * Si {@code desde} es null, {@code t} ya es la llegada a la primera parada; si no, {@code t} es la
     * hora de salida desde ese punto.
     */
    private void estimarParadas(List<PedidoSim> paradas, int inicio, double t, int[] desde, double velocidad) {
        int[] previo = desde;
        for (int i = inicio; i < paradas.size(); i++) {
            PedidoSim p = paradas.get(i);
            if (previo != null) t += rutas.distancia(previo[0], previo[1], p.x, p.y) / velocidad;
            p.etaHoras = t;
            t += HORAS_ENTREGA;
            previo = new int[]{p.x, p.y};
        }
    }

    String semaforo(PedidoSim p) {
        double ventana = Math.max(1e-6, p.deadlineHoras - p.registroHoras);
        double referencia = p.llegadaHoras >= 0 ? p.llegadaHoras : p.etaHoras != null ? Math.max(ahora, p.etaHoras) : ahora;
        double holgura = (p.deadlineHoras - referencia) / ventana * 100.0;
        if (holgura >= config.verdeMinPct()) return "VERDE";
        if (holgura >= config.ambarMinPct()) return "AMBAR";
        return "ROJO";
    }

    int atrasados() {
        int pendientesVencidos = 0;
        for (PedidoSim p : pedidos.values()) {
            if (p.estado != PedidoSim.Estado.ENTREGADO && p.atrasado(ahora)) pendientesVencidos++;
        }
        return entregadosTarde + pendientesVencidos;
    }

    private void evaluarParada() {
        if (config.es5D() && ahora >= inicioHoras + ScenarioDefaults.HORIZONTE_5D_HORAS - 1e-9) {
            finalizar(FINALIZADO, "Se completaron los 5 días simulados.");
            return;
        }
        if (config.esColapso()) {
            for (PedidoSim p : pedidos.values()) {
                if (p.atrasado(ahora)) {
                    finalizar(COLAPSADO, String.format("Colapso logístico: el pedido %s (cliente %s, %d paq.) incumplió su plazo de %.0f h; vencía %s.",
                            p.id, p.clienteId, p.cantidad, p.deadlineHoras - p.registroHoras, SimTime.formato(p.deadlineHoras)));
                    return;
                }
            }
            boolean todoAtendido = siguienteFuturo >= pedidosFuturos.size()
                    && pedidos.values().stream().allMatch(p -> p.estado == PedidoSim.Estado.ENTREGADO);
            if (todoAtendido) finalizar(FINALIZADO, "Sin colapso: se entregaron a tiempo todos los pedidos del archivo.");
        }
    }

    private void finalizar(String estadoFinal, String motivo) {
        estado = estadoFinal;
        causa = motivo;
        if (planEnCurso != null) planEnCurso.cancel(true);
        planEnCurso = null;
    }

    public synchronized void detener(String motivo) {
        if (EN_CURSO.equals(estado)) finalizar(DETENIDA, motivo);
    }

    // =====================================================================================
    // Operación diaria: registro de pedidos y bloqueos en vivo
    // =====================================================================================

    public synchronized Map<String, Object> registrarPedido(String clienteId, int x, int y, int cantidad, double horizonteHoras) {
        if (!EN_CURSO.equals(estado)) throw new IllegalStateException("La corrida no está en curso.");
        String id = String.format("ORD-%05d", ++contadorPedidos);
        Location destino = new Location(clienteId, clienteId, x, y, Location.LocationType.CUSTOMER);
        Order order = new Order(id, destino, cantidad, ahora, horizonteHoras);
        List<PedidoSim> partes = agregarPedido(order, clienteId);
        replanificarYa = true;

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", id);
        r.put("clienteId", clienteId);
        r.put("x", x);
        r.put("y", y);
        r.put("cantidad", cantidad);
        r.put("registradoEn", SimTime.iso(fechaBase, ahora));
        r.put("deadline", SimTime.iso(fechaBase, ahora + horizonteHoras));
        r.put("estado", "PENDIENTE");
        r.put("partes", partes.size());
        return r;
    }

    public synchronized int agregarBloqueos(List<BloqueoSim> nuevos) {
        bloqueos.addAll(nuevos);
        actualizarBloqueos();
        return nuevos.size();
    }

    public synchronized String siguienteIdBloqueo() {
        return String.format("BLQ-%04d", ++contadorBloqueos);
    }

    public synchronized Map<String, Object> buscarPedido(String id) {
        PedidoSim p = pedidos.get(id);
        if (p == null) return null;
        return SnapshotMapper.pedido(this, p);
    }

    // =====================================================================================
    // Consultas (el SnapshotMapper las usa bajo el candado del motor)
    // =====================================================================================

    public synchronized String getEstado() {
        return estado;
    }

    public synchronized boolean enCurso() {
        return EN_CURSO.equals(estado);
    }

    public synchronized double getAhora() {
        return ahora;
    }

    boolean planificando() {
        return planEnCurso != null;
    }

    VehiculoSim buscarVehiculo(String id) {
        for (VehiculoSim v : vehiculos) if (v.id.equals(id)) return v;
        return null;
    }

    List<int[]> caminoEntre(int x0, int y0, int x1, int y1) {
        return rutas.camino(x0, y0, x1, y1);
    }

    int pedidosPorIngresar() {
        return pedidosFuturos.size() - siguienteFuturo;
    }
}
