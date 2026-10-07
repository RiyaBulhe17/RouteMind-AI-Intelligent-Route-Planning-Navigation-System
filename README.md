# AI-Augmented Route Planner

A Java Swing desktop app that:
1. Stores a city/road network in a **SQLite-backed SQL table** (`cities`, `edges`).
2. Computes the shortest/best route between two cities using your choice of
   **BFS**, **DFS**, **Dijkstra** (optimal weighted shortest path), or
   **Greedy Best-First Search**.
3. Sends the computed route — including each city's landmark data — to an
   **AI model via OpenRouter** to generate a narrated description of the journey.

## Project layout

```
route-planner/
├── pom.xml
└── src/main/java/com/routeplanner/
    ├── model/            City.java, Edge.java
    ├── db/               DatabaseManager.java   (SQLite schema + queries)
    ├── graph/             Graph.java, GraphAlgorithms.java, RouteResult.java
    ├── ai/               OpenRouterClient.java, NarrationPromptBuilder.java
    └── ui/               MainFrame.java, ManageDataDialog.java
```

## Requirements

- Java 17+ (JDK)
- Maven 3.6+
- An OpenRouter API key: https://openrouter.ai/keys (free to sign up; pick any
  model, e.g. `openai/gpt-4o-mini`, `anthropic/claude-3.5-haiku`, `meta-llama/llama-3.1-8b-instruct`, etc.)

This build was prepared without live internet access, so the Maven
dependencies (`sqlite-jdbc`, `org.json`) have **not** been downloaded or
compiled here. The first `mvn` command below will fetch them from Maven
Central automatically — just run it on a machine with normal internet access.

## Build

```bash
cd route-planner
mvn clean package
```

This produces a single runnable "fat jar" at:

```
target/ai-route-planner.jar
```

## Run

```bash
java -jar target/ai-route-planner.jar
```

A window opens with:
- **From / To** city dropdowns and an **Algorithm** selector (BFS/DFS/Dijkstra/Greedy)
- **Find Route** — computes the path and shows the stop-by-stop route with distance
- **Manage Cities/Edges...** — add new cities (name, lat/lon, landmark text) and
  roads (edges) directly into the SQLite database from the UI
- At the bottom: paste your **OpenRouter API key** and (optionally) change the
  **model** string, then click **Save** (stored locally via Java Preferences,
  never written to source/version control)
- **Narrate Journey with AI** — sends the route + landmark data to OpenRouter
  and displays the AI's narrated story of the trip in the right-hand panel

## Database

On first run, `route_planner.db` (SQLite file) is created in the working
directory and seeded with a small demo network of Indian cities, each
carrying a `landmark` field (e.g. Agra → Taj Mahal). You can add more
cities/edges via the **Manage Cities/Edges** dialog, or by editing the DB
directly with any SQLite client.

Schema:

```sql
CREATE TABLE cities (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    lat REAL NOT NULL,
    lon REAL NOT NULL,
    landmark TEXT
);

CREATE TABLE edges (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    from_city_id INTEGER NOT NULL,
    to_city_id INTEGER NOT NULL,
    weight REAL NOT NULL,
    FOREIGN KEY (from_city_id) REFERENCES cities(id),
    FOREIGN KEY (to_city_id) REFERENCES cities(id)
);
```

## Algorithms

| Algorithm | What it optimizes | Notes |
|---|---|---|
| BFS | Fewest hops | Ignores edge weight |
| DFS | Any reachable path | Not optimal, explores depth-first |
| Dijkstra | True minimum total distance | The "optimal route" |
| Greedy Best-First | Heuristic (straight-line/haversine distance to goal) | Fast but not guaranteed optimal |

## Notes on the OpenRouter integration

- `OpenRouterClient` calls `POST https://openrouter.ai/api/v1/chat/completions`
  with your key as a Bearer token, using Java's built-in `java.net.http.HttpClient`
  (no extra HTTP dependency needed).
- The prompt (`NarrationPromptBuilder`) explicitly instructs the model not to
  invent landmarks beyond what's in the database, and to reference real leg
  distances from the computed route.
- The API call runs on a `SwingWorker` background thread so the UI never freezes.
- If you don't have an API key yet, you can still use every other feature
  (route computation, algorithm comparison, DB management) — only the
  "Narrate Journey" button requires it.

## Troubleshooting

- **"OpenRouter API error (HTTP 401)"** — invalid/missing API key.
- **"OpenRouter API error (HTTP 402)"** — insufficient credits/free-tier limit
  on the selected model; try a free/cheaper model string.
- **Build fails resolving dependencies** — ensure you have internet access to
  Maven Central (`repo1.maven.org`) when you run `mvn clean package`.
