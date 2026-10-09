package com.paqrap.model;

import java.util.Objects;

/**
 * Encapsulates a neighborhood move operator in Tabu Search.
 */
public class Move {
    public enum MoveType {
        RELOCATE,
        SWAP,
        TWO_OPT,
        VEHICLE_REASSIGN
    }

    private final MoveType type;
    private final int routeIndex1;
    private final int pos1;
    private final int routeIndex2;
    private final int pos2;
    private final String orderId1;
    private final String orderId2;
    private final VehicleType newVehicleType;

    public Move(MoveType type, int routeIndex1, int pos1, int routeIndex2, int pos2, String orderId1, String orderId2, VehicleType newVehicleType) {
        this.type = type;
        this.routeIndex1 = routeIndex1;
        this.pos1 = pos1;
        this.routeIndex2 = routeIndex2;
        this.pos2 = pos2;
        this.orderId1 = orderId1;
        this.orderId2 = orderId2;
        this.newVehicleType = newVehicleType;
    }

    public MoveType getType() {
        return type;
    }

    public int getRouteIndex1() {
        return routeIndex1;
    }

    public int getPos1() {
        return pos1;
    }

    public int getRouteIndex2() {
        return routeIndex2;
    }

    public int getPos2() {
        return pos2;
    }

    public String getOrderId1() {
        return orderId1;
    }

    public String getOrderId2() {
        return orderId2;
    }

    public VehicleType getNewVehicleType() {
        return newVehicleType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;
        return routeIndex1 == move.routeIndex1 &&
                pos1 == move.pos1 &&
                routeIndex2 == move.routeIndex2 &&
                pos2 == move.pos2 &&
                type == move.type &&
                Objects.equals(orderId1, move.orderId1) &&
                Objects.equals(orderId2, move.orderId2) &&
                newVehicleType == move.newVehicleType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, routeIndex1, pos1, routeIndex2, pos2, orderId1, orderId2, newVehicleType);
    }

    @Override
    public String toString() {
        switch (type) {
            case RELOCATE:
                return String.format("RELOCATE(Order %s: R%d[%d] -> R%d[%d])", orderId1, routeIndex1, pos1, routeIndex2, pos2);
            case SWAP:
                return String.format("SWAP(Order %s: R%d[%d] <-> Order %s: R%d[%d])", orderId1, routeIndex1, pos1, orderId2, routeIndex2, pos2);
            case TWO_OPT:
                return String.format("TWO_OPT(R%d[%d..%d])", routeIndex1, pos1, pos2);
            case VEHICLE_REASSIGN:
                return String.format("VEHICLE_REASSIGN(R%d -> %s)", routeIndex1, newVehicleType);
            default:
                return "MOVE()";
        }
    }
}
