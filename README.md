# Orvitary — Orbital Foundry

Native Android rocket-building and orbital-flight sandbox.

**Primary reference:** Spaceflight Simulator  
**Deeper simulation inspiration:** Juno: New Origins  
**Current development branch:** `orbital-foundry-sfs-rebuild-v0-5`

## Current project

The Android game lives in `orbital-foundry/`.

- Rocket builder with draggable part placement and stack reordering
- Vehicle analysis: mass, thrust-to-weight ratio and delta-v
- Sandbox mode alongside career progression and technology unlocks
- Orbital flight prototype, telemetry and system map
- Optional short tutorial
- Automated physics tests and debug APK build through GitHub Actions

## Build

The GitHub Actions workflow **Build Orbital Foundry APK** runs unit tests and assembles a debug APK from `orbital-foundry/`.

To build locally with Java 17 and Gradle 8.9:

```bash
gradle --no-daemon -p orbital-foundry :app:testDebugUnitTest
gradle --no-daemon -p orbital-foundry :app:assembleDebug
```

## Current priorities

1. Make the rocket builder reliable and comfortable to use on mobile.
2. Improve part snapping, stack editing and configurable staging.
3. Refine the aerospace UI and onboarding.
4. Continue improving flight physics, terrain and blueprint save/load.

All code and visual assets are original; the reference games inform gameplay goals, not copied source or extracted assets.
