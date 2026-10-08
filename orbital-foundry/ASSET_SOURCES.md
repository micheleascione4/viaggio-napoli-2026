# Orbital Foundry — free art sources

Reviewed on 2026-10-08. The new in-game part illustrations are original Compose Canvas vector drawings included in the app code; this keeps the APK self-contained and avoids copying the look or UI of another game. These are the vetted sources for the next content pass:

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

## Later 3D models / real-world spacecraft references

- **NASA 3D Resources**: https://science.nasa.gov/3d-resources/
- **NASA 3D Resources repository**: https://github.com/nasa/NASA-3D-Resources
  - Free downloadable spacecraft, rockets, textures and planets are available, but follow NASA's media-usage guidance and the specific asset's notes. Do not assume every NASA logo, insignia or third-party item is automatically unrestricted.

## Integration policy

1. Prefer standalone PNG/SVG sources over sprite sheets when possible; if using a sheet, crop each part into a named asset and preserve its license file.
2. Normalize sizes and pivots so each part aligns to the rocket stack.
3. Optimize textures for mobile memory and include local assets in the APK; the game must not depend on network access for art.
4. Keep assets and UI original; references are for production quality and licensing, not for copying existing games.
