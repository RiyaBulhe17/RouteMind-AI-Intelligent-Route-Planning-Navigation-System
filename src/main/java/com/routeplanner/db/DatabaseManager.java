package com.routeplanner.db;

import com.routeplanner.model.City;
import com.routeplanner.model.Edge;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all SQL persistence for the route planner.
 * Uses an embedded SQLite database file (route_planner.db) so the
 * city/edge network survives between runs, and is queried with real SQL.
 */
public class DatabaseManager {

    private final String dbUrl;
    private Connection connection;

    public DatabaseManager(String dbFilePath) {
        this.dbUrl = "jdbc:sqlite:" + dbFilePath;
    }

    public void connect() throws SQLException {
        connection = DriverManager.getConnection(dbUrl);
        createSchema();
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) {}
    }

    private void createSchema() throws SQLException {
        String citiesSql = """
            CREATE TABLE IF NOT EXISTS cities (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                lat REAL NOT NULL,
                lon REAL NOT NULL,
                landmark TEXT
            );
        """;

        String edgesSql = """
            CREATE TABLE IF NOT EXISTS edges (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                from_city_id INTEGER NOT NULL,
                to_city_id INTEGER NOT NULL,
                weight REAL NOT NULL,
                FOREIGN KEY (from_city_id) REFERENCES cities(id),
                FOREIGN KEY (to_city_id) REFERENCES cities(id)
            );
        """;

        try (Statement st = connection.createStatement()) {
            st.execute(citiesSql);
            st.execute(edgesSql);
        }
    }

    /** Seeds the database with a small demo network the first time it is empty. */
    public void seedIfEmpty() throws SQLException {
        if (countCities() > 0) return;

        int a = insertCity("Delhi", 28.6139, 77.2090, "Red Fort - Mughal-era fortress and UNESCO World Heritage Site");
        int b = insertCity("Agra", 27.1767, 78.0081, "Taj Mahal - iconic white marble mausoleum");
        int c = insertCity("Jaipur", 26.9124, 75.7873, "Hawa Mahal - the Palace of Winds");
        int d = insertCity("Lucknow", 26.8467, 80.9462, "Bara Imambara - grand Nawabi architecture");
        int e = insertCity("Bhopal", 23.2599, 77.4126, "Upper Lake - one of the largest man-made lakes in Asia");
        int f = insertCity("Mumbai", 19.0760, 72.8777, "Gateway of India - waterfront monument overlooking the Arabian Sea");
        int g = insertCity("Pune", 18.5204, 73.8567, "Shaniwar Wada - historic fortified palace");
        int h = insertCity("Indore", 22.7196, 75.8577, "Rajwada Palace - seven-storey historic palace");

        insertEdge(a, b, 233);
        insertEdge(a, c, 281);
        insertEdge(a, d, 555);
        insertEdge(c, h, 587);
        insertEdge(b, h, 490);
        insertEdge(d, e, 595);
        insertEdge(h, e, 190);
        insertEdge(e, g, 606);
        insertEdge(g, f, 149);
        insertEdge(h, g, 505);
        insertEdge(c, f, 1160);
    }

    public int countCities() throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM cities")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int insertCity(String name, double lat, double lon, String landmark) throws SQLException {
        String sql = "INSERT INTO cities(name, lat, lon, landmark) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setDouble(2, lat);
            ps.setDouble(3, lon);
            ps.setString(4, landmark);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public void insertEdge(int fromId, int toId, double weight) throws SQLException {
        String sql = "INSERT INTO edges(from_city_id, to_city_id, weight) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, fromId);
            ps.setInt(2, toId);
            ps.setDouble(3, weight);
            ps.executeUpdate();
        }
    }

    public List<City> getAllCities() throws SQLException {
        List<City> cities = new ArrayList<>();
        String sql = "SELECT id, name, lat, lon, landmark FROM cities ORDER BY name";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                cities.add(new City(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("lat"),
                        rs.getDouble("lon"),
                        rs.getString("landmark")
                ));
            }
        }
        return cities;
    }

    public List<Edge> getAllEdges() throws SQLException {
        List<Edge> edges = new ArrayList<>();
        String sql = "SELECT id, from_city_id, to_city_id, weight FROM edges";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                edges.add(new Edge(
                        rs.getInt("id"),
                        rs.getInt("from_city_id"),
                        rs.getInt("to_city_id"),
                        rs.getDouble("weight")
                ));
            }
        }
        return edges;
    }

    public void deleteAll() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("DELETE FROM edges");
            st.execute("DELETE FROM cities");
        }
    }
}
