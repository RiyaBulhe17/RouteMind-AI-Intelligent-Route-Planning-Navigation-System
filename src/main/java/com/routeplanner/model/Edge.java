package com.routeplanner.model;

/**
 * Represents a directed/undirected weighted connection between two cities.
 * Weight is treated as distance/cost (e.g. km, minutes).
 */
public class Edge {
    private final int id;
    private final int fromCityId;
    private final int toCityId;
    private final double weight;

    public Edge(int id, int fromCityId, int toCityId, double weight) {
        this.id = id;
        this.fromCityId = fromCityId;
        this.toCityId = toCityId;
        this.weight = weight;
    }

    public int getId() { return id; }
    public int getFromCityId() { return fromCityId; }
    public int getToCityId() { return toCityId; }
    public double getWeight() { return weight; }
}
