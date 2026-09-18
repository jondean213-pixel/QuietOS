# QuietOS Development Registry

Owner: Jon Dean
Publisher: DeanWay Labs
Project: QuietOS
Phase: Alpha 0.1
Repository: jondean213-pixel/QuietOS

## Governing rule
Evidence comes before claims. No evidence means NOT VERIFIED.

## 2026-09-18 - Repository foundation
Status: IMPLEMENTED / NOT YET BUILD-VERIFIED

Decisions recorded:
1. QuietOS is a distinct product, not a GhostMode reskin.
2. GhostMode's verified behavior is architectural evidence and regression guidance only.
3. Alpha 0.1 targets one Android device and the Attention Engine.
4. Core attention classes are Emergency/Now, Important/Soon, Useful/Digest, Noise/Quiet.
5. Local model candidate is Qwen3 1.7B dynamic INT4 for LiteRT-LM; physical performance is NOT VERIFIED.
6. Assistant identity is configuration-driven. Personal build name: Qwen. Production default: QuietOS, user-renamable.
7. QuietOS Network is deferred until the single-device foundation is established.
8. Facial presence, TTS, and lip sync are planned modules but cannot block the functional Attention Engine.
9. Tool/action capabilities will use explicit contracts and permission policy rather than unrestricted model authority.

## Evidence consulted before foundation
- QuietOS and QuietOS Network - Project Definition
- DeanWay Absolutes canonical record dated 2026-09-16
- GhostMode 0.6.0-beta03 verified signed baseline
- GhostMode 0.6.0 Beta01 build plan
- GhostMode 0.5.0 end-to-end physical PASS
- GhostMode 0.5.0 build verification

## Next verification gate
Create the minimal native Android project, unit-test the deterministic attention-policy layer, and establish a reproducible CI debug build before claiming Alpha 0.1 builds.
