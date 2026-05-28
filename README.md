# Ranger Survivor

A recreated top-down tile-based survival shooter. Move with WASD, shoot arrows with the arrow keys, dodge enemies that spawn faster and faster. A college-project recreation, this time rebuilt as a browser game on an HTML5 Canvas with a Spring Boot backend for leaderboards.

## Architecture

Two pieces.

**Frontend** is a single-page HTML5 Canvas game in plain JavaScript. Served as static files from the Spring Boot backend at `/`.

- Game loop driven by `requestAnimationFrame`.
- Tile grid stored as a 2D array, rendered every frame from a sprite sheet.
- Player movement is grid-snapped on WASD keydown.
- Arrow projectiles fire in a straight line on UP / LEFT / DOWN / RIGHT keydown.
- Enemies spawn at fixed spawn points and use A* on the tile grid to path toward the player.
- Collision check between enemy and player ends the game. Collision between arrow and enemy kills the enemy.
- Kill counter ticks up in a top bar. Spawn cadence accelerates over time.

**Backend** is a Spring Boot REST API for high scores. Three endpoints.

- `POST /api/scores` accepts `{ name, kills, durationSeconds }`, persists.
- `GET /api/scores/top?limit=10` returns the top-N scores by kills.
- `GET /api/scores/me?name=...` returns this player's recent runs.

H2 in-memory database for local development. Swap to Postgres for a real deployment.

## Quick start

Requires Java 17+ and Maven.

```bash
cd ranger-survivor
mvn spring-boot:run
# open http://localhost:8080 in a browser
```

The H2 console is at `http://localhost:8080/h2-console` (jdbc url `jdbc:h2:mem:rangersurvivor`, user `sa`, no password).

## Project structure

```
ranger-survivor/
├── README.md
├── pom.xml
├── src/main/java/com/rangersurvivor/
│   ├── RangerSurvivorApplication.java
│   ├── controller/ScoreController.java
│   ├── model/Score.java
│   └── repository/ScoreRepository.java
├── src/main/resources/
│   ├── application.yml
│   └── static/
│       ├── index.html
│       ├── game.js
│       ├── styles.css
│       └── sprites/          # drop the sprite pack here
└── src/test/java/com/rangersurvivor/
    └── ScoreControllerTest.java
```
