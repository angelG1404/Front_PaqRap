package com.paqrap;

import com.paqrap.model.*;
import com.paqrap.simulator.ScenarioGenerator;
import com.paqrap.solver.ATSOptimizer;
import com.paqrap.util.ReportGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Verification test suite for Adaptive Tabu Search (ATS) prototype.
 */
public class ATSTest {

    public static void main(String[] args) {
        System.out.println("Running ATS Verification Tests...");
        testATSIntegracionMantenimientoYBloqueo();
        System.out.println("ALL TESTS PASSED SUCCESSFULLY!");
    }
    
    public static void testATSIntegracionMantenimientoYBloqueo() {
        System.out.print("Testing ATS Integration (Maintenance + Roadblock)... ");
        
        // 1. Setup: Crear restricciones combinadas
        List<Maintenance> m = new ArrayList<>();
        List<Breakdown> b = new ArrayList<>();
        List<Roadblock> r = new ArrayList<>();
        
        m.add(new Maintenance("AUTO-1", 0.0, 48.0)); 
        
        // Hay un bloqueo vial 1 que obliga a rodear
        List<Location> path1 = new ArrayList<>();
        path1.add(new Location("B1", "B1", 40, 25, Location.LocationType.CUSTOMER));
        path1.add(new Location("B2", "B2", 40, 30, Location.LocationType.CUSTOMER));
        r.add(new Roadblock(path1, 0.0, 10.0));
        
        // Bloqueo vial 2 (simultáneo)
        List<Location> path2 = new ArrayList<>();
        path2.add(new Location("B3", "B3", 11, 14, Location.LocationType.CUSTOMER));
        path2.add(new Location("B4", "B4", 11, 25, Location.LocationType.CUSTOMER));
        path2.add(new Location("B5", "B5", 5, 25, Location.LocationType.CUSTOMER));
        r.add(new Roadblock(path2, 0.0, 10.0));

        List<Order> orders = new ArrayList<>();
        orders.add(new Order("ORD-1", new Location("C1", "C1", 5, 20, Location.LocationType.CUSTOMER), 1, 0.0, 24.0));
        orders.add(new Order("ORD-2", new Location("C2", "C2", 15, 35, Location.LocationType.CUSTOMER), 1, 0.0, 24.0));
        orders.add(new Order("ORD-3", new Location("C3", "C3", 50, 10, Location.LocationType.CUSTOMER), 1, 0.0, 24.0));

        // 3. Generar escenario custom
        ScenarioGenerator.Scenario scenario = ScenarioGenerator.generateCustomScenario(orders, r, m, new ArrayList<>());
        scenario.printMap(0.0);
        
        // 3. Ejecutar optimizador
        ATSOptimizer optimizer = new ATSOptimizer();
        com.paqrap.solver.InitialSolutionBuilder builder = new com.paqrap.solver.InitialSolutionBuilder();
        //3.1 Construir solución inicial
        Solution initialSolution = builder.buildInitialSolution(scenario.orders, scenario.fleet, scenario.warehouses, 0.0);
        
        // --- INICIO DE CODIGO DE DEPURACION ---
        System.out.println("--- INSPECCION DE SOLUCION INICIAL ---");
        System.out.println(initialSolution.toString());
        for(Route re : initialSolution.getRoutes()) {
            System.out.printf("Ruta %s: Capacidad %d/%d | Entregas: %d%n", 
                re.getVehicle().getId(), re.getTotalDemand(), re.getVehicle().getCapacity(), re.getOrders().size());
        }
        // --- FIN DE CODIGO DE DEPURACION ---

        //3.2 Ejecutar optimización
        Solution optimizedSolution = optimizer.solve(initialSolution, m, b, r);
        
        // --- VISUALIZACION FINAL ---
        ReportGenerator.printFinalMap(scenario, optimizedSolution);
        
        // 4. Verificaciones integradas
        // (a) AUTO-1 no debe tener órdenes (validación de Mantenimiento)
        for (Route route : optimizedSolution.getRoutes()) {
            if (route.getVehicle().getId().equals("AUTO-1")) {
                assert route.getOrders().isEmpty() : "Vehículo en mantenimiento debe tener 0 órdenes";
            }
        }
        
        // (b) El sistema debe ser capaz de encontrar rutas alternativas (validación de A* sobre bloqueos)
        assert optimizedSolution.getUnassignedOrders().isEmpty() : "El sistema debe encontrar rutas alternativas rodeando bloqueos";
        
        System.out.println("OK");
    }
}