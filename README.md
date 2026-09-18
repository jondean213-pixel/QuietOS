# QuietOS by DeanWay Labs

QuietOS is a local-first Android attention intelligence and personal-assistant project.

## Alpha 0.1 objective
Prove contextual attention management on one Android device:

Notification -> capture -> analyze -> classify -> delivery decision -> explainable record.

Attention classes:
- Emergency -> Now
- Important -> Soon
- Useful -> Digest
- Noise -> Quiet

## Product identity
QuietOS is the production product name. Assistant display identity is configurable. Jon Dean's personal build uses **Qwen**.

## Local AI
Current physical-test candidate: `Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm` via LiteRT-LM. It is not considered verified until benchmarked on the target Android device.

## Engineering rules
- Local-first.
- Evidence before claims.
- Every meaningful change and test is recorded.
- GhostMode is a reference baseline, not a source tree to overwrite.
- QuietOS has its own package, history, signing identity, UI, tests, and release lifecycle.
- QuietOS Network/Pro must not block Alpha 0.1.

See `docs/DEVELOPMENT_REGISTRY.md` and `docs/ARCHITECTURE.md`.
