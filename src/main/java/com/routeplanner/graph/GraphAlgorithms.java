package com.routeplanner.graph;

import com.routeplanner.model.City;

import java.util.*;

/**
 * Collection of classic pathfinding algorithms over the Graph model.
 * - BFS: fewest hops (ignores edge weight)
 * - DFS: any path found via depth-first exploration (not optimal)
 * - Dijkstra: true shortest weighted path (the "optimal route")
 * - Greedy Best-First: heuristic-driven (straight-line distance to goal)
 */
public class GraphAlgorithms {

    public enum Algorithm { BFS, DFS, DIJKSTRA, GREEDY }

    public static RouteResult findRoute(Graph graph, int startId, int endId, Algorithm algorithm) {
        return switch (algorithm) {
            case BFS -> bfs(graph, startId, endId);
            case DFS -> dfs(graph, startId, endId);
            case DIJKSTRA -> dijkstra(graph, startId, endId);
            case GREEDY -> greedyBestFirst(graph, startId, endId);
        };
    }

    // ---------------------------------------------------------------
    // BFS - shortest path by number of edges (unweighted)
    // ---------------------------------------------------------------
    private static RouteResult bfs(Graph graph, int startId, int endId) {
        Map<Integer, Integer> parent = new HashMap<>();
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.add(startId);
        visited.add(startId);
        int explored = 0;

        while (!queue.isEmpty()) {
            int current = queue.poll();
            explored++;
            if (current == endId) break;
            for (Graph.Neighbor n : graph.getNeighbors(current)) {
                if (!visited.contains(n.cityId)) {
                    visited.add(n.cityId);
                    parent.put(n.cityId, current);
                    queue.add(n.cityId);
                }
            }
        }

        if (!visited.contains(endId)) {
            return new RouteResult(Collections.emptyList(), 0, explored, "BFS", false);
        }
        List<City> path = reconstructPath(graph, parent, startId, endId);
        double total = totalWeightOf(graph, path);
        return new RouteResult(path, total, explored, "BFS (fewest hops)", true);
    }

    // ---------------------------------------------------------------
    // DFS - depth-first exploration; finds *a* path, not necessarily optimal
    // ---------------------------------------------------------------
    private static RouteResult dfs(Graph graph, int startId, int endId) {
        Map<Integer, Integer> parent = new HashMap<>();
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(startId);
        int explored = 0;

        while (!stack.isEmpty()) {
            int current = stack.pop();
            if (visited.contains(current)) continue;
            visited.add(current);
            explored++;
            if (current == endId) break;
            for (Graph.Neighbor n : graph.getNeighbors(current)) {
                if (!visited.contains(n.cityId)) {
                    parent.putIfAbsent(n.cityId, current);
                    stack.push(n.cityId);
                }
            }
        }

        if (!visited.contains(endId)) {
            return new RouteResult(Collections.emptyList(), 0, explored, "DFS", false);
        }
        List<City> path = reconstructPath(graph, parent, startId, endId);
        double total = totalWeightOf(graph, path);
        return new RouteResult(path, total, explored, "DFS (depth-first, not optimal)", true);
    }

    // ---------------------------------------------------------------
    // Dijkstra - the true optimal (minimum total weight) route
    // ---------------------------------------------------------------
    private static RouteResult dijkstra(Graph graph, int startId, int endId) {
        Map<Integer, Double> dist = new HashMap<>();
        Map<Integer, Integer> parent = new HashMap<>();
        PriorityQueue<double[]> queue = new PriorityQueue<>(Comparator.comparingDouble(a -> a[1]));

        for (City c : graph.getAllCities()) dist.put(c.getId(), Double.POSITIVE_INFINITY);
        dist.put(startId, 0.0);
        queue.add(new double[]{startId, 0.0});
        Set<Integer> visited = new HashSet<>();
        int explored = 0;

        while (!queue.isEmpty()) {
            double[] top = queue.poll();
            int current = (int) top[0];
            if (visited.contains(current)) continue;
            visited.add(current);
            explored++;
            if (current == endId) break;

            for (Graph.Neighbor n : graph.getNeighbors(current)) {
                double newDist = dist.get(current) + n.weight;
                if (newDist < dist.getOrDefault(n.cityId, Double.POSITIVE_INFINITY)) {
                    dist.put(n.cityId, newDist);
                    parent.put(n.cityId, current);
                    queue.add(new double[]{n.cityId, newDist});
                }
            }
        }

        if (dist.getOrDefault(endId, Double.POSITIVE_INFINITY) == Double.POSITIVE_INFINITY) {
            return new RouteResult(Collections.emptyList(), 0, explored, "Dijkstra", false);
        }
        List<City> path = reconstructPath(graph, parent, startId, endId);
        return new RouteResult(path, dist.get(endId), explored, "Dijkstra (optimal weighted shortest path)", true);
    }

    // ---------------------------------------------------------------
    // Greedy Best-First - uses straight-line (haversine) distance to goal as heuristic
    // ---------------------------------------------------------------
    private static RouteResult greedyBestFirst(Graph graph, int startId, int endId) {
        City goal = graph.getCity(endId);
        Map<Integer, Integer> parent = new HashMap<>();
        Set<Integer> visited = new HashSet<>();
        PriorityQueue<double[]> queue = new PriorityQueue<>(Comparator.comparingDouble(a -> a[1]));
        queue.add(new double[]{startId, heuristic(graph.getCity(startId), goal)});
        int explored = 0;

        while (!queue.isEmpty()) {
            double[] top = queue.poll();
            int current = (int) top[0];
            if (visited.contains(current)) continue;
            visited.add(current);
            explored++;
            if (current == endId) break;

            for (Graph.Neighbor n : graph.getNeighbors(current)) {
                if (!visited.contains(n.cityId)) {
                    parent.putIfAbsent(n.cityId, current);
                    queue.add(new double[]{n.cityId, heuristic(graph.getCity(n.cityId), goal)});
                }
            }
        }

        if (!visited.contains(endId)) {
            return new RouteResult(Collections.emptyList(), 0, explored, "Greedy Best-First", false);
        }
        List<City> path = reconstructPath(graph, parent, startId, endId);
        double total = totalWeightOf(graph, path);
        return new RouteResult(path, total, explored, "Greedy Best-First (heuristic-driven, not guaranteed optimal)", true);
    }

    private static double heuristic(City a, City b) {
        // Haversine distance in km as the straight-line heuristic
        double R = 6371.0;
        double dLat = Math.toRadians(b.getLat() - a.getLat());
        double dLon = Math.toRadians(b.getLon() - a.getLon());
        double lat1 = Math.toRadians(a.getLat());
        double lat2 = Math.toRadians(b.getLat());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
        return R * c;
    }

    // ---------------------------------------------------------------
    private static List<City> reconstructPath(Graph graph, Map<Integer, Integer> parent, int startId, int endId) {
        LinkedList<City> path = new LinkedList<>();
        Integer current = endId;
        path.addFirst(graph.getCity(current));
        while (current != startId) {
            Integer prev = parent.get(current);
            if (prev == null) break; // safety
            path.addFirst(graph.getCity(prev));
            current = prev;
        }
        return path;
    }

    private static double totalWeightOf(Graph graph, List<City> path) {
        double total = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            int fromId = path.get(i).getId();
            int toId = path.get(i + 1).getId();
            for (Graph.Neighbor n : graph.getNeighbors(fromId)) {
                if (n.cityId == toId) {
                    total += n.weight;
                    break;
                }
            }
        }
        return total;
    }
}
