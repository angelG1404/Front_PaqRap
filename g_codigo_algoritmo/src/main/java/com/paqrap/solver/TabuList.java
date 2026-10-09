package com.paqrap.solver;

import com.paqrap.model.Move;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Tabu memory structure with dynamic adaptive tenure support.
 */
public class TabuList {
    private final Map<Move, Integer> tabuMap; // Move -> Expiration iteration
    private int currentTenure;
    private final int minTenure;
    private final int maxTenure;

    public TabuList(int initialTenure, int minTenure, int maxTenure) {
        this.tabuMap = new HashMap<>();
        this.currentTenure = initialTenure;
        this.minTenure = minTenure;
        this.maxTenure = maxTenure;
    }

    public int getCurrentTenure() {
        return currentTenure;
    }

    /**
     * Dynamically increases tabu tenure (used during search stagnation / diversification).
     */
    public void increaseTenure(int step) {
        this.currentTenure = Math.min(maxTenure, this.currentTenure + step);
    }

    /**
     * Dynamically decreases tabu tenure (used during search progress / intensification).
     */
    public void decreaseTenure(int step) {
        this.currentTenure = Math.max(minTenure, this.currentTenure - step);
    }

    /**
     * Adds a move to the tabu list until currentIteration + currentTenure.
     */
    public void addMove(Move move, int currentIteration) {
        int expiry = currentIteration + currentTenure;
        tabuMap.put(move, expiry);
    }

    /**
     * Checks if a move is currently tabu.
     */
    public boolean isTabu(Move move, int currentIteration) {
        Integer expiry = tabuMap.get(move);
        if (expiry == null) {
            return false;
        }
        if (expiry <= currentIteration) {
            tabuMap.remove(move);
            return false;
        }
        return true;
    }

    /**
     * Clean up expired entries to optimize memory.
     */
    public void purgeExpired(int currentIteration) {
        Iterator<Map.Entry<Move, Integer>> it = tabuMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Move, Integer> entry = it.next();
            if (entry.getValue() <= currentIteration) {
                it.remove();
            }
        }
    }

    public void clear() {
        tabuMap.clear();
    }
}
