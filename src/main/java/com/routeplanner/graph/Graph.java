package com.routeplanner.graph;

import com.routeplanner.model.City;
import com.routeplanner.model.Edge;

import java.util.*;

/**
 * In-memory graph representation built from the SQL-backed city/edge tables.
 * Edges are treated as undirected (bidirectional roads) since routes are
 * typically travelable both ways; adjust addEdge() if directed roads are needed.
 */
public class Graph {

    public static class Neighbor {
        public final int cityId;
        public final double weight;
        public Neighbor(int cityId, double weight) {
            this.cityId = cityId;
            this.weight = weight;
        }
    }

    private final Map<Integer, City> cities = new HashMap<>();
    private final Map<Integer, List<Neighbor>> adjacency = new HashMap<>();

    public void addCity(City city) {
        cities.put(city.getId(), city);
        adjacency.putIfAbsent(city.getId(), new ArrayList<>());
    }

    public void addEdge(Edge edge) {
        adjacency.computeIfAbsent(edge.getFromCityId(), k -> new ArrayList<>())
                .add(new Neighbor(edge.getToCityId(), edge.getWeight()));
        adjacency.computeIfAbsent(edge.getToCityId(), k -> new ArrayList<>())
                .add(new Neighbor(edge.getFromCityId(), edge.getWeight()));
    }

    public static Graph build(List<City> cities, List<Edge> edges) {
        Graph g = new Graph();
        for (City c : cities) g.addCity(c);
        for (Edge e : edges) g.addEdge(e);
        return g;
    }

    public City getCity(int id) { return cities.get(id); }
    public Collection<City> getAllCities() { return cities.values(); }
    public List<Neighbor> getNeighbors(int cityId) {
        return adjacency.getOrDefault(cityId, Collections.emptyList());
    }
}
