# Orbital Foundry

A native Android rocket-building and orbital-flight sandbox, using *Spaceflight Simulator* as the primary gameplay reference and Juno: New Origins as a reference for deeper engineering systems. All code and visual assets in this project are original.

## v0.5 — SFS-style play loop and builder interaction
- Launch into the main menu rather than a mandatory tutorial; the short guide is optional.
- Rebuilt the home screen with a calmer blue/steel palette and explicit Build, Sandbox, Quick Launch and Career/Tech Tree entry points.
- Reworked the vehicle preview so rocket modules keep a consistent aspect ratio instead of being stretched across the screen.
- The builder supports dragging parts from the left rail to a chosen stack position, dragging installed parts to reorder, tapping parts to auto-attach, a stack editor, staging and direct launch.
- Sandbox unlocks every part without taking away access to career funds, contracts or technology progression.
- Kept the map's pinch zoom/pan and locally bundled Earth/Mars surface maps.
- The guide is optional and reduced to three short pages.

## Design baseline
The target is the recognizable 2D mobile rocket-building loop: a blue blueprint grid, narrow part rail, silver vertical component stack, launch at the top, compact engineering readouts and a minimal flight view. The implementation uses original shapes, rendering and code rather than extracting another game's assets or source.

## Next milestones
- [x] Native Android project
- [x] Rocket builder and stack reorder
- [x] Drag-and-drop part placement
- [x] Orbital flight prototype and telemetry
- [x] Career contracts and technology progression
- [x] Unrestricted sandbox entry
- [x] Pinch/pan system map and bundled surface maps
- [ ] True configurable stage editor
- [ ] Full 2D terrain and surface landings
- [ ] More accurate drag/re-entry and orbital maneuvers
- [ ] Blueprint save/load/import/export
- [ ] Improved spacecraft textures, plume animations and camera transitions
