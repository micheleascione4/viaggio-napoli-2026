# Orbital Foundry — free art sources

Reviewed on 2026-10-09. Orbital Foundry uses original Compose Canvas part illustrations and downloads the following surface maps during the Android CI build, then bundles them in the APK so the app needs no network while playing. Menu proportions and control layout are inspired by genre conventions, not copied from another game's artwork or code:

## 2D parts, rockets, satellites and effects

- **Kenney — Space Shooter Extension**: https://kenney.nl/assets/space-shooter-extension
  - Official pack page marks the 270+ assets as Creative Commons CC0.
  - Useful categories: rocket/ship parts, missiles, satellite modules, projectiles and space effects.
  - Attribution is not required by CC0, but credit is appreciated. Keep the included license text with any imported pack files.
- **Kenney — Space Shooter Remastered**: https://kenney.nl/assets/space-shooter-remastered
  - Official pack page marks the pack as CC0.
  - Useful for consistent alternate spacecraft silhouettes and effects.
- **Kenney — Planets**: https://kenney.nl/assets/planets
  - Official Kenney page for 2D planet art; check the pack's displayed license when integrating.

## Fonts

- **Kenney — Fonts**: https://kenney.nl/assets/kenney-fonts
  - Use the display family such as “Rocket” for headings only; keep telemetry and dense controls highly legible.
  - Confirm the pack's CC0 notice and retain the included license in imported assets.

## Planet surface maps bundled in the APK

- **Earth — NASA Blue Marble, land/ocean/ice, 2048 px**  
  Source: https://svs.gsfc.nasa.gov/vis/a000000/a002900/a002915/  
  Build URL: https://svs.gsfc.nasa.gov/vis/a000000/a002900/a002915/bluemarble-2048.png  
  NASA imagery is generally available for factual/educational/game-simulation use subject to NASA media rules. Do not use NASA logos or imply endorsement. Read the current guidelines: https://www.nasa.gov/nasa-brand-center/images-and-media/
- **Mars — JPL Solar System Simulator / USGS Viking map**  
  Source page: https://space.jpl.nasa.gov/tmaps/mars.html  
  Build URL: https://space.jpl.nasa.gov/tmaps/pix/mar0kuu2.jpg  
  The texture table attributes this map to Viking / Caltech-JPL-USGS. Retain that attribution in project documentation; it is a game visual map, not a scientific dataset.
- **World map art — Kenney Planets (CC0 1.0)**  
  https://kenney-assets.itch.io/planets  
  Useful for future sprite variants; its visible license states CC0 1.0 Universal.

## Later 3D models / real-world spacecraft references

- **NASA 3D Resources**: https://science.nasa.gov/3d-resources/
- **NASA 3D Resources repository**: https://github.com/nasa/NASA-3D-Resources
  - Free downloadable spacecraft, rockets, textures and planets are available, but follow NASA's media-usage guidance and the specific asset's notes. Do not assume every NASA logo, insignia or third-party item is automatically unrestricted.

## Integration policy

1. Prefer standalone PNG/SVG sources over sprite sheets when possible; if using a sheet, crop each part into a named asset and preserve its license file.
2. Normalize sizes and pivots so each part aligns to the rocket stack.
3. Download the Earth and Mars texture maps in `.github/workflows/build-orbital-foundry.yml` and bundle them under `app/src/main/assets/textures/`; the game must not depend on network access while playing.
4. Keep assets and UI original; references are for production quality and licensing, not for copying existing games.
