package com.paqrap.solver;

import com.paqrap.model.*;

import java.util.*;

/**
 * Adaptive Tabu Search (ATS) Metaheuristic Solver for PaqRap VRP.
 */
public class ATSOptimizer implements RouteOptimizer {

    public static class ATSConfig {
        public int maxIterations = 300;
        public int initialTenure = 7;
        public int minTenure = 3;
        public int maxTenure = 25;
        public int stagnationThreshold = 20; // Iterations without improvement to trigger adaptation
        public double diversificationPenaltyWeight = 50.0; // Penalty weight for frequent move edges
        public boolean verboseLogging = true;
    }

    private final ATSConfig config;
    private final SolutionEvaluator evaluator;
    private final TabuList tabuList;
    private final Map<String, Integer> moveFrequencyMap; // Track move frequency for long-term memory
    private final List<String> logs;

    public ATSOptimizer() {
        this(new ATSConfig());
    }

    public ATSOptimizer(ATSConfig config) {
        this.config = config;
        this.evaluator = new SolutionEvaluator();
        this.tabuList = new TabuList(config.initialTenure, config.minTenure, config.maxTenure);
        this.moveFrequencyMap = new HashMap<>();
        this.logs = new ArrayList<>();
    }

    @Override
    public String getAlgorithmName() {
        return "ATS (Adaptive Tabu Search)";
    }

    @Override
    public List<String> getLogs() {
        return Collections.unmodifiableList(logs);
    }

    //CAMBIO ITERACION 01: Estado de terminación
    public enum SolutionStatus { OK, COLAPSO_LOGISTICO }
    private SolutionStatus finalStatus;
    
    public SolutionStatus getFinalStatus() { return finalStatus; }

    /**
     * Executes Adaptive Tabu Search optimization.
     */
    @Override
    public Solution solve(Solution initialSolution) {
        return solve(initialSolution, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Executes Adaptive Tabu Search optimization with constraints.
     */
    public Solution solve(Solution initialSolution, List<Maintenance> maintenances, List<Breakdown> breakdowns, List<Roadblock> roadblocks) {
        // Inicialización de estado y variables de control
        finalStatus = SolutionStatus.OK;
        
        Solution currentSolution = initialSolution.cloneSolution();
        evaluator.evaluate(currentSolution);

        Solution bestSolution = currentSolution.cloneSolution();

        int iterationsWithoutImprovement = 0;
        int tenureAdaptationsCount = 0;
        int diversificationEventsCount = 0;

        log(String.format("=== INICIO BÚSQUEDA TABÚ ADAPTATIVA (ATS) ==="));
        log(String.format("Solución Inicial - Costo: S/%.2f, Fitness: %.2f, Sin Asignar: %d",
                currentSolution.getTotalMonetaryCost(), currentSolution.getFitness(), currentSolution.getUnassignedOrders().size()));
        log(String.format("Configuración ATS: Iteraciones=%d, Tenure Inicial=%d [%d..%d], Stagnation Limit=%d",
                config.maxIterations, config.initialTenure, config.minTenure, config.maxTenure, config.stagnationThreshold));

        // Bucle principal del algoritmo ATS
        for (int iter = 1; iter <= config.maxIterations; iter++) {
            // 1. Generar vecindario: todos los movimientos posibles (Relocate, Swap, Reassign)
            List<CandidateMove> candidateMoves = generateNeighborhood(currentSolution, maintenances, breakdowns, roadblocks);

            if (candidateMoves.isEmpty()) {
                log(String.format("Iter %d: Vecindario vacío. Deteniendo ATS.", iter));
                break;
            }

            CandidateMove bestMoveCandidate = null;

            // 2. Evaluar candidatos considerando restricciones Tabú y memoria
            for (CandidateMove cand : candidateMoves) {
                Move move = cand.move;
                double candidateFitness = cand.evaluatedFitness;

                // Penalización por frecuencia (Memoria a largo plazo para diversificación)
                String moveSignature = move.getType() + ":" + move.getOrderId1() + "->" + move.getOrderId2();
                int frequency = moveFrequencyMap.getOrDefault(moveSignature, 0);
                double penalizedFitness = candidateFitness + (frequency * config.diversificationPenaltyWeight);

                cand.penalizedFitness = penalizedFitness;

                // Verificación de lista Tabú y Criterio de Aspiración
                boolean isTabu = tabuList.isTabu(move, iter);
                boolean satisfiesAspiration = candidateFitness < bestSolution.getFitness();

                if (!isTabu || satisfiesAspiration) {
                    if (bestMoveCandidate == null || cand.penalizedFitness < bestMoveCandidate.penalizedFitness) {
                        bestMoveCandidate = cand;
                    }
                }
            }

            // 3. Selección del mejor movimiento
            if (bestMoveCandidate == null) {
                // Fallback si todos los movimientos están prohibidos por la lista tabú
                log(String.format("Iter %d: Todos los movimientos son tabú. Diversificando tenencia.", iter));
                tabuList.decreaseTenure(2);
                continue;
            }

            // Aplicar el movimiento seleccionado y actualizar la lista Tabú
            currentSolution = bestMoveCandidate.resultingSolution;
            Move chosenMove = bestMoveCandidate.move;
            tabuList.addMove(chosenMove, iter);

            // Actualizar memoria de frecuencia
            String moveSignature = chosenMove.getType() + ":" + chosenMove.getOrderId1() + "->" + chosenMove.getOrderId2();
            moveFrequencyMap.put(moveSignature, moveFrequencyMap.getOrDefault(moveSignature, 0) + 1);

            // 4. Adaptación de parámetros (Mecanismo Adaptativo)
            if (currentSolution.getFitness() < bestSolution.getFitness() - 1e-4) {
                // Mejora encontrada: actualizar mejor solución global
                bestSolution = currentSolution.cloneSolution();
                iterationsWithoutImprovement = 0;

                // Intensificación: reducir tenencia tabú para explorar más cerca
                tabuList.decreaseTenure(1);
                tenureAdaptationsCount++;

                if (config.verboseLogging) {
                    log(String.format("Iter %3d: [MEJORA GLOBAL] Fitness: %.2f | Costo: S/%.2f | Move: %s | TabuTenure: %d",
                            iter, bestSolution.getFitness(), bestSolution.getTotalMonetaryCost(), chosenMove.toString(), tabuList.getCurrentTenure()));
                }
            } else {
                iterationsWithoutImprovement++;

                // Diversificación: aumentar tenencia si hay estancamiento
                if (iterationsWithoutImprovement >= config.stagnationThreshold) {
                    tabuList.increaseTenure(3);
                    tenureAdaptationsCount++;
                    diversificationEventsCount++;
                    iterationsWithoutImprovement = 0;

                    log(String.format("Iter %3d: [ADAPTACIÓN ATS - DIVERSIFICACIÓN] Estancamiento detectado -> Tenure incrementado a %d",
                            iter, tabuList.getCurrentTenure()));
                }
            }

            // 5. Limpieza de memoria (Tabú)
            tabuList.purgeExpired(iter);
        }

        // Resumen final de la optimización
        log(String.format("=== RESUMEN DE OPTIMIZACIÓN ATS ==="));
        log(String.format("Fitness Final: %.2f (Mejora: %.2f%%)",
                bestSolution.getFitness(),
                ((initialSolution.getFitness() - bestSolution.getFitness()) / initialSolution.getFitness()) * 100.0));
        log(String.format("Costo Operativo Final: S/%.2f | Distancia Total: %.2f km",
                bestSolution.getTotalMonetaryCost(), bestSolution.getTotalDistanceKm()));
        log(String.format("Adaptaciones de Tenencia Tabú: %d | Eventos de Diversificación: %d",
                tenureAdaptationsCount, diversificationEventsCount));

        return bestSolution;
    }

    private static class CandidateMove {
        Move move;
        Solution resultingSolution;
        double evaluatedFitness;
        double penalizedFitness;

        CandidateMove(Move move, Solution resultingSolution, double evaluatedFitness) {
            this.move = move;
            this.resultingSolution = resultingSolution;
            this.evaluatedFitness = evaluatedFitness;
            this.penalizedFitness = evaluatedFitness;
        }
    }

    private List<CandidateMove> generateNeighborhood(Solution currentSolution, List<Maintenance> maintenances, List<Breakdown> breakdowns, List<Roadblock> roadblocks) {
        List<CandidateMove> candidates = new ArrayList<>();
        List<Route> routes = currentSolution.getRoutes();
        
        // 1. RELOCATE OPERATOR (Move an order from route R1 to route R2 or different position in R1)
        for (int r1 = 0; r1 < routes.size(); r1++) {
            Route route1 = routes.get(r1);
            List<Order> orders1 = route1.getOrders();

            for (int p1 = 0; p1 < orders1.size(); p1++) {
                Order orderToMove = orders1.get(p1);

                for (int r2 = 0; r2 < routes.size(); r2++) {
                    Route route2 = routes.get(r2);
                    int maxP2 = (r1 == r2) ? route2.getOrders().size() - 1 : route2.getOrders().size();

                    for (int p2 = 0; p2 <= maxP2; p2++) {
                        if (r1 == r2 && (p1 == p2 || p1 == p2 - 1)) continue;

                        Solution neighbor = currentSolution.cloneSolution();
                        Route nr1 = neighbor.getRoutes().get(r1);
                        Route nr2 = neighbor.getRoutes().get(r2);

                        Order removed = nr1.removeOrderAt(p1);
                        nr2.addOrderAt(p2, removed);

                        double fit = evaluator.evaluate(neighbor);
                        
                        //CAMBIO ITERACION 01: Filtrado
                        if (!neighbor.isFeasible(maintenances, breakdowns)) continue;

                        Move move = new Move(Move.MoveType.RELOCATE, r1, p1, r2, p2, orderToMove.getId(), null, null);
                        candidates.add(new CandidateMove(move, neighbor, fit));
                    }
                }
            }
        }

        // 2. SWAP OPERATOR (Swap two orders between route R1 and route R2)
        for (int r1 = 0; r1 < routes.size(); r1++) {
            Route route1 = routes.get(r1);
            List<Order> orders1 = route1.getOrders();

            for (int p1 = 0; p1 < orders1.size(); p1++) {
                Order o1 = orders1.get(p1);

                for (int r2 = r1; r2 < routes.size(); r2++) {
                    Route route2 = routes.get(r2);
                    List<Order> orders2 = route2.getOrders();
                    int startP2 = (r1 == r2) ? p1 + 1 : 0;

                    for (int p2 = startP2; p2 < orders2.size(); p2++) {
                        Order o2 = orders2.get(p2);

                        Solution neighbor = currentSolution.cloneSolution();
                        Route nr1 = neighbor.getRoutes().get(r1);
                        Route nr2 = neighbor.getRoutes().get(r2);

                        if (r1 == r2) {
                            Collections.swap(nr1.getOrders(), p1, p2);
                        } else {
                            Order temp1 = nr1.getOrders().get(p1);
                            Order temp2 = nr2.getOrders().get(p2);
                            nr1.getOrders().set(p1, temp2);
                            nr2.getOrders().set(p2, temp1);
                        }

                        double fit = evaluator.evaluate(neighbor);
                        
                        //CAMBIO ITERACION 01: Filtrado
                        if (!neighbor.isFeasible(maintenances, breakdowns)) continue;

                        Move move = new Move(Move.MoveType.SWAP, r1, p1, r2, p2, o1.getId(), o2.getId(), null);
                        candidates.add(new CandidateMove(move, neighbor, fit));
                    }
                }
            }
        }

        // 3. VEHICLE TYPE REASSIGN OPERATOR (Change vehicle type assigned to route R1 if cost/capacity allows)
        for (int r = 0; r < routes.size(); r++) {
            Route route = routes.get(r);
            Vehicle v = route.getVehicle();

            for (VehicleType newType : VehicleType.values()) {
                if (newType != v.getType()) {
                    Solution neighbor = currentSolution.cloneSolution();
                    Route nr = neighbor.getRoutes().get(r);
                    nr.getVehicle().setType(newType);

                    double fit = evaluator.evaluate(neighbor);
                    
                    //CAMBIO ITERACION 01: Filtrado
                    if (!neighbor.isFeasible(maintenances, breakdowns)) continue;

                    Move move = new Move(Move.MoveType.VEHICLE_REASSIGN, r, 0, r, 0, null, null, newType);
                    candidates.add(new CandidateMove(move, neighbor, fit));
                }
            }
        }

        return candidates;
    }

    private void log(String msg) {
        logs.add(msg);
        if (config.verboseLogging) {
            System.out.println(msg);
        }
    }
}
