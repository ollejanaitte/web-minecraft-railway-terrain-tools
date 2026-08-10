# Phase 0.2 GPU / CPU Investigation

## Hardware
- CPU: 12 cores.
- GPU0: NVIDIA GeForce GTX 1050 Mobile (GP107M), driver 580.173.02, CUDA 13.0.
- GPU1: Intel CoffeeLake UHD 630 (iGPU).
- `/dev/dri/card0` (nvidia), `card1` (intel), `renderD128/129` with ACL
  `user:masaharu:rw` (direct device access available).
- Vulkan ICDs: `nvidia_icd.json`, `intel_icd`, `intel_hasvk`, `lvp` (sw).
- glvnd: `10_nvidia.json`, `50_mesa.json`.
- Chrome 151.0.7922.71. Node v22. Xvfb available (software GL only).

## SwiftShader high-CPU root cause (CONFIRMED)
Phase 0.1 left **4 stale headless Chrome instances** running (profiles
`chrome-profile-fix5..fix8`). Each SwiftShader `gpu-process` pegs several
cores:
- Worst instance: **658% CPU**, **46h CPU time in ~5.5h wall clock** (~8 cores).
- Chrome total RSS ~6.8 GB across the 4 trees.
- System load average **~16-19** on 12 cores; game page hung (CDP eval timeout
  = the known SwiftShader freeze).
- After SIGTERM of the 4 project Chrome trees: load dropped to ~3.4 then ~1.1.

=> Sustained CPU saturation = multiple long-running SwiftShader instances left
behind; even one instance burns several cores while rendering.

## Hardware GPU path (STANDARD)
```
--headless=new --use-gl=angle --use-angle=vulkan
```
- WebGL renderer: `ANGLE (NVIDIA, Vulkan 1.4.312 (NVIDIA GeForce GTX 1050))`
- WebGL2 OK; `navigator` main loop ~60 fps; `SystemInfo` selected adapter:
  `NVIDIA GeForce GTX 1050, backend=Vulkan, adapterType=Discrete GPU`.
- Game boots to title, menus, world create, world join — all working on
  hardware GPU, no freeze.
- CPU at menu: browser ~1-4% + renderer ~5-8% ≈ **~9% total**; in-world with
  train+chunks: renderer ~66% (still far below SwiftShader's 658%).
- `nvidia-smi`: 25-37% GPU util, ~144-190 MiB VRAM.

## SwiftShader (FALLBACK)
```
--headless=new --no-sandbox --enable-unsafe-swiftshader --use-gl=angle
--use-angle=swiftshader
```
- WebGL renderer: `ANGLE (Google, Vulkan 1.3.0 (SwiftShader Device (Subzero)))`.
- Use only when hardware Vulkan is unavailable (e.g. VM without GPU/ACL).
- Mitigations: single instance, short-lived runs, strict stale-process cleanup.

## Comparison
| Metric | SwiftShader | Hardware Vulkan |
|---|---|---|
| Boot/title | ~3-4 min, freeze risk | ~1-2 min, responsive |
| WebGL renderer | software (Subzero) | NVIDIA GTX 1050 |
| Menu CPU | 100%+ per instance | ~9% total |
| In-world CPU | 300-658% | ~66% renderer |
| CDP/screenshot | hangs under load | reliable |
| GPU util | 0% | 25-37% |

## Recommendation
- **Standard = Hardware GPU via ANGLE-Vulkan** for both run-game.sh and
  run-validation.sh.
- SwiftShader is the documented fallback only.
- Enforce stale-process cleanup after every run.
