# Phase 0.1 Known Issues (INTERMEDIATE)

| ID | Severity | Reproduction | Expected | Actual | Architecture impact | Owner | Blocker |
|----|----------|--------------|----------|--------|----------------------|-------|---------|
| P01-CHAT | HIGH | Headless game; chat key/type via JS events | /railsysv2 command executes | Command not confirmed (no [RAILSYSTEM] log); JS key events don't submit chat | None (test-harness only); bypassed by auto-validate hook | Cursor | YES (for screenshot capture) |
| P02-FREEZE | HIGH | After heavy chunk render under SwiftShader | Game responsive | Main thread saturates; evals/screenshots hang; textures can break to gray/white | None (env) | Cursor | YES |
| P03-NAV | MEDIUM | Select World entry click | Reliable world load | Entry position varies between boots; world lost when profile reset | None (test-harness) | Cursor | YES (blocked final run) |
| P04-VISION | HIGH | Any screenshot | Agent visually confirms | Model cannot ingest images; pixel-analysis only | None | User | YES (for PASS) |
| P05-NO_SHOTS | HIGH | Required screenshots | SS-01..SS-07 | Only SS-01-style title/in-world evidence; SS-02..07 missing | None | Cursor | YES (for PASS) |
| P06-TIME | INFO | World load slow | fast | ~2-3 min per world under SwiftShader | perf input for Phase 1 | - | NO |

## Auto-validate hook
`RailV2AutoValidate.onServerTick(MinecraftServer)` (hooked in
MinecraftServer.tick) runs once: places the course, spawns 4 cars, starts
them (speed 0.12), teleports the camera to the track. It bypasses the chat
blocker. It was added and built, but not observed firing (final world-load
run not completed).
