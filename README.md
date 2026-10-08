# Orvitary — Orbital Foundry

Native Android rocket-building and orbital-flight sandbox. The primary gameplay reference is *Spaceflight Simulator*: fast 2D rocket assembly, simple launch controls and a direct design-to-flight loop. Deeper engineering systems draw inspiration from rocket design simulators. Code, artwork and interface elements are original.

Development branch: `orbital-foundry-sfs-rebuild-v0-5`.

The Android project lives in `orbital-foundry/`. Its current interface has persistent Home / Build / Career / Map navigation, a mission-control home screen, and a wide workshop canvas with categorized horizontal parts inventory. Parts can be tapped to attach or dragged onto the stack; long vehicles can be panned vertically. The workshop includes stage editing, local blueprint save/load/delete, vehicle analysis, career progression and sandbox mode.

Run physics unit tests and build a debug APK with Java 17 and Gradle 8.9:

```bash
gradle --no-daemon -p orbital-foundry :app:testDebugUnitTest
gradle --no-daemon -p orbital-foundry :app:assembleDebug
```

GitHub Actions runs the tests, assembles the debug APK and publishes the `orbital-foundry-debug` artifact.