package com.routeplanner.graph;

import com.routeplanner.model.City;

import java.util.List;

/** Holds the outcome of a pathfinding run: the ordered path and its stats. */
public class RouteResult {
    private final List<City> path;
    private final double totalWeight;
    private final int nodesExplored;
    private final String algorithmUsed;
    private final boolean found;

    public RouteResult(List<City> path, double totalWeight, int nodesExplored, String algorithmUsed, boolean found) {
        this.path = path;
        this.totalWeight = totalWeight;
        this.nodesExplored = nodesExplored;
        this.algorithmUsed = algorithmUsed;
        this.found = found;
    }

    public List<City> getPath() { return path; }
    public double getTotalWeight() { return totalWeight; }
    public int getNodesExplored() { return nodesExplored; }
    public String getAlgorithmUsed() { return algorithmUsed; }
    public boolean isFound() { return found; }
}
