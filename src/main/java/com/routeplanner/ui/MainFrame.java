package com.routeplanner.ui;

import com.routeplanner.ai.NarrationPromptBuilder;
import com.routeplanner.ai.OpenRouterClient;
import com.routeplanner.db.DatabaseManager;
import com.routeplanner.graph.Graph;
import com.routeplanner.graph.GraphAlgorithms;
import com.routeplanner.graph.RouteResult;
import com.routeplanner.model.City;
import com.routeplanner.model.Edge;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Minimal Swing UI for the AI-Augmented Route Planner.
 *
 * Layout:
 *  - Top: From / To / Algorithm selectors + "Find Route" button
 *  - Middle-left: computed route + stats
 *  - Middle-right: AI narration panel + "Narrate Journey" button
 *  - Bottom: API key entry (persisted locally via Preferences) + status bar
 */
public class MainFrame extends JFrame {

    private final DatabaseManager db;
    private Graph graph;

    private JComboBox<City> fromBox;
    private JComboBox<City> toBox;
    private JComboBox<GraphAlgorithms.Algorithm> algoBox;
    private JTextArea routeArea;
    private JTextArea narrationArea;
    private JTextField apiKeyField;
    private JTextField modelField;
    private JLabel statusLabel;
    private JButton findRouteButton;
    private JButton narrateButton;

    private RouteResult lastResult;

    private static final String PREF_API_KEY = "openrouter_api_key";
    private static final String PREF_MODEL = "openrouter_model";
    private final Preferences prefs = Preferences.userNodeForPackage(MainFrame.class);

    public MainFrame() {
        super("AI-Augmented Route Planner");
        db = new DatabaseManager("route_planner.db");

        try {
            db.connect();
            db.seedIfEmpty();
            loadGraph();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(),
                    "Startup Error", JOptionPane.ERROR_MESSAGE);
        }

        buildUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);
    }

    private void loadGraph() throws SQLException {
        List<City> cities = db.getAllCities();
        List<Edge> edges = db.getAllEdges();
        graph = Graph.build(cities, edges);
    }

    private void buildUI() {
        setLayout(new BorderLayout(8, 8));

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(new EmptyBorder(5, 10, 5, 10));

        List<City> cities = graph.getAllCities().stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();

        fromBox = new JComboBox<>(cities.toArray(new City[0]));
        toBox = new JComboBox<>(cities.toArray(new City[0]));
        if (cities.size() > 1) toBox.setSelectedIndex(1);

        algoBox = new JComboBox<>(GraphAlgorithms.Algorithm.values());

        findRouteButton = new JButton("Find Route");
        findRouteButton.addActionListener(e -> onFindRoute());

        panel.add(new JLabel("From:"));
        panel.add(fromBox);
        panel.add(new JLabel("To:"));
        panel.add(toBox);
        panel.add(new JLabel("Algorithm:"));
        panel.add(algoBox);
        panel.add(findRouteButton);

        JButton manageButton = new JButton("Manage Cities/Edges...");
        manageButton.addActionListener(e -> openManageDialog());
        panel.add(manageButton);

        return panel;
    }

    private JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 8));
        panel.setBorder(new EmptyBorder(0, 10, 0, 10));

        // Left: route result
        JPanel left = new JPanel(new BorderLayout(4, 4));
        left.setBorder(BorderFactory.createTitledBorder("Computed Route"));
        routeArea = new JTextArea();
        routeArea.setEditable(false);
        routeArea.setLineWrap(true);
        routeArea.setWrapStyleWord(true);
        routeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        left.add(new JScrollPane(routeArea), BorderLayout.CENTER);

        // Right: AI narration
        JPanel right = new JPanel(new BorderLayout(4, 4));
        right.setBorder(BorderFactory.createTitledBorder("AI Journey Narration"));
        narrationArea = new JTextArea();
        narrationArea.setEditable(false);
        narrationArea.setLineWrap(true);
        narrationArea.setWrapStyleWord(true);
        narrationArea.setFont(new Font(Font.SERIF, Font.PLAIN, 14));
        right.add(new JScrollPane(narrationArea), BorderLayout.CENTER);

        narrateButton = new JButton("Narrate Journey with AI");
        narrateButton.setEnabled(false);
        narrateButton.addActionListener(e -> onNarrate());
        right.add(narrateButton, BorderLayout.SOUTH);

        panel.add(left);
        panel.add(right);
        return panel;
    }

    private JPanel buildBottomPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBorder(new EmptyBorder(5, 10, 10, 10));

        JPanel keyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        keyPanel.add(new JLabel("OpenRouter API Key:"));
        apiKeyField = new JPasswordField(prefs.get(PREF_API_KEY, ""), 30);
        keyPanel.add(apiKeyField);

        keyPanel.add(new JLabel("Model:"));
        modelField = new JTextField(prefs.get(PREF_MODEL, "openai/gpt-4o-mini"), 20);
        keyPanel.add(modelField);

        JButton saveKeyButton = new JButton("Save");
        saveKeyButton.addActionListener(e -> {
            prefs.put(PREF_API_KEY, apiKeyField.getText().trim());
            prefs.put(PREF_MODEL, modelField.getText().trim());
            statusLabel.setText("API key and model saved locally.");
        });
        keyPanel.add(saveKeyButton);

        statusLabel = new JLabel("Ready.");
        statusLabel.setBorder(new EmptyBorder(4, 4, 4, 4));

        outer.add(keyPanel, BorderLayout.NORTH);
        outer.add(statusLabel, BorderLayout.SOUTH);
        return outer;
    }

    // -----------------------------------------------------------------
    private void onFindRoute() {
        City from = (City) fromBox.getSelectedItem();
        City to = (City) toBox.getSelectedItem();
        GraphAlgorithms.Algorithm algo = (GraphAlgorithms.Algorithm) algoBox.getSelectedItem();

        if (from == null || to == null) return;
        if (from.getId() == to.getId()) {
            JOptionPane.showMessageDialog(this, "Please choose two different cities.");
            return;
        }

        lastResult = GraphAlgorithms.findRoute(graph, from.getId(), to.getId(), algo);
        narrationArea.setText("");

        if (!lastResult.isFound()) {
            routeArea.setText("No route found between " + from.getName() + " and " + to.getName() + ".");
            narrateButton.setEnabled(false);
            statusLabel.setText("No path exists between selected cities.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Algorithm: ").append(lastResult.getAlgorithmUsed()).append("\n");
        sb.append("Nodes explored: ").append(lastResult.getNodesExplored()).append("\n");
        sb.append("Total distance: ").append(String.format("%.1f km", lastResult.getTotalWeight())).append("\n\n");
        sb.append("Path:\n");
        List<City> path = lastResult.getPath();
        for (int i = 0; i < path.size(); i++) {
            City c = path.get(i);
            sb.append("  ").append(i + 1).append(". ").append(c.getName());
            if (c.getLandmark() != null && !c.getLandmark().isBlank()) {
                sb.append("  [").append(c.getLandmark()).append("]");
            }
            sb.append("\n");
        }
        routeArea.setText(sb.toString());
        narrateButton.setEnabled(true);
        statusLabel.setText("Route computed with " + lastResult.getAlgorithmUsed() + ".");
    }

    private void onNarrate() {
        if (lastResult == null || !lastResult.isFound()) return;

        String apiKey = apiKeyField.getText().trim();
        if (apiKey.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter your OpenRouter API key at the bottom of the window and click Save.",
                    "API Key Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String model = modelField.getText().trim();

        narrateButton.setEnabled(false);
        statusLabel.setText("Contacting OpenRouter for narration...");
        narrationArea.setText("Generating narration...");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                OpenRouterClient client = new OpenRouterClient(apiKey, model);
                String system = NarrationPromptBuilder.systemPrompt();
                String user = NarrationPromptBuilder.userPrompt(lastResult);
                return client.narrate(system, user);
            }

            @Override
            protected void done() {
                narrateButton.setEnabled(true);
                try {
                    String narration = get();
                    narrationArea.setText(narration);
                    statusLabel.setText("Narration complete.");
                } catch (Exception ex) {
                    narrationArea.setText("");
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Failed to get narration:\n" + msg,
                            "OpenRouter Error", JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("Narration failed.");
                }
            }
        };
        worker.execute();
    }

    private void openManageDialog() {
        ManageDataDialog dialog = new ManageDataDialog(this, db);
        dialog.setVisible(true);
        // Reload graph and combo boxes after potential edits
        try {
            loadGraph();
            List<City> cities = graph.getAllCities().stream()
                    .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                    .toList();
            fromBox.setModel(new JComboBox<>(cities.toArray(new City[0])).getModel());
            toBox.setModel(new JComboBox<>(cities.toArray(new City[0])).getModel());
            statusLabel.setText("City/edge data reloaded.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to reload graph: " + e.getMessage());
        }
    }

    @Override
    public void dispose() {
        db.close();
        super.dispose();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
