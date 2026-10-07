package com.routeplanner.ui;

import com.routeplanner.db.DatabaseManager;
import com.routeplanner.model.City;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Simple dialog to add new cities (with landmark data) and edges (roads)
 * directly into the SQLite-backed tables.
 */
public class ManageDataDialog extends JDialog {

    private final DatabaseManager db;

    public ManageDataDialog(Frame owner, DatabaseManager db) {
        super(owner, "Manage Cities & Edges", true);
        this.db = db;
        setSize(500, 420);
        setLocationRelativeTo(owner);
        buildUI();
    }

    private void buildUI() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Add City", buildAddCityPanel());
        tabs.add("Add Edge (Road)", buildAddEdgePanel());
        add(tabs);
    }

    private JPanel buildAddCityPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JTextField nameField = new JTextField(20);
        JTextField latField = new JTextField(20);
        JTextField lonField = new JTextField(20);
        JTextArea landmarkArea = new JTextArea(4, 20);
        landmarkArea.setLineWrap(true);
        landmarkArea.setWrapStyleWord(true);

        int row = 0;
        addRow(panel, gc, row++, "City Name:", nameField);
        addRow(panel, gc, row++, "Latitude:", latField);
        addRow(panel, gc, row++, "Longitude:", lonField);

        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        panel.add(new JLabel("Landmark Info:"), gc);
        gc.gridx = 1; gc.gridy = row; gc.weightx = 1;
        panel.add(new JScrollPane(landmarkArea), gc);
        row++;

        JButton addButton = new JButton("Add City");
        gc.gridx = 1; gc.gridy = row; gc.anchor = GridBagConstraints.EAST;
        panel.add(addButton, gc);

        addButton.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                double lat = Double.parseDouble(latField.getText().trim());
                double lon = Double.parseDouble(lonField.getText().trim());
                String landmark = landmarkArea.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "City name is required.");
                    return;
                }
                db.insertCity(name, lat, lon, landmark);
                JOptionPane.showMessageDialog(this, "City '" + name + "' added.");
                nameField.setText("");
                latField.setText("");
                lonField.setText("");
                landmarkArea.setText("");
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Latitude/Longitude must be valid numbers.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
            }
        });

        return panel;
    }

    private JPanel buildAddEdgePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<City> fromBox = new JComboBox<>();
        JComboBox<City> toBox = new JComboBox<>();
        JTextField weightField = new JTextField(20);

        try {
            List<City> cities = db.getAllCities();
            for (City c : cities) { fromBox.addItem(c); toBox.addItem(c); }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Failed to load cities: " + ex.getMessage());
        }

        int row = 0;
        addRow(panel, gc, row++, "From City:", fromBox);
        addRow(panel, gc, row++, "To City:", toBox);
        addRow(panel, gc, row++, "Distance/Weight (km):", weightField);

        JButton addButton = new JButton("Add Road");
        gc.gridx = 1; gc.gridy = row; gc.anchor = GridBagConstraints.EAST;
        panel.add(addButton, gc);

        addButton.addActionListener(e -> {
            try {
                City from = (City) fromBox.getSelectedItem();
                City to = (City) toBox.getSelectedItem();
                double weight = Double.parseDouble(weightField.getText().trim());
                if (from == null || to == null || from.getId() == to.getId()) {
                    JOptionPane.showMessageDialog(this, "Please select two different cities.");
                    return;
                }
                db.insertEdge(from.getId(), to.getId(), weight);
                JOptionPane.showMessageDialog(this, "Road added: " + from.getName() + " <-> " + to.getName());
                weightField.setText("");
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Weight must be a valid number.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
            }
        });

        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints gc, int row, String label, JComponent field) {
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; gc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(label), gc);
        gc.gridx = 1; gc.gridy = row; gc.weightx = 1;
        panel.add(field, gc);
    }
}
