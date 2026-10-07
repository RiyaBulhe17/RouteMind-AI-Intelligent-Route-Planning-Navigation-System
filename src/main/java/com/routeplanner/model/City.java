package com.routeplanner.model;

/**
 * Represents a node in the route graph: a city / location, optionally
 * enriched with a "landmark" description that the AI will use for narration.
 */
public class City {
    private final int id;
    private final String name;
    private final double lat;
    private final double lon;
    private final String landmark; // free-text landmark / point-of-interest info

    public City(int id, String name, double lat, double lon, String landmark) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.landmark = landmark;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public String getLandmark() { return landmark; }

    @Override
    public String toString() { return name; }
}
