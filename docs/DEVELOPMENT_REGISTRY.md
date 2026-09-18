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


## 2026-09-18 - Alpha 0.1 native Android foundation

Status: SOURCE IMPLEMENTED / BUILD NOT YET VERIFIED

### Step 1 - Repository access verification
- Verified repository: `jondean213-pixel/QuietOS`.
- Verified visibility: private.
- Verified default branch: `main`.
- Verified connected permissions include pull, push, maintain, and admin.
- Repository was effectively empty of Android source before this increment.
- Existing project-definition documentation was preserved.

### Step 2 - GhostMode reference inspection
- Inspected the root of the private `jondean213-pixel/GhostMode-Android` repository.
- Confirmed preserved GhostMode source archives are present, including the 0.6.0 beta03 source archive.
- GhostMode remains a reference/proven baseline. No GhostMode source or signing identity was modified or copied into QuietOS during this increment.

### Step 3 - Android project foundation
Created:
- `settings.gradle.kts`
- root `build.gradle.kts`
- `gradle.properties`
- `app/build.gradle.kts`

Initial Android decisions:
- Namespace/application ID: `com.deanwaylabs.quietos`.
- Version code: 1.
- Version name: `0.1.0-alpha01`.
- Minimum SDK: 26.
- Target/compile SDK: 35.
- Kotlin Android application.
- Production signing is intentionally not configured in source.
- Gradle JVM ceiling initially set to 2048 MB for the build process. This is a build-host setting, not the Android runtime RAM budget.

Reason for package choice:
- QuietOS is a DeanWay Labs product and requires an identity separate from GhostMode.
- This decision must remain documented if changed later.

### Step 4 - Initial application shell
Created:
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/values/styles.xml`
- `app/src/main/java/com/deanwaylabs/quietos/MainActivity.kt`

The first activity intentionally contains only a minimal diagnostic shell. It identifies QuietOS Alpha 0.1, displays the configured assistant name, and indicates that the Attention Engine foundation is present. This is not final UI and is not evidence of a successful device build.

### Step 5 - Assistant identity abstraction
Created `AssistantProfile.kt`.

Behavior:
- Personal configuration returns assistant display name `Qwen`.
- Production configuration defaults to `QuietOS`.
- Production name accepts a user-selected name.
- Blank production names fall back to `QuietOS`.

Reason:
- The underlying local model and the consumer-facing assistant identity must remain separate.
- Jon Dean's personal build can remain Qwen without forcing that identity on commercial users.

### Step 6 - Deterministic Attention Engine seed
Created `attention/AttentionEngine.kt`.

Initial classifications:
- `NOW`
- `SOON`
- `DIGEST`
- `QUIET`

Initial deterministic behavior:
- Emergency source flag or an emergency term -> NOW.
- Direct request -> SOON.
- Promotional content -> QUIET.
- Otherwise -> DIGEST.

Important limitation:
- These rules are a testable seed, not the final classifier.
- Emergency handling is deliberately deterministic and does not depend solely on a generative model.
- Qwen is not integrated yet and no model-performance claim is made.

### Step 7 - Unit-test foundation
Created:
- `AttentionEngineTest.kt`
- `AssistantProfileTest.kt`

Tests authored:
1. Emergency event -> NOW.
2. Direct request -> SOON.
3. Promotion -> QUIET.
4. Ordinary event -> DIGEST.
5. Personal assistant identity -> Qwen.
6. Production assistant can be renamed.
7. Blank production name -> QuietOS.

Status:
- Tests are WRITTEN.
- Tests are NOT YET VERIFIED PASSING because no successful CI/local test execution evidence exists yet.

### Step 8 - CI/build definition
Created `.github/workflows/android-ci.yml`.

Intended CI sequence:
1. Check out source.
2. Install Java 17.
3. Configure Gradle 8.9.
4. Run `gradle testDebugUnitTest`.
5. Run `gradle assembleDebug`.
6. Upload `app-debug.apk` as artifact `QuietOS-alpha-debug`.

### Step 9 - Immediate CI evidence check
Queried GitHub Actions after workflow creation.

Observed result:
- Workflow run count returned 0 at the time checked.
- Therefore no test PASS, build PASS, or APK artifact is claimed.

Verification state:
- Repository writes: VERIFIED by GitHub commit responses.
- Android source existence: VERIFIED by repository writes.
- Unit tests passing: NOT VERIFIED.
- Android compilation: NOT VERIFIED.
- APK creation: NOT VERIFIED.
- Physical install: NOT VERIFIED.
- Qwen integration: NOT IMPLEMENTED.
- Qwen RAM behavior: NOT VERIFIED.
- Notification capture: NOT IMPLEMENTED in QuietOS yet.
- Physical Attention Engine behavior: NOT VERIFIED.

### RAM/model constraint recorded
QuietOS must be designed so the local model can be replaced without redesigning the application. Qwen3 1.7B dynamic INT4 remains the first candidate. Runtime testing must measure model load time, peak and sustained RAM pressure, response latency, process survival/background behavior, reload behavior, heat, and interaction with normal phone workloads. If Qwen is too heavy, the proven lightweight Gemma approach from GhostMode is the planned fallback through the same model-adapter boundary.

### Documentation discipline from this point forward
For every meaningful implementation increment, this registry will record:
1. Starting state/evidence.
2. Files inspected.
3. Files created, changed, or removed.
4. Reason for each architecture decision.
5. Expected behavior.
6. Tests added or changed.
7. Actual test/build evidence.
8. Failures and exact symptoms.
9. Fix applied and why.
10. Regression result.
11. Remaining unverified claims.
12. Version/build identifier when applicable.

No future QuietOS milestone is considered complete merely because source was written.
