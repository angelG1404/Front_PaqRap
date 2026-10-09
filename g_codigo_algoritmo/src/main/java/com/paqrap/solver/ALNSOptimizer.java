package com.paqrap.solver;

import com.paqrap.model.*;

import java.util.*;

/**
 * Adaptive Large Neighborhood Search (ALNS) Metaheuristic Solver for PaqRap VRP.
 *
 * Implements the framework proposed by Ropke and Pisinger (2006):
 * - Destroy Operators: Random, Worst-Cost, Shaw (Relatedness), Route-Removal, Late-Order Removal.
 * - Repair Operators: Greedy Insertion, Regret-2 Insertion, Urgency (EDF) Insertion, Vehicle-Cost Aware Insertion.
 * - Adaptive Weight Mechanism: Roulette-wheel operator selection with segment-based score updating.
 * - Acceptance Criterion: Simulated Annealing (SA) with geometric temperature cooling.
 */
public class ALNSOptimizer implements RouteOptimizer {

    public static class ALNSConfig {
        public int maxIterations = 250;
        public double coolingRate = 0.985;
        public double startTemperature = -1.0; // Auto-calibrated if <= 0
        public double minTemperature = 0.01;
        public double minDestroyRatio = 0.10;
        public double maxDestroyRatio = 0.35;
        public int segmentSize = 25; // Iterations per weight update segment
        public double reactionFactor = 0.20; // Rho parameter for smoothing weight adaptation
        public double weightMin = 0.10; // Minimum weight to prevent operator starvation
        public double scoreBest = 30.0;    // Sigma 1: New global best solution
        public double scoreBetter = 15.0;  // Sigma 2: Accepted solution better than current
        public double scoreAccepted = 5.0; // Sigma 3: Accepted solution worse than current (SA)
        public double scoreRejected = 0.0; // Sigma 4: Rejected solution
        public boolean verboseLogging = true;
        public long randomSeed = 20260910L;
    }

    public static class OperatorStats {
        public final String name;
        public double weight;
        public int usageCount;
        public double scoreSum;
        public int globalImprovements;
        public int localImprovements;
        public int acceptedCount;

        public OperatorStats(String name, double initialWeight) {
            this.name = name;
            this.weight = initialWeight;
            this.usageCount = 0;
            this.scoreSum = 0.0;
            this.globalImprovements = 0;
            this.localImprovements = 0;
            this.acceptedCount = 0;
        }
    }

    private final ALNSConfig config;
    private final SolutionEvaluator evaluator;
    private final Random rand;
    private final List<String> logs;

    private final List<OperatorStats> destroyStats;
    private final List<OperatorStats> repairStats;

    public ALNSOptimizer() {
        this(new ALNSConfig());
    }

    public ALNSOptimizer(ALNSConfig config) {
        this.config = config;
        this.evaluator = new SolutionEvaluator();
        this.rand = new Random(config.randomSeed);
        this.logs = new ArrayList<>();

        this.destroyStats = new ArrayList<>();
        this.destroyStats.add(new OperatorStats("Random-Destroy", 1.0));
        this.destroyStats.add(new OperatorStats("WorstCost-Destroy", 1.0));
        this.destroyStats.add(new OperatorStats("ShawRelatedness-Destroy", 1.0));
        this.destroyStats.add(new OperatorStats("RouteRemoval-Destroy", 1.0));
        this.destroyStats.add(new OperatorStats("LateOrder-Destroy", 1.0));

        this.repairStats = new ArrayList<>();
        this.repairStats.add(new OperatorStats("Greedy-Repair", 1.0));
        this.repairStats.add(new OperatorStats("Regret2-Repair", 1.0));
        this.repairStats.add(new OperatorStats("UrgencyEDF-Repair", 1.0));
        this.repairStats.add(new OperatorStats("VehicleCostAware-Repair", 1.0));
    }

    @Override
    public String getAlgorithmName() {
        return "ALNS (Adaptive Large Neighborhood Search)";
    }

    @Override
    public List<String> getLogs() {
        return Collections.unmodifiableList(logs);
    }

    public List<OperatorStats> getDestroyStats() {
        return Collections.unmodifiableList(destroyStats);
    }

    public List<OperatorStats> getRepairStats() {
        return Collections.unmodifiableList(repairStats);
    }

    @Override
    public Solution solve(Solution initialSolution) {
        return solve(initialSolution, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public Solution solve(Solution initialSolution, List<Maintenance> maintenances, List<Breakdown> breakdowns, List<Roadblock> roadblocks) {
        Solution currentSolution = initialSolution.cloneSolution();
        for (Route r : currentSolution.getRoutes()) {
            r.setRoadblocks(roadblocks);
        }
        evaluator.evaluate(currentSolution);

        Solution bestSolution = currentSolution.cloneSolution();
        for (Route r : bestSolution.getRoutes()) {
            r.setRoadblocks(roadblocks);
        }

        int totalOrders = countTotalOrders(currentSolution);
        if (totalOrders == 0) {
            log("ALNS: No hay pedidos en la solución inicial. Terminando.");
            return bestSolution;
        }

        double temperature = config.startTemperature;
        if (temperature <= 0) {
            temperature = Math.max(10.0, (currentSolution.getFitness() * 0.05) / Math.log(2.0));
        }

        int totalAccepted = 0;
        int totalGlobalImprovements = 0;
        int weightUpdateCount = 0;

        log("=== INICIO ADAPTIVE LARGE NEIGHBORHOOD SEARCH (ALNS) ===");
        log(String.format("Solución Inicial - Costo: S/%.2f, Distancia: %.2f km, Fitness: %.2f, Sin Asignar: %d",
                currentSolution.getTotalMonetaryCost(), currentSolution.getTotalDistanceKm(),
                currentSolution.getFitness(), currentSolution.getUnassignedOrders().size()));
        log(String.format("Configuración ALNS: Iteraciones=%d, T0=%.2f, CoolingRate=%.4f, Segmento=%d, Destrucción=[%.0f%%..%.0f%%]",
                config.maxIterations, temperature, config.coolingRate, config.segmentSize,
                config.minDestroyRatio * 100.0, config.maxDestroyRatio * 100.0));

        for (int iter = 1; iter <= config.maxIterations; iter++) {
            int minQ = Math.max(1, (int) Math.round(totalOrders * config.minDestroyRatio));
            int maxQ = Math.max(minQ, (int) Math.round(totalOrders * config.maxDestroyRatio));
            maxQ = Math.min(totalOrders, maxQ);
            int q = minQ + (maxQ > minQ ? rand.nextInt(maxQ - minQ + 1) : 0);

            int destroyIndex = selectRouletteOperator(destroyStats);
            int repairIndex = selectRouletteOperator(repairStats);

            OperatorStats dOp = destroyStats.get(destroyIndex);
            OperatorStats rOp = repairStats.get(repairIndex);

            dOp.usageCount++;
            rOp.usageCount++;

            Solution neighbor = currentSolution.cloneSolution();
            for (Route r : neighbor.getRoutes()) {
                r.setRoadblocks(roadblocks);
            }

            List<Order> destroyedPool = applyDestroyOperator(destroyIndex, neighbor, q);
            applyRepairOperator(repairIndex, neighbor, destroyedPool);

            // Filtrado de viabilidad según restricciones de Ángel
            if (!neighbor.isFeasible(maintenances, breakdowns)) {
                continue;
            }

            double candidateFitness = evaluator.evaluate(neighbor);
            double currentFitness = currentSolution.getFitness();
            double bestFitness = bestSolution.getFitness();

            double deltaFitness = candidateFitness - currentFitness;
            double scoreAwarded;
            boolean accepted = false;

            if (candidateFitness < bestFitness - 1e-4) {
                bestSolution = neighbor.cloneSolution();
                currentSolution = neighbor.cloneSolution();
                scoreAwarded = config.scoreBest;
                accepted = true;
                totalGlobalImprovements++;
                totalAccepted++;
                dOp.globalImprovements++;
                rOp.globalImprovements++;
                dOp.acceptedCount++;
                rOp.acceptedCount++;

                if (config.verboseLogging) {
                    log(String.format("Iter %3d: [MEJORA GLOBAL ALNS] Fitness: %.2f | Costo: S/%.2f | Temp: %.2f | Ops: [%s + %s] | q=%d",
                            iter, bestSolution.getFitness(), bestSolution.getTotalMonetaryCost(), temperature,
                            dOp.name, rOp.name, q));
                }
            } else if (candidateFitness < currentFitness - 1e-4) {
                currentSolution = neighbor.cloneSolution();
                scoreAwarded = config.scoreBetter;
                accepted = true;
                totalAccepted++;
                dOp.localImprovements++;
                rOp.localImprovements++;
                dOp.acceptedCount++;
                rOp.acceptedCount++;
            } else {
                double acceptProb = Math.exp(-deltaFitness / Math.max(temperature, 1e-6));
                if (rand.nextDouble() < acceptProb) {
                    currentSolution = neighbor.cloneSolution();
                    scoreAwarded = config.scoreAccepted;
                    accepted = true;
                    totalAccepted++;
                    dOp.acceptedCount++;
                    rOp.acceptedCount++;
                } else {
                    scoreAwarded = config.scoreRejected;
                    accepted = false;
                }
            }

            dOp.scoreSum += scoreAwarded;
            rOp.scoreSum += scoreAwarded;

            temperature = Math.max(config.minTemperature, temperature * config.coolingRate);

            if (iter % config.segmentSize == 0) {
                updateOperatorWeights(destroyStats);
                updateOperatorWeights(repairStats);
                weightUpdateCount++;

                if (config.verboseLogging) {
                    log(String.format("Iter %3d: [ADAPTACIÓN DE PESOS ALNS #%d] Mejor Fitness: %.2f | Temp: %.2f | Aceptaciones: %d/%d",
                            iter, weightUpdateCount, bestSolution.getFitness(), temperature, totalAccepted, iter));
                }
            }
        }

        evaluator.evaluate(bestSolution);

        double initialFitness = initialSolution.getFitness();
        double finalFitness = bestSolution.getFitness();
        double improvementPct = (initialFitness > 0) ? ((initialFitness - finalFitness) / initialFitness) * 100.0 : 0.0;

        log("=== RESUMEN DE OPTIMIZACIÓN ALNS ===");
        log(String.format("Fitness Final: %.2f (Mejora: %.2f%%)", finalFitness, improvementPct));
        log(String.format("Costo Operativo Final: S/%.2f | Distancia Total: %.2f km",
                bestSolution.getTotalMonetaryCost(), bestSolution.getTotalDistanceKm()));
        log(String.format("Total Mejoras Globales: %d | Total Soluciones Aceptadas: %d / %d",
                totalGlobalImprovements, totalAccepted, config.maxIterations));

        log("\n--- Rendimiento de Operadores de Destrucción (Destroy) ---");
        for (OperatorStats s : destroyStats) {
            log(String.format(" %-24s: Usos=%3d | MejorasGlob=%2d | Aceptadas=%3d | PesoFinal=%.3f",
                    s.name, s.usageCount, s.globalImprovements, s.acceptedCount, s.weight));
        }

        log("\n--- Rendimiento de Operadores de Reconstrucción (Repair) ---");
        for (OperatorStats s : repairStats) {
            log(String.format(" %-24s: Usos=%3d | MejorasGlob=%2d | Aceptadas=%3d | PesoFinal=%.3f",
                    s.name, s.usageCount, s.globalImprovements, s.acceptedCount, s.weight));
        }

        return bestSolution;
    }

    // =========================================================================
    // DESTROY OPERATORS
    // =========================================================================

    private List<Order> applyDestroyOperator(int operatorIndex, Solution solution, int q) {
        switch (operatorIndex) {
            case 0:
                return destroyRandom(solution, q);
            case 1:
                return destroyWorstCost(solution, q);
            case 2:
                return destroyShawRelatedness(solution, q);
            case 3:
                return destroyRouteRemoval(solution, q);
            case 4:
                return destroyLateOrders(solution, q);
            default:
                return destroyRandom(solution, q);
        }
    }

    /**
     * Operator 1: Random Removal.
     * Uniformly removes q randomly selected customer orders.
     */
    private List<Order> destroyRandom(Solution solution, int q) {
        List<Order> removed = new ArrayList<>();
        List<OrderRef> allAssigned = getAllAssignedOrders(solution);
        if (allAssigned.isEmpty()) return removed;

        Collections.shuffle(allAssigned, rand);
        int toRemove = Math.min(q, allAssigned.size());

        Set<Order> ordersToRemove = new HashSet<>();
        for (int i = 0; i < toRemove; i++) {
            ordersToRemove.add(allAssigned.get(i).order);
        }

        removeOrdersFromSolution(solution, ordersToRemove, removed);
        return removed;
    }

    /**
     * Operator 2: Worst-Cost Removal.
     * Identifies orders that contribute the most to route cost and travel distance.
     * Uses randomized power-law selection (p=3) to prevent determinism.
     */
    private List<Order> destroyWorstCost(Solution solution, int q) {
        List<Order> removed = new ArrayList<>();
        List<OrderRef> allAssigned = getAllAssignedOrders(solution);
        if (allAssigned.isEmpty()) return removed;

        // Calculate cost saving if each order is removed
        List<OrderCostSaving> savings = new ArrayList<>();
        for (OrderRef ref : allAssigned) {
            double currentCost = ref.route.getCost();
            double saving = calculateRemovalCostSaving(ref.route, ref.index);
            savings.add(new OrderCostSaving(ref.order, ref.route, ref.index, saving));
        }

        // Sort descending by cost savings (highest saving first)
        savings.sort((a, b) -> Double.compare(b.saving, a.saving));

        Set<Order> ordersToRemove = new HashSet<>();
        double p = 3.0; // Randomization parameter
        int remaining = Math.min(q, savings.size());

        while (ordersToRemove.size() < remaining && !savings.isEmpty()) {
            double r = rand.nextDouble();
            int selectedIndex = (int) Math.floor(savings.size() * Math.pow(r, p));
            selectedIndex = Math.min(selectedIndex, savings.size() - 1);

            OrderCostSaving chosen = savings.remove(selectedIndex);
            ordersToRemove.add(chosen.order);
        }

        removeOrdersFromSolution(solution, ordersToRemove, removed);
        return removed;
    }

    /**
     * Operator 3: Shaw (Relatedness) Removal.
     * Removes orders that are closely related in terms of distance, deadline, and package quantity.
     */
    private List<Order> destroyShawRelatedness(Solution solution, int q) {
        List<Order> removed = new ArrayList<>();
        List<OrderRef> allAssigned = getAllAssignedOrders(solution);
        if (allAssigned.isEmpty()) return removed;

        // Pick a random seed order
        OrderRef seedRef = allAssigned.get(rand.nextInt(allAssigned.size()));
        Order seedOrder = seedRef.order;

        // Normalization bounds
        double maxDist = 45.0; // Approx max distance across 30x30 matrix
        double maxDeadlineDiff = 36.0;
        double maxDemandDiff = 4.0;

        double phiDist = 0.50;
        double phiTime = 0.35;
        double phiDemand = 0.15;

        List<OrderRelatedness> relatednessList = new ArrayList<>();
        for (OrderRef ref : allAssigned) {
            if (ref.order.equals(seedOrder)) continue;

            double dist = seedOrder.getDestination().distanceTo(ref.order.getDestination());
            double timeDiff = Math.abs(seedOrder.getDeadlineHours() - ref.order.getDeadlineHours());
            double demandDiff = Math.abs(seedOrder.getQuantity() - ref.order.getQuantity());

            double rScore = phiDist * (dist / maxDist)
                    + phiTime * (timeDiff / maxDeadlineDiff)
                    + phiDemand * (demandDiff / maxDemandDiff);

            relatednessList.add(new OrderRelatedness(ref.order, rScore));
        }

        // Sort ascending by relatedness score (lower score = more related)
        relatednessList.sort(Comparator.comparingDouble(a -> a.score));

        Set<Order> ordersToRemove = new HashSet<>();
        ordersToRemove.add(seedOrder);

        double p = 3.0;
        int target = Math.min(q, allAssigned.size());

        while (ordersToRemove.size() < target && !relatednessList.isEmpty()) {
            double r = rand.nextDouble();
            int index = (int) Math.floor(relatednessList.size() * Math.pow(r, p));
            index = Math.min(index, relatednessList.size() - 1);

            OrderRelatedness chosen = relatednessList.remove(index);
            ordersToRemove.add(chosen.order);
        }

        removeOrdersFromSolution(solution, ordersToRemove, removed);
        return removed;
    }

    /**
     * Operator 4: Route Removal.
     * Selects one or two non-empty routes and removes their orders to encourage vehicle consolidation.
     */
    private List<Order> destroyRouteRemoval(Solution solution, int q) {
        List<Order> removed = new ArrayList<>();
        List<Route> nonEmptyRoutes = new ArrayList<>();
        for (Route r : solution.getRoutes()) {
            if (!r.getOrders().isEmpty()) {
                nonEmptyRoutes.add(r);
            }
        }

        if (nonEmptyRoutes.isEmpty()) return removed;

        Collections.shuffle(nonEmptyRoutes, rand);
        Set<Order> ordersToRemove = new HashSet<>();

        for (Route r : nonEmptyRoutes) {
            for (Order o : r.getOrders()) {
                ordersToRemove.add(o);
                if (ordersToRemove.size() >= q) break;
            }
            if (ordersToRemove.size() >= q) break;
        }

        removeOrdersFromSolution(solution, ordersToRemove, removed);
        return removed;
    }

    /**
     * Operator 5: Late / Critical Order Removal.
     * Specifically removes orders that are late or close to expiration (ROJO or AMBAR).
     */
    private List<Order> destroyLateOrders(Solution solution, int q) {
        List<Order> removed = new ArrayList<>();
        List<OrderRef> allAssigned = getAllAssignedOrders(solution);
        if (allAssigned.isEmpty()) return removed;

        List<OrderUrgency> urgencyList = new ArrayList<>();
        for (Route r : solution.getRoutes()) {
            double[] deliveryTimes = new double[r.getOrders().size()];
            r.calculateDeliveryTimesAndLateCount(deliveryTimes);

            for (int i = 0; i < r.getOrders().size(); i++) {
                Order order = r.getOrders().get(i);
                double delivTime = deliveryTimes[i];
                double margin = order.getDeadlineHours() - delivTime; // Negative if late
                urgencyList.add(new OrderUrgency(order, margin));
            }
        }

        // Sort ascending by margin: late orders first (negative margins first)
        urgencyList.sort(Comparator.comparingDouble(a -> a.margin));

        Set<Order> ordersToRemove = new HashSet<>();
        int target = Math.min(q, urgencyList.size());
        for (int i = 0; i < target; i++) {
            ordersToRemove.add(urgencyList.get(i).order);
        }

        removeOrdersFromSolution(solution, ordersToRemove, removed);
        return removed;
    }

    // =========================================================================
    // REPAIR OPERATORS
    // =========================================================================

    private void applyRepairOperator(int operatorIndex, Solution solution, List<Order> unassignedPool) {
        // Also combine with any previously unassigned orders in solution
        List<Order> allOrdersToInsert = new ArrayList<>(unassignedPool);
        allOrdersToInsert.addAll(solution.getUnassignedOrders());
        solution.getUnassignedOrders().clear();

        switch (operatorIndex) {
            case 0:
                repairGreedy(solution, allOrdersToInsert);
                break;
            case 1:
                repairRegret2(solution, allOrdersToInsert);
                break;
            case 2:
                repairUrgencyEDF(solution, allOrdersToInsert);
                break;
            case 3:
                repairVehicleCostAware(solution, allOrdersToInsert);
                break;
            default:
                repairGreedy(solution, allOrdersToInsert);
                break;
        }
    }

    /**
     * Operator 1: Greedy Insertion.
     * Iteratively inserts the order and position that yields the minimal increase in fitness.
     */
    private void repairGreedy(Solution solution, List<Order> ordersToInsert) {
        List<Order> pool = new ArrayList<>(ordersToInsert);

        while (!pool.isEmpty()) {
            double bestDeltaFitness = Double.MAX_VALUE;
            Order bestOrder = null;
            Route bestRoute = null;
            int bestPos = -1;
            int bestPoolIdx = -1;

            double baseFitness = evaluator.evaluate(solution);

            for (int i = 0; i < pool.size(); i++) {
                Order order = pool.get(i);

                for (Route r : solution.getRoutes()) {
                    if (!r.getVehicle().isAvailable() || r.getVehicle().getActiveBreakdown() != null) {
                        continue;
                    }
                    // Check capacity
                    if (r.getTotalDemand() + order.getQuantity() > r.getVehicle().getCapacity()) {
                        continue;
                    }

                    int orderCount = r.getOrders().size();
                    for (int pos = 0; pos <= orderCount; pos++) {
                        r.addOrderAt(pos, order);
                        double candFitness = evaluator.evaluate(solution);
                        double delta = candFitness - baseFitness;
                        r.removeOrderAt(pos);

                        if (delta < bestDeltaFitness) {
                            bestDeltaFitness = delta;
                            bestOrder = order;
                            bestRoute = r;
                            bestPos = pos;
                            bestPoolIdx = i;
                        }
                    }
                }
            }

            if (bestOrder != null && bestRoute != null && bestPos >= 0) {
                bestRoute.addOrderAt(bestPos, bestOrder);
                pool.remove(bestPoolIdx);
            } else {
                // No capacity found without overflow: insert remaining into unassigned
                solution.getUnassignedOrders().addAll(pool);
                break;
            }
        }
    }

    /**
     * Operator 2: Regret-2 Insertion.
     * Calculates the regret (deltaCost(second best route) - deltaCost(best route)).
     * Inserts the order with the largest regret first.
     */
    private void repairRegret2(Solution solution, List<Order> ordersToInsert) {
        List<Order> pool = new ArrayList<>(ordersToInsert);

        while (!pool.isEmpty()) {
            double maxRegret = -Double.MAX_VALUE;
            Order bestRegretOrder = null;
            Route bestRegretRoute = null;
            int bestRegretPos = -1;
            int bestRegretPoolIdx = -1;

            double baseFitness = evaluator.evaluate(solution);

            for (int i = 0; i < pool.size(); i++) {
                Order order = pool.get(i);

                double bestDelta = Double.MAX_VALUE;
                Route bestRoute = null;
                int bestPos = -1;

                double secondBestDelta = Double.MAX_VALUE;

                for (Route r : solution.getRoutes()) {
                    if (!r.getVehicle().isAvailable() || r.getVehicle().getActiveBreakdown() != null) {
                        continue;
                    }

                    if (r.getTotalDemand() + order.getQuantity() > r.getVehicle().getCapacity()) {
                        continue;
                    }

                    double routeBestDelta = Double.MAX_VALUE;
                    int routeBestPos = -1;

                    for (int pos = 0; pos <= r.getOrders().size(); pos++) {
                        r.addOrderAt(pos, order);
                        double delta = evaluator.evaluate(solution) - baseFitness;
                        r.removeOrderAt(pos);

                        if (delta < routeBestDelta) {
                            routeBestDelta = delta;
                            routeBestPos = pos;
                        }
                    }

                    if (routeBestDelta < bestDelta) {
                        secondBestDelta = bestDelta;
                        bestDelta = routeBestDelta;
                        bestRoute = r;
                        bestPos = routeBestPos;
                    } else if (routeBestDelta < secondBestDelta) {
                        secondBestDelta = routeBestDelta;
                    }
                }

                if (bestRoute != null) {
                    double regret = (secondBestDelta < Double.MAX_VALUE) ? (secondBestDelta - bestDelta) : bestDelta;
                    if (regret > maxRegret) {
                        maxRegret = regret;
                        bestRegretOrder = order;
                        bestRegretRoute = bestRoute;
                        bestRegretPos = bestPos;
                        bestRegretPoolIdx = i;
                    }
                }
            }

            if (bestRegretOrder != null && bestRegretRoute != null && bestRegretPos >= 0) {
                bestRegretRoute.addOrderAt(bestRegretPos, bestRegretOrder);
                pool.remove(bestRegretPoolIdx);
            } else {
                solution.getUnassignedOrders().addAll(pool);
                break;
            }
        }
    }

    /**
     * Operator 3: Urgency (Earliest Deadline First) Insertion.
     * Sorts unassigned orders by their deadline, then inserts each urgent order into its best position.
     */
    private void repairUrgencyEDF(Solution solution, List<Order> ordersToInsert) {
        List<Order> sorted = new ArrayList<>(ordersToInsert);
        sorted.sort(Comparator.comparingDouble(Order::getDeadlineHours));

        for (Order order : sorted) {
            double bestDelta = Double.MAX_VALUE;
            Route bestRoute = null;
            int bestPos = -1;
            double baseFitness = evaluator.evaluate(solution);

            for (Route r : solution.getRoutes()) {
                if (!r.getVehicle().isAvailable() || r.getVehicle().getActiveBreakdown() != null) {
                    continue;
                }
                
                if (r.getTotalDemand() + order.getQuantity() > r.getVehicle().getCapacity()) {
                    continue;
                }

                for (int pos = 0; pos <= r.getOrders().size(); pos++) {
                    r.addOrderAt(pos, order);
                    double delta = evaluator.evaluate(solution) - baseFitness;
                    r.removeOrderAt(pos);

                    if (delta < bestDelta) {
                        bestDelta = delta;
                        bestRoute = r;
                        bestPos = pos;
                    }
                }
            }

            if (bestRoute != null && bestPos >= 0) {
                bestRoute.addOrderAt(bestPos, order);
            } else {
                solution.getUnassignedOrders().add(order);
            }
        }
    }

    /**
     * Operator 4: Vehicle Cost-Aware Insertion.
     * Evaluates insertion prioritizing economic vehicles (Bici S/3, Moto S/6) if capacity and deadlines permit.
     */
    private void repairVehicleCostAware(Solution solution, List<Order> ordersToInsert) {
        List<Order> pool = new ArrayList<>(ordersToInsert);

        while (!pool.isEmpty()) {
            double bestScore = Double.MAX_VALUE;
            Order bestOrder = null;
            Route bestRoute = null;
            int bestPos = -1;
            int bestPoolIdx = -1;

            double baseFitness = evaluator.evaluate(solution);

            for (int i = 0; i < pool.size(); i++) {
                Order order = pool.get(i);

                for (Route r : solution.getRoutes()) {
                    if (!r.getVehicle().isAvailable() || r.getVehicle().getActiveBreakdown() != null) {
                        continue;
                    }

                    if (r.getTotalDemand() + order.getQuantity() > r.getVehicle().getCapacity()) {
                        continue;
                    }

                    // Weight lower cost per km
                    double vehicleCostFactor = r.getVehicle().getCostPerKm() / 8.00;

                    for (int pos = 0; pos <= r.getOrders().size(); pos++) {
                        r.addOrderAt(pos, order);
                        double delta = evaluator.evaluate(solution) - baseFitness;
                        r.removeOrderAt(pos);

                        double weightedDelta = delta * (0.70 + 0.30 * vehicleCostFactor);

                        if (weightedDelta < bestScore) {
                            bestScore = weightedDelta;
                            bestOrder = order;
                            bestRoute = r;
                            bestPos = pos;
                            bestPoolIdx = i;
                        }
                    }
                }
            }

            if (bestOrder != null && bestRoute != null && bestPos >= 0) {
                bestRoute.addOrderAt(bestPos, bestOrder);
                pool.remove(bestPoolIdx);
            } else {
                solution.getUnassignedOrders().addAll(pool);
                break;
            }
        }
    }

    // =========================================================================
    // ADAPTIVE MECHANISMS (ROULETTE WHEEL & WEIGHT UPDATE)
    // =========================================================================

    private int selectRouletteOperator(List<OperatorStats> operators) {
        double totalWeight = 0.0;
        for (OperatorStats op : operators) {
            totalWeight += op.weight;
        }

        double r = rand.nextDouble() * totalWeight;
        double cumulative = 0.0;

        for (int i = 0; i < operators.size(); i++) {
            cumulative += operators.get(i).weight;
            if (r <= cumulative) {
                return i;
            }
        }

        return operators.size() - 1;
    }

    private void updateOperatorWeights(List<OperatorStats> operators) {
        for (OperatorStats op : operators) {
            if (op.usageCount > 0) {
                double avgScore = op.scoreSum / op.usageCount;
                op.weight = (1.0 - config.reactionFactor) * op.weight + config.reactionFactor * avgScore;
            }
            op.weight = Math.max(op.weight, config.weightMin);
            op.usageCount = 0;
            op.scoreSum = 0.0;
        }
    }

    // =========================================================================
    // HELPER STRUCTURES & UTILITIES
    // =========================================================================

    private static class OrderRef {
        Order order;
        Route route;
        int index;

        OrderRef(Order order, Route route, int index) {
            this.order = order;
            this.route = route;
            this.index = index;
        }
    }

    private static class OrderCostSaving {
        Order order;
        Route route;
        int index;
        double saving;

        OrderCostSaving(Order order, Route route, int index, double saving) {
            this.order = order;
            this.route = route;
            this.index = index;
            this.saving = saving;
        }
    }

    private static class OrderRelatedness {
        Order order;
        double score;

        OrderRelatedness(Order order, double score) {
            this.order = order;
            this.score = score;
        }
    }

    private static class OrderUrgency {
        Order order;
        double margin;

        OrderUrgency(Order order, double margin) {
            this.order = order;
            this.margin = margin;
        }
    }

    private List<OrderRef> getAllAssignedOrders(Solution solution) {
        List<OrderRef> list = new ArrayList<>();
        for (Route r : solution.getRoutes()) {
            List<Order> orders = r.getOrders();
            for (int i = 0; i < orders.size(); i++) {
                list.add(new OrderRef(orders.get(i), r, i));
            }
        }
        return list;
    }

    private int countTotalOrders(Solution solution) {
        return solution.getTotalServedOrders() + solution.getUnassignedOrders().size();
    }

    private void removeOrdersFromSolution(Solution solution, Set<Order> ordersToRemove, List<Order> removedOut) {
        for (Route r : solution.getRoutes()) {
            Iterator<Order> it = r.getOrders().iterator();
            while (it.hasNext()) {
                Order o = it.next();
                if (ordersToRemove.contains(o)) {
                    it.remove();
                    removedOut.add(o);
                }
            }
        }
    }

    private double calculateRemovalCostSaving(Route route, int index) {
        List<Order> orders = route.getOrders();
        Location depot = route.getDepot();

        Location prev = (index == 0) ? depot : orders.get(index - 1).getDestination();
        Location curr = orders.get(index).getDestination();
        Location next = (index == orders.size() - 1) ? depot : orders.get(index + 1).getDestination();

        double distBefore = prev.distanceTo(curr) + curr.distanceTo(next);
        double distAfter = prev.distanceTo(next);
        double deltaDist = distBefore - distAfter;

        return deltaDist * route.getVehicle().getCostPerKm();
    }

    private void log(String msg) {
        logs.add(msg);
        if (config.verboseLogging) {
            System.out.println(msg);
        }
    }
}
