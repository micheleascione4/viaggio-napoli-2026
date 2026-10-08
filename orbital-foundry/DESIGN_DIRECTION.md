# Orbital Foundry — direction v0.2

## The target
A mobile aerospace sandbox between Spaceflight Simulator and Juno: New Origins.

### Spaceflight Simulator side
- immediate part snapping
- readable 2D/2.5D construction
- fast launch loop
- simple controls
- sandbox freedom

### Juno side
- career/company progression
- contracts and milestones
- tech tree
- richer vehicle analysis
- telemetry and map planning
- better atmosphere, exhaust, re-entry and impact presentation

## Core pillars

1. BUILD
   - snap parts vertically and radially
   - engines, tanks, capsules, fairings, fins, RCS, parachutes, landing legs, solar, probes
   - stage editor
   - symmetry
   - save/load vehicle

2. ANALYZE
   - mass
   - fuel
   - thrust
   - TWR
   - delta-v
   - estimated max altitude
   - stage-by-stage performance
   - warning when TWR is too low

3. FLY
   - physical 2D orbital simulation first
   - navball
   - throttle
   - SAS-like attitude modes
   - staging
   - time warp
   - predicted trajectory
   - atmospheric heating/re-entry
   - landing

4. EXPLORE
   - Earth
   - Moon
   - Mars
   - Phobos
   - later: Venus and Mercury
   - persistent spacecraft

5. CAREER
   - company funds
   - science/tech points
   - technology tree
   - contracts
   - milestones
   - launch history
   - unlock progressively instead of dumping every part on the player

6. VISUAL IDENTITY
   - dark aerospace UI
   - glass panels
   - luminous telemetry
   - high-quality exhaust particles
   - atmosphere gradient
   - orbital lines
   - cinematic launch camera
   - impact dust / water splash
   - clean mobile-first controls

## What we deliberately avoid
- copying game assets
- copying code
- copying UI layouts pixel-for-pixel
- requiring a desktop-like editor on a phone
- overwhelming new players with orbital mechanics immediately

## Gameplay loop

Contract -> design -> analyze -> launch -> fly -> achieve objective -> recover -> earn funds/tech -> unlock -> build better vehicle.

## Next implementation priorities

A. Replace the current simple part list with a visual stack editor.
B. Add stage groups and stage separation.
C. Add navball + SAS modes.
D. Add TWR/delta-v analysis.
E. Add career contracts and tech tree.
F. Upgrade flight visuals and trajectory map.
G. Expand celestial bodies.
