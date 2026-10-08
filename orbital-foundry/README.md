# Orvitary — Orbital Foundry

Native Android rocket-building and orbital-flight sandbox. *Spaceflight Simulator* is the primary gameplay reference; deeper engineering systems draw inspiration from vehicle-design simulators. Code, artwork and UI components are original.

## v0.6 — mobile workshop and navigation rebuild

- Persistent bottom navigation gives direct access to **Home**, **Build**, **Career** and **Map**. Map becomes available after a flight state exists; flight controls remain full-screen.
- The home screen is a mission-control deck with a persistent vehicle preview, launch readiness, mass, propellant, delta-v, career funds and science.
- The builder uses a wide central blueprint canvas instead of narrow side rails. Components live in a horizontally scrollable inventory with **Structure**, **Propulsion**, **Control** and **Utility** categories.
- Tap a component to attach it or drag it onto the canvas to choose an insertion point. Drag installed parts to reorder; drag an empty canvas area to pan long vehicles vertically.
- Dedicated **Stack**, **Stages**, **Designs** and **Analyze** panels support detailed edits, staging, local blueprint save/load/delete and vehicle checks.
- Sandbox unlocks all components without removing access to career funds, contracts or technology progression.
- The guide is optional and matches the workshop controls.

## Build and verification

The **Build Orbital Foundry APK** GitHub Actions workflow runs physics unit tests, builds the Android debug APK and uploads it as `orbital-foundry-debug`. Build with Java 17 and Gradle 8.9:

```bash
gradle --no-daemon -p orbital-foundry :app:testDebugUnitTest
gradle --no-daemon -p orbital-foundry :app:assembleDebug
```

Planet textures are downloaded from the configured sources during CI. The project does not include code or extracted assets from Spaceflight Simulator.

## Next development targets

- [x] Mobile navigation and mission-control home
- [x] Wide workshop canvas and categorized component tray
- [x] Drag-to-insert and drag-to-reorder interactions
- [x] Vertical panning for tall rocket stacks
- [x] Stage editor and saved blueprint library
- [x] Vehicle analysis panel
- [x] Career contracts and technology progression
- [ ] Validate every staging pattern against flight physics
- [ ] Surface terrain, landing legs and ground interaction
- [ ] Persistent spacecraft and transfer maneuvers