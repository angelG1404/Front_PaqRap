package com.paqrap.simulator;

import com.paqrap.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates operational scenarios based on the PaqRap problem statement.
 */
public class ScenarioGenerator {

    public static class Scenario {
        public List<Warehouse> warehouses;
        public List<Vehicle> fleet;
        public List<Order> orders;
        //CAMBIO ITERACION 01
        public List<Maintenance> maintenances = new ArrayList<>();
        public List<Breakdown> breakdowns = new ArrayList<>();
        public List<Roadblock> roadblocks = new ArrayList<>();
        public int gridRows = 71;
        public int gridCols = 51;

        public Scenario(List<Warehouse> warehouses, List<Vehicle> fleet, List<Order> orders) {
            this(warehouses, fleet, orders, 71, 51);
        }

        public Scenario(List<Warehouse> warehouses, List<Vehicle> fleet, List<Order> orders, int gridRows, int gridCols) {
            this.warehouses = warehouses;
            this.fleet = fleet;
            this.orders = orders;
            this.gridRows = gridRows;
            this.gridCols = gridCols;
        }

        public void printMap(double currentTime) {
            char[][] map = new char[gridRows][gridCols];
            // Inicializar con '*'
            for (int x = 0; x < gridRows; x++) {
                for (int y = 0; y < gridCols; y++) {
                    map[x][y] = '*';
                }
            }

            // Marcar Almacenes ('A')
            for (Warehouse w : warehouses) {
                int wx = (int) Math.round(w.getLocation().getX());
                int wy = (int) Math.round(w.getLocation().getY());
                if (wx >= 0 && wx < gridRows && wy >= 0 && wy < gridCols) {
                    map[wx][wy] = 'A';
                }
            }

            // Marcar Pedidos ('P')
            for (Order o : orders) {
                int px = (int) Math.round(o.getDestination().getX());
                int py = (int) Math.round(o.getDestination().getY());
                if (px >= 0 && px < gridRows && py >= 0 && py < gridCols) {
                    // Solo marcar si no es un almacén o bloqueo
                    if (map[px][py] == '*') {
                        map[px][py] = 'P';
                    }
                }
            }

            // Marcar Bloqueos activos ('B')
            for (Roadblock rb : roadblocks) {
                if (currentTime >= rb.startTimeHours && currentTime <= rb.endTimeHours) {
                    for (Location loc : rb.path) {
                        int rx = (int) Math.round(loc.getX());
                        int ry = (int) Math.round(loc.getY());
                        if (rx >= 0 && rx < gridRows && ry >= 0 && ry < gridCols) {
                            map[rx][ry] = 'B';
                        }
                    }
                }
            }

            // Imprimir (Origen 0,0 en esquina inferior izquierda)
            System.out.println("Mapa (" + gridRows + "x" + gridCols + ") en tiempo " + currentTime + ":");
            for (int y = gridCols - 1; y >= 0; y--) {
                for (int x = 0; x < gridRows; x++) {
                    System.out.print(map[x][y] + " ");
                }
                System.out.println();
            }

            // Resumen de restricciones
            System.out.println("\n--- Resumen de Restricciones ---");
            if (!maintenances.isEmpty()) {
                System.out.println("Mantenimientos:");
                for (Maintenance m : maintenances) {
                    System.out.println("- " + m.vehicleId + ": " + m.startTimeHours + " to " + m.endTimeHours);
                }
            }
            if (!roadblocks.isEmpty()) {
                System.out.println("Bloqueos:");
                for (Roadblock rb : roadblocks) {
                    System.out.println("- Activo en tiempo [" + rb.startTimeHours + ", " + rb.endTimeHours + "]");
                }
            }
            System.out.println("--------------------------------\n");
        }
    }

    /**
     * Generates a PaqRap benchmark instance.
     *
     * @param orderCount Number of customer orders to generate
     * @param seed Random seed for reproducibility
     */
    public static Scenario generateScenario(int orderCount, long seed) {
        return generateScenario(orderCount, seed, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Generates a custom PaqRap scenario with explicit orders and constraints.
     */
    public static Scenario generateCustomScenario(List<Order> orders, List<Roadblock> r, List<Maintenance> m, List<Breakdown> b) {
        // Mismos almacenes y flota base
        Location centralLoc = new Location("DEP-CENTRAL", "Almacén Central", 35.0, 25.0, Location.LocationType.CENTRAL_WAREHOUSE);
        Location sub1Loc = new Location("DEP-SUB1", "Almacén Intermedio Norte", 12.0, 38.0, Location.LocationType.INTERMEDIATE_WAREHOUSE);
        Location sub2Loc = new Location("DEP-SUB2", "Almacén Intermedio Sur", 57.0, 27.0, Location.LocationType.INTERMEDIATE_WAREHOUSE);

        List<Warehouse> warehouses = new ArrayList<>();
        warehouses.add(new Warehouse("WH-0", centralLoc, true, Integer.MAX_VALUE));
        warehouses.add(new Warehouse("WH-1", sub1Loc, false, 1000));
        warehouses.add(new Warehouse("WH-2", sub2Loc, false, 1000));

          // 2. Heterogeneous Fleet
        List<Vehicle> fleet = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            fleet.add(new Vehicle(String.format("TA%02d", i), VehicleType.AUTO, centralLoc));
        }
        for (int i = 1; i <= 15; i++) {
            Location startDep = (i % 2 == 0) ? sub1Loc : sub2Loc;
            fleet.add(new Vehicle(String.format("TM%02d", i), VehicleType.MOTO, startDep));
        }
        for (int i = 1; i <= 12; i++) {
            Location startDep = (i % 2 == 0) ? sub1Loc : sub2Loc;
            fleet.add(new Vehicle(String.format("TB%02d", i), VehicleType.BICI, startDep));
        }

        Scenario sc = new Scenario(warehouses, fleet, orders);
        sc.maintenances = m;
        sc.breakdowns = b;
        sc.roadblocks = r;
        return sc;
    }

    /**
     * Generates a PaqRap benchmark instance with optional constraints.
     */
    public static Scenario generateScenario(int orderCount, long seed, 
                                            List<Maintenance> m, List<Breakdown> b, List<Roadblock> r) {
        Random rand = new Random(seed);

        // 1. Locations (30km x 30km operational area)
        Location centralLoc = new Location("DEP-CENTRAL", "Almacén Central", 27.0, 14.0, Location.LocationType.CENTRAL_WAREHOUSE);
        Location sub1Loc = new Location("DEP-SUB1", "Almacén Intermedio Norte", 12.0, 38.0, Location.LocationType.INTERMEDIATE_WAREHOUSE);
        Location sub2Loc = new Location("DEP-SUB2", "Almacén Intermedio Sur", 57.0, 27.0, Location.LocationType.INTERMEDIATE_WAREHOUSE);

        List<Warehouse> warehouses = new ArrayList<>();
        warehouses.add(new Warehouse("WH-0", centralLoc, true, Integer.MAX_VALUE));
        warehouses.add(new Warehouse("WH-1", sub1Loc, false, 1000));
        warehouses.add(new Warehouse("WH-2", sub2Loc, false, 1000));

        // 2. Heterogeneous Fleet
        List<Vehicle> fleet = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            fleet.add(new Vehicle(String.format("TA%02d", i), VehicleType.AUTO, centralLoc));
        }
        for (int i = 1; i <= 15; i++) {
            Location startDep = (i % 2 == 0) ? sub1Loc : sub2Loc;
            fleet.add(new Vehicle(String.format("TM%02d", i), VehicleType.MOTO, startDep));
        }
        for (int i = 1; i <= 12; i++) {
            Location startDep = (i % 2 == 0) ? sub1Loc : sub2Loc;
            fleet.add(new Vehicle(String.format("TB%02d", i), VehicleType.BICI, startDep));
        }

        // 3. Customer Orders with Priority Deadlines
        List<Order> orders = new ArrayList<>();
        double[] possibleDeadlines = {4.0, 8.0, 12.0, 18.0, 36.0};

        for (int i = 1; i <= orderCount; i++) {
            double x = 1.0 + rand.nextDouble() * 28.0;
            double y = 1.0 + rand.nextDouble() * 28.0;
            Location custLoc = new Location("CUST-" + i, "Cliente " + i, x, y, Location.LocationType.CUSTOMER);

            int quantity = 1 + rand.nextInt(4); // 1 to 4 packages of product P
            double window = possibleDeadlines[rand.nextInt(possibleDeadlines.length)];
            double placementTime = rand.nextDouble() * 6.0; // Placed within first 6 hours

            orders.add(new Order("ORD-" + i, custLoc, quantity, placementTime, window));
        }

        Scenario sc = new Scenario(warehouses, fleet, orders);
        sc.maintenances = m;
        sc.breakdowns = b;
        sc.roadblocks = r;
        return sc;
    }
}
