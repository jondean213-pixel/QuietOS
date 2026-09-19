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


## 2026-09-18 - CI failure 001: JVM target mismatch

Evidence: GitHub Actions reached Android compilation and failed at app compileDebugKotlin because Java targeted JVM 1.8 while Kotlin targeted JVM 17. Unit tests could not complete; APK build and artifact upload were skipped.

Root cause: app/build.gradle.kts did not explicitly align Java compile compatibility with Kotlin JVM 17.

Fix: Java source and target compatibility were set to Java 17, and Kotlin jvmTarget was explicitly set to 17. Fix commit: a7b1ed0310e6934584bfda127d3f5866352831b2.

Verification after fix: root cause VERIFIED from CI logs; source fix VERIFIED committed; post-fix tests and APK remain PENDING CI; physical-device behavior remains NOT VERIFIED.


## 2026-09-18 - Alpha 0.1 local-model runtime adapter begins

Status: SOURCE IMPLEMENTED / CI AND PHYSICAL RUNTIME VERIFICATION PENDING

### Scope gate
Alpha 0.1 is deliberately narrow. The next acceptance question is: can QuietOS run a useful local conversational AI reliably on the target Motorola without unacceptable RAM pressure?

Order is locked:
1. Local Qwen runtime.
2. Motorola physical load/conversation test.
3. Measure load time, RAM pressure, response latency, sustained stability, repeat/reload behavior and heat.
4. Only after the runtime passes, layer the Attention Engine around it.

Network/Pro, face/lip-sync, contextual visual presentation and large tool ecosystems remain outside this Alpha gate.

### Previous CI evidence now recorded
GitHub Actions runs for commits a7b1ed0310e6934584bfda127d3f5866352831b2 and bc507e4db58995c2a46e0e8b9911ca6e29d5dee9 completed successfully. Unit tests, debug APK assembly and artifact upload passed. This verifies the JVM-17 build fix. It does not verify physical-device behavior or Qwen runtime behavior.

### Runtime architecture added
Created:
- `ai/LocalModel.kt`
- `ai/LiteRtQwenModel.kt`

The LocalModel interface is the replaceable model boundary. Qwen is the first runtime candidate; the proven lightweight Gemma approach remains the fallback without redesigning QuietOS.

LiteRtQwenModel:
- uses the official LiteRT-LM Kotlin Engine API;
- initializes on Dispatchers.IO so model loading does not block the Android UI thread;
- starts with CPU backend for the first controlled baseline;
- records model load time;
- exposes explicit UNLOADED/LOADING/READY/ERROR state;
- creates a conversation and streams response chunks;
- explicitly closes the engine so native model resources can be released.

Added the LiteRT-LM Android Maven runtime and Android coroutines dependency to the app module.

### RAM rule
Qwen must not be treated as an always-needed OS service. QuietOS should handle deterministic work itself and invoke model reasoning only when understanding is required. The model boundary must support unloading/replacement. User reports the Qwen model file already on the Motorola is approximately 977 MB; that is user-supplied file-size evidence, not runtime-RAM evidence.

### Product-development rule
DeanWay Labs development follows: concept -> prototype -> personal test -> keep/modify/cut -> product candidate. Experimental features are allowed during personal testing, but they do not become product requirements without evidence of value and acceptable performance/complexity cost.

### Future requirements preserved but NOT Alpha 0.1
- Contextual Visual Presentation: relevant cards/images synchronized with spoken responses.
- Qwen personal personality: familiar, sarcastic and playful in companion context; professional during work; direct and serious in critical contexts.
- Personality never overrides permissions. Qwen may be rebellious in conversation, not in authorization.
- Commercial assistant identity remains configurable and separate from Jon's personal Qwen profile.

### Verification state
- Model abstraction source: IMPLEMENTED.
- LiteRT-LM adapter source: IMPLEMENTED.
- Dependency resolution/build after runtime addition: PENDING CI.
- Model selection from the existing Motorola file: NOT YET IMPLEMENTED.
- Qwen model initialization on Motorola: NOT VERIFIED.
- Qwen conversation on Motorola: NOT VERIFIED.
- Runtime RAM/load time/latency/stability/heat: NOT VERIFIED.


## 2026-09-18 - Alpha 0.1 Qwen picker, CI recovery, and green build

Status: SOURCE -> UNIT TESTS -> DEBUG APK -> ARTIFACT = VERIFIED PASS / QWEN PHYSICAL RUNTIME = NOT VERIFIED

### Starting state and objective
The previously verified diagnostic QuietOS shell could install and launch on the target Motorola, but it only displayed the configured assistant identity `Qwen`. It did not select, load, or run the approximately 977 MB user-supplied Qwen LiteRT-LM model. The next Alpha 0.1 objective was therefore narrowed to a functional model-selection/load/conversation path and a reproducible green CI build.

### Physical diagnostic-shell evidence
The earlier diagnostic APK was physically installed and launched on Jon Dean's Motorola. The observed screen rendered:
- `QuietOS Alpha 0.1`
- `Assistant: Qwen`
- `Attention Engine: foundation ready`

Physical evidence classification:
- APK install: PASS.
- App launch: PASS.
- Diagnostic shell rendering: PASS.
- AssistantProfile personal display name `Qwen`: PASS.
- Qwen model selection/load/inference: NOT VERIFIED.
- Qwen runtime RAM/load time/latency/stability: NOT VERIFIED.
- Attention Engine runtime behavior on this build: NOT VERIFIED.

Process failure recorded: an earlier GitHub Actions artifact ZIP was initially described as though it were the raw APK. That was misleading and caused Android to offer inappropriate handlers such as Termux/Google. Corrective rule: an Actions artifact is a ZIP archive unless the actual APK has been extracted and verified. Future physical-test handoffs must provide the actual APK and must state only the test objective that build can perform.

### Functional Alpha model-test UI implemented
Commit `002478f535c54bd16065a9637fa0666148e63d45`
Message: `Alpha 0.1: add Qwen model picker and conversation test UI`

Changed `MainActivity.kt` from the diagnostic-only shell to a deliberately minimal functional test interface:
- Android OpenDocument model picker.
- Import of the selected model into app-private `filesDir/models`.
- Model status reporting for import, load, READY and failure.
- Calls to `LiteRtQwenModel.load()`.
- Basic prompt field, Send button and transcript.
- Calls to `LiteRtQwenModel.send()`.
- Full-response elapsed-time display.
- Model close during Activity destruction.
- No generated graphics and no nonessential presentation work.

Storage tradeoff: the current implementation copies the selected model into app-private storage because the adapter currently supplies a filesystem path to LiteRT-LM. With the reported model size of approximately 977 MB, this can consume roughly another model-sized allocation of storage. This is accepted only as an Alpha proof path and must be reassessed after runtime proof. Reinstall/removal of app data can remove the private copy.

Known implementation limitations preserved for follow-up:
- Current model file copy is performed from a lifecycle coroutine without an explicit `Dispatchers.IO` wrapper and should be moved off the main dispatcher before physical stress testing.
- `takePersistableUriPermission` should be removed or safely guarded because immediate private copying makes persistent URI access unnecessary and not every provider guarantees that grant.
- Selected model validation is minimal.
- Send-button READY comparison currently uses the enum name string rather than direct `ModelState.READY`.
- The adapter creates a new LiteRT-LM conversation for each send, so conversational history/multi-turn model context is not yet proven.
- `onDestroy` model close may require lifecycle/race hardening if load or inference is active.
These limitations do not invalidate the CI build result, but they remain unverified physical-runtime risks and must not be hidden by the green build.

### Android lifecycle support
Commit `c5e6f050063b382f5b11bd0917191dd4e808837d`
Message: `Alpha 0.1: add activity lifecycle support for Qwen loader`

Added:
- `androidx.activity:activity-ktx:1.10.1`
- `androidx.lifecycle:lifecycle-runtime-ktx:2.8.7`

Purpose: support the Activity result contract and lifecycle-scoped coroutine execution used by the model picker/load test path.

### CI failure 002 - LiteRT-LM/Kotlin metadata incompatibility
GitHub Actions Run #10:
- Run ID: `35383930880`
- Head SHA: `c5e6f050063b382f5b11bd0917191dd4e808837d`
- Conclusion: FAILURE.
- Failed step: Unit tests / Kotlin compilation.
- Debug APK: SKIPPED.
- Artifact upload: SKIPPED.

Exact root evidence from CI: Maven resolved LiteRT-LM 0.17.1, whose Kotlin metadata was 2.4.0, while the project Kotlin compiler was 2.0.21 and expected metadata 2.0.0. The compiler therefore rejected LiteRT-LM and associated Kotlin 2.4-era metadata before the Qwen source could be meaningfully compiled.

Fix commit `bff3ddc573c04cabb1d6a53ef1050e651076cee5`
Message: `Fix: align Kotlin compiler with LiteRT-LM 0.17.1`
- Root Kotlin Android plugin changed from 2.0.21 to 2.4.0.

Fix commit `c760c34ed8e097de64a831c198bf64ee2371add6`
Message: `Fix: pin LiteRT-LM runtime for reproducible Alpha build`
- Replaced `litertlm-android:latest.release` with explicit `litertlm-android:0.17.1`.
- Reason: reproducible builds must not silently change runtime versions when a new Maven release appears.

### CI failure 003 - Kotlin 2.4 JVM-target DSL migration
GitHub Actions Run #12:
- Run ID: `35389772204`
- Head SHA: `c760c34ed8e097de64a831c198bf64ee2371add6`
- Conclusion: FAILURE.
- Failed step: Unit tests / build-script compilation.
- Debug APK: SKIPPED.
- Artifact upload: SKIPPED.

Exact CI symptom:
`Using 'jvmTarget: String' is an error. Please migrate to the compilerOptions DSL.`

Root cause: upgrading the Kotlin Gradle plugin to 2.4.0 solved the LiteRT-LM metadata incompatibility but made the older `kotlinOptions { jvmTarget = "17" }` syntax invalid.

Fix commit `2ecbd3fcb97ac43ecfb2d2c3e528e5cb5644f22b`
Message: `Fix: migrate Kotlin JVM target to compilerOptions DSL`

Changed `app/build.gradle.kts`:
- imported `org.jetbrains.kotlin.gradle.dsl.JvmTarget`;
- removed the obsolete `kotlinOptions` block;
- added `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }`;
- retained Java source/target compatibility at Java 17.

### CI verification - Run #13 GREEN
GitHub Actions Run #13:
- Run ID: `35395321316`
- Head SHA: `2ecbd3fcb97ac43ecfb2d2c3e528e5cb5644f22b`
- Workflow: `QuietOS Android CI`
- Conclusion: SUCCESS.

Verified successful job steps:
1. Job setup: PASS.
2. Checkout: PASS.
3. Java setup: PASS.
4. Gradle setup: PASS.
5. Unit tests: PASS.
6. Debug APK assembly: PASS.
7. Artifact upload: PASS.
8. Job completion: PASS.

Run #13 artifact evidence:
- Artifact name: `QuietOS-alpha-debug`
- Artifact ID: `10568060072`
- Archive size: 25,944,111 bytes.
- SHA-256 artifact digest: `924a375f2b0148a2de63ed7e1229044906502f66803d888a3cb0a94ebded04cf`.
- Created: 2026-09-18T21:12:08Z.
- Expiration: 2026-12-17T21:09:43Z.
- Expired at verification time: false.

### Regression and evidence boundary
Run #13 establishes:
- Current source compiles under the aligned Kotlin/LiteRT-LM toolchain: PASS.
- Existing unit tests: PASS.
- Debug APK assembly: PASS.
- GitHub artifact creation/upload: PASS.
- Reproducible LiteRT-LM dependency is pinned to 0.17.1: VERIFIED in source.

Run #13 does NOT establish:
- APK installation/update compatibility on the target Motorola.
- Qwen model selection from the Motorola.
- Successful import of the approximately 977 MB model.
- LiteRT-LM engine initialization on the Motorola.
- First local Qwen response.
- Multi-turn conversation continuity.
- Runtime RAM usage, load time, inference latency, sustained stability, heat, background survival, lock/unlock behavior, app-switch behavior, process-kill/reload behavior.
- Attention Engine integration with the live Qwen runtime.

All items above remain NOT VERIFIED until physical evidence is collected.

### Next Alpha 0.1 gate
Before expanding QuietOS scope:
1. Harden the model import path for background I/O and URI-provider behavior.
2. Preserve the green CI baseline after those changes.
3. Extract and verify the actual APK from the successful Actions artifact before physical handoff.
4. Install/update on the Motorola.
5. Select the existing Qwen LiteRT-LM model.
6. Verify Qwen reaches READY and record actual load time.
7. Send a local prompt and record the first successful response and latency.
8. Measure RAM and stability under repeated use, app switching, lock/unlock and reload.
9. Record failures exactly and fix/regression-test them.
10. Only after the local runtime is physically proven begin layering the Attention Engine around Qwen.

Alpha 0.1 remains intentionally protected from Network/Pro, face/lip-sync, contextual visual presentation, and broad tool integration until this gate is passed.


## 2026-09-18 - Motorola physical Qwen test failure and corrective increment

### Physical evidence from Jon
Build from CI Run #15 installed and launched on the target Motorola. Two user-visible failures were reported:
1. Every app launch required selecting/importing the Qwen model again.
2. After Qwen was selected/loaded, sending a message produced no visible reply.

This is a physical-runtime FAIL for conversational Qwen operation. It does not invalidate Run #15's compile/unit-test/APK evidence.

### Root cause confirmed for repeated model selection
Inspection of MainActivity showed that import copied the selected .litertlm file into app-private filesDir/models, but onCreate never searched that directory or reloaded the existing copy. Therefore the app always presented Qwen as not loaded after process restart.

### Corrective source changes
Commit 760f6a2d35c16bb77c68a6bb2e5d9de6b5ee860b
Message: Alpha 0.1: persist Qwen model and expose inference diagnostics
- Startup now searches app-private models for the newest .litertlm file and attempts to load it automatically.
- Manual model selection remains available to replace a failed or different model.
- Send now visibly reports generation in progress.
- Empty responses are surfaced explicitly.
- Generation exceptions now expose exception class and status instead of appearing as silence.
- Successful response updates status with measured response latency.

Commit c6db7a592f3f24e266b73da5de1e59d557b09481
Message: Alpha 0.1: diagnose silent Qwen generation failures
- Inference now counts LiteRT-LM response chunks.
- Zero response chunks becomes an explicit failure instead of a silent empty result.
- A 180-second timeout prevents an indefinitely silent generation attempt.
- sendMessageAsync remains the LiteRT-LM Conversation Flow API documented by the upstream Kotlin runtime.

### Evidence boundary
VERIFIED from physical test:
- Run #15 APK installs/launches on Motorola.
- User can reach the model selection/load path.
- Previous build fails persistence across relaunch.
- Previous build fails to produce a visible Qwen reply.

NOT YET VERIFIED:
- automatic reload of saved Qwen after these corrective commits;
- whether LiteRT-LM emits chunks, returns empty, throws, or times out on this Motorola/model combination;
- successful first local Qwen response;
- RAM/latency/stability targets.

### Next gate
CI must pass these exact corrective commits. Then extract the actual APK and perform a second Motorola test. The second test must record:
1. saved-model automatic load after relaunch;
2. exact status/error after Send;
3. successful response and latency if generation works.
No Attention Engine expansion until local Qwen inference is physically proven.


## 2026-09-18 - Motorola Run #18: FIRST LOCAL QWEN RESPONSE PASS

### Physical evidence
Jon installed the Run #18 APK on the target Motorola and performed the corrected Alpha 0.1 test.

Results:
- Saved-model persistence across app restart: PASS. QuietOS reopened without requiring the Qwen .litertlm model to be selected/imported again.
- Local Qwen generation: PASS. Qwen produced a reply through QuietOS/LiteRT-LM on the Motorola.
- Measured response elapsed time reported by QuietOS: 62,346 ms (62.346 seconds).
- Response latency at this stage: FAIL for acceptable interactive use / optimization required.

This is the first physical proof in the project that the complete local path can produce a response:
QuietOS -> LiteRT-LM -> local Qwen model -> Motorola -> generated reply.

### Evidence boundary
VERIFIED:
- local model persistence/reload works on the target Motorola;
- LiteRT-LM engine can execute the selected Qwen model on the target Motorola;
- local inference can produce a visible response;
- current measured end-to-end response time for this test was 62,346 ms.

NOT YET VERIFIED:
- time to first token separately from total generation time;
- tokens per second;
- prompt/output token counts;
- peak and sustained RAM;
- repeated-turn latency;
- thermal behavior;
- sustained stability;
- app switching, lock/unlock and process-reload behavior;
- whether CPU backend is the best supported backend for this Motorola/model;
- multi-turn conversation continuity.

### Next gate
Do not expand into the Attention Engine yet. Instrument inference so time-to-first-token, total generation time, chunk count and approximate output size are visible. Then establish a repeated-prompt baseline and investigate supported runtime/backend/model optimizations using evidence rather than guesses.


## 2026-09-18 - Alpha 0.1 inference wiring telemetry increment

### Starting evidence
Run #18 physically proved local Qwen inference and saved-model restart persistence on the target Motorola. The first measured complete response took 62,346 ms. The existing UI status "generating" only marked the application call boundary and did not expose where the delay occurred inside LiteRT-LM generation.

### Changes
- LocalModel.send now returns a typed GenerationResult with GenerationMetrics.
- LiteRtQwenModel records the timestamp immediately before conversation generation, the first emitted LiteRT-LM response chunk, and completion.
- Metrics: time to first chunk, generation time after first chunk, total runtime, chunk count, and output character count.
- MainActivity exposes those wiring measurements after each successful response.

### Reason
This separates startup/prompt-processing latency from post-first-output generation throughput. It prevents optimization by guesswork and establishes evidence for the 62-second latency investigation.

### Expected physical evidence
The next Motorola response should display:
[wiring: first chunk X ms | after first Y ms | total Z ms | chunks N | chars C]

### Evidence boundary
Source instrumentation is committed. CI and physical-device results are NOT YET VERIFIED for this increment. No backend or model optimization has been applied yet.


## Alpha 0.1 — Run #25 physical Qwen wiring telemetry PASS

### Physical evidence
- Target device: Jon Dean's Motorola Android test phone.
- CI source: Run #25, commit `385379da652e00858f5d86bd567a8c684c0562f1`.
- Local Qwen response completed successfully.
- UI response measurement: **58,807 ms**.
- LiteRT-LM wiring total: **58,805 ms**.
- Time to first generated chunk: **13,544 ms**.
- Generation after first chunk: **45,261 ms**.
- Emitted chunks: **125**.
- Final visible output: **39 characters**.

### Interpretation / evidence boundary
This test localizes the latency. The delay is not exclusively startup/prefill: first output takes 13.544 s, then generation continues for another 45.261 s. Post-first-chunk generation is the larger component (~77% of measured total). The runtime is therefore functionally connected, but performance remains unacceptable for normal conversation. These measurements do not yet establish token/s, token counts, RAM usage, thermal behavior, repeated-turn stability, or the optimal LiteRT-LM backend.

### Next gate
Do not expand Alpha scope. Measure/reduce decode/generation cost and verify RAM/stability before Attention Engine integration. Preserve this Run #25 measurement as the first internal wiring telemetry baseline.


## Alpha 0.1 — First physical RAM-pressure baseline

### Test method
Jon measured Android memory from Termux/Debian on the same Motorola used for QuietOS physical testing. QuietOS/Qwen was measured closed, loaded/idle, and at three points during a generation. Values below are direct `free -m` observations. Debian/proot was present for all snapshots, so this is a comparative device-level baseline rather than isolated QuietOS process RSS.

### Measurements
| State | Used RAM MB | Free RAM MB | Available MB | Swap used MB |
|---|---:|---:|---:|---:|
| QuietOS/Qwen closed baseline | 2131 | 188 | 1197 | 1232 |
| Qwen loaded / idle | 2670 | 112 | 774 | 1284 |
| Generation — early/send sample | 2505 | 150 | 722 | 2337 |
| Generation — ~15 s sample | 2485 | 164 | 745 | 2405 |
| Generation — later sample | 2237 | 136 | 975 | 2670 |

### Evidence / interpretation
- Loading Qwen idle coincided with +539 MB used physical RAM versus baseline and a 423 MB reduction in MemAvailable.
- During generation, swap usage rose dramatically: from 1,284 MB loaded/idle to as high as 2,670 MB in the later sample, an increase of about 1,386 MB.
- Physical used RAM did not monotonically increase during generation; Android reclaimed/cache-managed memory while swap consumption rose.
- This is direct evidence of substantial device-level memory pressure during Qwen inference on this Motorola.
- It does NOT yet prove that swap activity causes the measured ~58.8 s response latency, nor does it isolate QuietOS/Qwen process RSS or peak working set.

### Alpha status after RAM baseline
- Local Qwen inference: PASS.
- Saved-model persistence: PASS.
- Wiring telemetry: PASS.
- Conversational latency: FAIL / optimization required.
- Memory pressure: FAIL / optimization required for this target device/configuration.

### Next gate
Preserve these measurements as the first RAM comparison baseline. Optimization work must be followed by the same measurement sequence so improvements are evidence-based. Do not expand Attention Engine scope until Qwen runtime latency, RAM pressure, and sustained stability are characterized sufficiently.


## Alpha 0.1 — Run #29 physical optimization comparison

### Configuration under test
- CI Run #29, commit `19e315ce79fc97c43158e02c45a872d4e5a82bfc`.
- LiteRT-LM CPU backend unchanged.
- Engine maxNumTokens reduced from 1024 to 512.
- Conversation maxOutputToken reduced from 128 to 64.
- Thinking remained disabled.

### Physical latency evidence
Jon tested the build on the same Motorola used for the Run #25 baseline.
- App response time: **40,269 ms**.
- Time to first generated chunk: **38,905 ms**.
- Generation after first chunk: **~1,364 ms** (derived from 40,269 - 38,905 because the typed telemetry value was partially garbled as `136e`).
- Emitted chunks: **11**.
- Final visible output: **37 characters**.
- Compared with Run #25: total response improved from 58,807 ms to 40,269 ms (~31.5% faster), while time-to-first-chunk worsened from 13,544 ms to 38,905 ms. Post-first generation collapsed from 45,261 ms to ~1,364 ms, and chunks fell from 125 to 11.

### Physical RAM / swap evidence
Same Termux/Debian environment was left running for comparison. The new baseline before loading QuietOS/Qwen was approximately: used RAM 2,158 MB; free 122 MB; available 1,233 MB; swap used 1,388 MB.
Observed subsequent samples were:
- Qwen loaded/idle: used RAM **2,362 MB**, free **144 MB**, available **1,074 MB**, swap used **1,533 MB**.
- Early/send sample: used RAM **2,316 MB**, free **139 MB**, available **1,077 MB**, swap used **1,546 MB**.
- Mid-generation sample: used RAM **2,268 MB**, free **179 MB**, available **1,111 MB**, swap used **1,573 MB**.
- Later-generation sample: used RAM **2,281 MB**, free **140 MB**, available **1,099 MB**, swap used **1,571 MB**.

### Comparison with Run #25
- Run #25 loaded/idle swap: 1,284 MB; later generation swap: 2,670 MB (+1,386 MB during generation).
- Run #29 loaded/idle swap: 1,533 MB; later generation swap: ~1,571 MB (+38 MB during generation).
- Run #29 therefore shows dramatically less additional swap growth during generation under this test sequence.
- Loaded/idle physical used RAM is also lower than Run #25 (2,362 MB vs 2,670 MB), although whole-device background variation means this is not isolated process RSS.

### Interpretation / evidence boundary
The reduced token/KV budgets materially changed runtime behavior. Memory pressure during generation improved substantially, and total response time improved by about one third. However, almost the entire remaining latency now occurs before the first emitted chunk. This suggests the next optimization target is startup/prefill/conversation setup/backend behavior rather than post-first decode. This test does not yet prove the exact cause of the 38.9 s TTFC, isolated QuietOS process RAM, token throughput, or thermal behavior.

### Status
- Local inference: PASS.
- Model persistence: PASS.
- Memory pressure: materially improved versus Run #25, but still requires characterization.
- Total latency: improved but still FAIL for normal conversation.
- TTFC: regressed and is now the dominant performance problem.

### Next gate
Keep the reduced memory/output budgets for the next controlled experiment unless evidence requires reversal. Investigate the pre-first-chunk path and backend/runtime behavior without broadening Alpha scope. Repeat the same physical RAM and latency protocol after each change.


## Alpha 0.1 — Run #32 physical conversation-reuse test

### Configuration under test
- CI Run #32, commit `db36f7314f813aadd2ee3945c79c02cac7a8a098`.
- Qwen conversation created once at model load and reused for sends.
- 512-token engine/KV budget retained.
- 64 max output tokens retained.
- Thinking disabled.
- CPU backend unchanged.

### Physical telemetry evidence
Screenshot from Jon's Motorola confirms:
- Response: **54,433 ms**.
- First chunk: **53,389 ms**.
- After first: **1,043 ms**.
- Total: **54,433 ms**.
- Chunks: **9**.
- Characters: **34**.

### RAM / swap observations
Under the same Termux/Debian comparative setup, observed samples included:
- Pre-load baseline: used RAM ~2,111 MB; available ~1,343 MB; swap used ~1,115 MB.
- Loaded/early: used RAM ~2,280 MB; available ~1,129 MB; swap used ~1,256 MB.
- Mid-generation: used RAM ~2,240 MB; available ~1,145 MB; swap used ~1,289 MB.
- Later sample: used RAM ~2,084 MB; available ~1,289 MB; swap used ~1,741 MB.

### Comparison / interpretation
- Conversation reuse did **not** improve first-chunk latency on this first physical test. TTFC worsened from Run #29's 38,905 ms to 53,389 ms.
- Post-first generation remained fast at ~1.043 s, confirming the dominant problem remains before the first emitted chunk.
- Memory behavior remained much better than Run #25, though the later swap sample rose to ~1,741 MB and was not as clean as Run #29's ~1,571 MB later sample.
- The screenshot resolves the earlier typed-value ambiguity and confirms the telemetry values above.

### Evidence boundary
This single test does not prove conversation reuse itself causes the regression; device state, thermal state, background activity, cache state, or runtime warmup may contribute. However, there is no evidence from this run that reuse reduces TTFC, so it should not be treated as an optimization win.

### Next gate
Preserve the low-memory token/KV settings. Investigate the pre-first-chunk path, including runtime/backend behavior and warm/cold inference effects, with controlled repeat measurements before broadening Alpha scope.


## Alpha 0.1 — Run #38 physical prefill test

### Configuration under test
- CI Run #38, commit `ced498a503b5e770e9ee54e5b88621128ffeefd2`.
- Persistent Qwen conversation retained.
- LiteRT-LM `prefillPrefaceOnInit = true` enabled on the real conversation.
- 512-token engine/KV budget retained.
- 64 max output tokens retained.
- Thinking disabled.
- CPU backend unchanged.

### Physical startup evidence
Motorola screenshot confirms:
- Load time: **8,489 ms**.
- Reported warmup/prefill time: **29 ms**.

### Physical response telemetry
Motorola screenshot confirms:
- Response: **49,831 ms**.
- First chunk: **48,894 ms**.
- After first: **936 ms**.
- Total: **49,831 ms**.
- Chunks: **9**.
- Characters: **34**.

### Comparison / interpretation
- Versus Run #37, TTFC improved from 57,046 ms to 48,894 ms (~14.3% faster) and total response improved from 58,067 ms to 49,831 ms (~14.2% faster).
- Versus Run #29, Run #38 is still slower: Run #29 TTFC was 38,905 ms and total was 40,269 ms.
- Post-first generation remains fast (<1 second), so the dominant problem remains pre-first-chunk work.
- The reported prefill/warmup duration of only 29 ms indicates that `prefillPrefaceOnInit` did not move a substantial amount of the 49-second first-response cost into startup for this configuration. One likely reason is that the conversation has little/no preface or system instruction to prefill; this should be treated as a hypothesis, not a verified cause.

### RAM evidence boundary
The RAM screenshot supplied alongside this test is timestamped 10:49, while Run #38 startup/response screenshots are timestamped 11:13 and 11:16. Therefore it is not accepted as Run #38 RAM evidence. No new Run #38 RAM claim is recorded from that image.

### Status
- Local inference: PASS.
- Model persistence: PASS.
- Post-first generation: PASS / fast for short response.
- TTFC: still FAIL for normal conversation.
- Prefill experiment: small improvement versus Run #37, not sufficient, and not better than Run #29 baseline.

### Next gate
Keep the low-memory token/KV settings. Move the controlled investigation to CPU/backend/runtime configuration and warm-vs-cold execution, because conversation prefill has not solved the dominant TTFC problem.


## Alpha 0.1 — First Gemma 3 1B physical inference test

### Candidate brain swap
Jon physically loaded the Gemma 3 1B INT4 LiteRT-LM candidate into QuietOS on the Motorola and performed the first conversational test. The QuietOS UI still displays the hardcoded assistant label "Qwen"; that label does not identify the underlying model and should not be treated as model evidence.

### Physical telemetry evidence
Screenshot confirms first-run Gemma response to "Hi Gemma":
- Response: **8,906 ms**.
- First chunk: **2,191 ms**.
- After first: **6,714 ms**.
- Total: **8,906 ms**.
- Chunks: **64**.
- Characters: **248**.

### Comparison with Qwen 1.7B
Compared with the best prior Qwen result (Run #29: 40,269 ms total; 38,905 ms first chunk), Gemma's first physical run is dramatically faster: about **77.9% lower total latency** and about **94.4% lower time-to-first-chunk**. Gemma also produced a substantially longer response in this test (248 characters), so the lower latency is not explained by a shorter visible answer.

### Evidence boundary
This is a single physical conversational test. It verifies that Gemma 3 1B can run through the existing QuietOS LiteRT-LM adapter and deliver much faster first-output and total-response timing on the Motorola. RAM/swap behavior, repeated-turn stability, work-task quality, personality quality, thermals, and long-session behavior are not yet verified for this candidate.

### Next gate
Measure Gemma RAM/swap using the same Termux/Debian protocol, then test repeated conversation and representative work tasks. Do not select the final brain until Gemma proves both capability and resource headroom for QuietOS voice/animation/custom presentation.


## 2026-09-19 - Gemma 3 1B physical RAM/latency follow-up

### Physical evidence supplied by Jon Dean
QuietOS on the Motorola auto-loaded the saved Gemma 3 1B INT4 LiteRT-LM candidate. The installed test build still displayed the older hardcoded Qwen UI labels; this does not identify the loaded model.

App telemetry from the follow-up run:
- Model load: **18,260 ms**
- Warmup: **10 ms**
- Response: **9,288 ms**
- Time to first chunk: **2,549 ms**
- After first chunk: **6,738 ms**
- Total: **9,288 ms**
- Chunks: **64**
- Characters: **248**

Termux/Debian free -m screenshots across the test showed:
- Physical RAM total: **3,643 MB**
- Observed RAM used range: **2,229-2,369 MB**
- Observed available range: **853-1,203 MB**
- Observed swap used range: **1,199-2,016 MB**

### Interpretation
Gemma repeated the sub-10-second total-response result and ~2.5-second first-output behavior, so the latency improvement over Qwen is reproducible across at least two physical conversational runs. The RAM readings are in roughly the same broad whole-device range as the optimized Qwen tests rather than proving a dramatic RAM reduction. Swap varied substantially during the captured sequence, so phase-by-phase causal attribution is not yet justified from this screenshot alone. The key positive result is that Gemma delivers much better interactive latency without an obvious catastrophic RAM blow-up on the 3.6 GB device.

### Evidence boundary / next gate
Do not claim Gemma is lighter than Qwen yet. For a clean memory comparison, capture labeled snapshots at closed baseline, loaded idle, immediate generation, ~15 seconds, and post-generation in one uninterrupted test, then compare deltas from the same baseline. Continue repeated-turn and work-quality testing before final model selection.


## 2026-09-19 - Gemma durable memory contract created

A native Google Doc named `QuietOS Gemma Memory Core - Personal Build` was created as the durable, human-readable memory source for Jon Dean's private QuietOS Gemma build. The document defines identity, behavior modes, DeanWay principles, current Gemma evidence, memory categories, write/retrieval rules, conflict handling, sensitive-data exclusions, and the intended future stack: Drive source of truth -> local structured cache -> relevance retrieval -> prompt/context injection -> authorized write-back/sync.

Evidence boundary: the Drive document exists, but QuietOS does **not** yet retrieve or write Gemma memory automatically. Automatic memory remains NOT IMPLEMENTED until a local memory store, retrieval/index layer, context builder, and authorized Drive sync path are added and physically tested. Drive must not become a hard dependency for local inference.

Google Doc ID: `1SxVyseEGDlpeK-_WyQ16gADXM62t52f5LybQAmWLmRY`


## 2026-09-19 - Gemma tuned conversation A/B test PASS

Physical Motorola test of Run #50 after adding the concise/natural Gemma system instruction while keeping maxOutputToken at 96.

Prompt: Jon asked Gemma to talk naturally, ask one useful question about what he wants QuietOS to become, then respond naturally.

Observed response: `Okay Jon. What's your primary goal for QuietOS?`

Telemetry:
- Time to first chunk: **3,855 ms**
- After first chunk: **1,324 ms**
- Total: **5,179 ms**
- Chunks: **13**
- Characters: **47**

Interpretation: The tuning materially improved response cleanliness and reduced unnecessary filler compared with the previous 96-token untuned run. Gemma followed the instruction to ask one useful question, addressed Jon by name, avoided brochure-style preamble, and completed the thought cleanly. Total latency also dropped to ~5.2 seconds for this response. This is a PASS for concise natural conversation behavior in this probe.

Evidence boundary: one tuned prompt is not sufficient to prove general conversational consistency. Continue multi-turn testing and real work/troubleshooting tasks before locking behavior defaults.


## 2026-09-19 - Gemma multi-turn conversation test: mixed result

Physical Motorola multi-turn test of tuned Run #50.

Observed turns:
1. Jon described QuietOS as a real personal AI system for managing his phone, work, and day without getting in the way. Gemma responded naturally, reflected the goal, and asked one follow-up question. Telemetry: first chunk 3,775 ms; after-first 7,847 ms; total 11,623 ms; 70 chunks; 374 characters.
2. Jon asked for a few helpful suggestions. Gemma produced relevant ideas including task prioritization, notification management, and contextual reminders. Telemetry: first chunk 1,392 ms; after-first 10,132 ms; total 11,525 ms; 96 chunks; 467 characters. The answer ended mid-thought at `Think`, strongly indicating the 96-token output cap was reached and is too low for some useful list-style responses.
3. Jon asked how much Gemma knows about QuietOS. Gemma replied that she was still learning but then claimed there had been significant development since her initial training and that she had been reading about QuietOS integration capabilities. Those claims were not grounded in the active conversation or connected memory and are treated as hallucinated provenance. Telemetry: first chunk 862 ms; after-first 5,763 ms; total 6,626 ms; 55 chunks; 293 characters.

Assessment:
- Multi-turn context retention: PASS for this probe.
- Natural conversational follow-up: PASS.
- Responsiveness: strong, including one 862 ms first-chunk result.
- 96-token cap: FAIL for longer useful answers; truncation observed.
- Grounding/provenance honesty: FAIL; Gemma invented knowledge-source claims about QuietOS.

Next tuning gate:
1. Add a grounding rule: Gemma must not claim to have read, trained on, remembered, or accessed QuietOS/DeanWay information unless that information is actually present in the current conversation, injected memory, or verified tool result.
2. Increase maxOutputToken in a controlled test from 96 to 128 so longer answers can complete.
3. Re-run the same multi-turn sequence and verify both complete answers and provenance honesty.


## 2026-09-19 - Run #55 physical UI PASS and QuietOS grounding failure

Physical Motorola test of Run #55 after enlarging the dedicated conversation window and adding bottom system/IME insets.

UI result:
- Large conversation area visible.
- Message field and SEND button remained reachable above Android navigation and the on-screen keyboard.
- Layout fix: **PASS**.

Conversation probe:
- Jon: `hi Gemma`
- Gemma: `Hi Jon! How’s your day going?`
- Telemetry: first chunk **3,239 ms**; after first **1,042 ms**; total **4,281 ms**; chunks **10**; chars **29**.

QuietOS knowledge probe:
- Jon asked what Gemma knew about QuietOS and its purpose.
- Gemma incorrectly described QuietOS as a Linux-based environment focused on quiet/minimal operation and Linux applications.
- Telemetry: first chunk **2,513 ms**; after first **9,010 ms**; total **11,524 ms**; chunks **84**; chars **406**.

Assessment:
- UI usability: PASS.
- Natural greeting: PASS.
- Grounding/provenance: FAIL. Gemma invented a definition of QuietOS not supplied by the active conversation or memory/tool context.

Corrective change committed immediately after this test:
- Added explicit grounding rule preventing invented claims about QuietOS, DeanWay, Jon, training, memory, reading, tool access, or external sources.
- Gemma must say she does not know when relevant facts are not supplied by current conversation or QuietOS context.
- Increased maxOutputToken from **96 to 128** for a controlled completion test.
- Code commit: `705d211eda54f6213c396e39f7434f0193438751`.

Next test: rerun the same QuietOS knowledge question. Expected behavior is either an answer grounded only in supplied context or a plain statement that she does not yet have enough information. No invented Linux/minimalism story.


## 2026-09-19 - Run #56 grounding retest FAIL; authoritative core reference added

Physical Motorola retest after adding grounding rules and raising maxOutputToken to 128.

Prompt: `Tell me what you know about QuietOS and what its purpose is.`

Observed response remained hallucinated. Gemma described QuietOS as an assistant focused on privacy/control, then invented a `Privacy-First` approach, active-mode privacy toggles, an Offline mode, and QuietOS/QuietOS Lite privacy tiers that are not part of the verified project state.

Telemetry from screenshot:
- First answer: first chunk **4,439 ms**; after first **6,869 ms**; total **11,309 ms**; chunks **66**; chars **344**.
- Follow-up answer: first chunk **918 ms**; after first **11,861 ms**; total **12,779 ms**; chunks **110**; chars **515**.
- Status line later showed first **869 ms**, total **12,057 ms** for the next generation.

Assessment:
- Grounding rule alone: **FAIL**. The model still filled missing project knowledge with plausible inventions.
- 128-token cap: no truncation observed in these shown turns.
- Latency: first-token response remained strong on later turns.

Corrective architecture change:
- Added an authoritative QuietOS core reference directly to Gemma's system context, derived from current README and architecture docs.
- Core facts include: local-first Android attention intelligence; notification capture/analyze/classify/delivery/explainability; attention classes; deterministic emergency path; Gemma identity; Gemma 3 1B IT INT4 via LiteRT-LM; separation between Gemma reasoning and QuietOS permissions/tools/execution; and explicit statements that QuietOS is not a Linux distribution or generic privacy-mode OS.
- Existing anti-hallucination grounding rules remain.
- Code commit: `8f557a1200325e6c6848ffc18cd8917672ccd4fe`.

Next test: ask the exact same QuietOS-purpose question. PASS requires an answer substantially grounded in the injected core reference without invented modes, tiers, platforms, or source claims.


## 2026-09-19 - Run #58 physical grounding PASS; zero-chunk error and Jon identity gap found

Physical Motorola test of the build with authoritative QuietOS core reference injected into Gemma's system context.

Primary grounding prompt: `Tell me what you know about QuietOS and what its purpose is.`

Observed answer was substantially grounded in the real project: Gemma identified herself as Jon's QuietOS assistant; described intelligent notification handling, capture/analyze/classify/delivery timing, deterministic safety paths, and the Gemma 3 1B IT INT4 local model. A later repeat also correctly described QuietOS as managing attention and proactively handling notifications, with Gemma as Jon's personal assistant through LiteRT-LM.

Telemetry:
- First grounded answer: first chunk **7,953 ms**; after first **10,507 ms**; total **18,460 ms**; chunks **100**; chars **463**.
- Later grounded repeat: first chunk **842 ms**; after first **5,661 ms**; total **6,503 ms**; chunks **52**; chars **253**.

Grounding assessment: **PASS** for the QuietOS-purpose probe. The injected core reference corrected the prior Linux/privacy-mode hallucinations.

New issues exposed:
1. Prompt `who is Jon` produced `IllegalStateException: LiteRT-LM completed without producing response chunks.` This is a runtime robustness failure, not a content failure.
2. After the error/reload, a later `who is jon` answer incorrectly said Jon was an account used by QuietOS. The current core reference names Jon but does not define who he is, so Gemma filled that gap incorrectly.
3. User reported the chat window/flow felt off after the zero-chunk error, indicating error recovery should preserve a stable UI/conversation state instead of leaving the session in an awkward state.

Next corrective work:
- Add explicit Jon identity to the authoritative core reference (Jon Dean, owner/builder of DeanWay Labs/QuietOS; Gemma is his personal QuietOS assistant).
- Add controlled recovery for zero-chunk generation: detect no-output completion, recover the conversation cleanly, preserve UI usability, and avoid raw exception text as the user-facing response.
- Regression-test the QuietOS-purpose prompt and `Who is Jon?` after recovery work.


## 2026-09-19 - Run #61 regression PASS: Jon identity and QuietOS grounding

Physical Motorola regression test after adding explicit Jon identity and zero-chunk recovery.

Prompt 1: `who is Jon`
- Gemma answered: `Jon Dean owns QuietOS.`
- Telemetry: first chunk **8,571 ms**; after first **586 ms**; total **9,157 ms**; chunks **6**; chars **22**.
- Identity grounding: **PASS**. The prior incorrect `Jon is an account` answer was corrected.

Prompt 2: `Tell me what you know about QuietOS and what its purpose is.`
- Gemma correctly identified QuietOS as an attention-intelligence project by DeanWay Labs built by Jon Dean, and correctly stated that its primary purpose is intelligent notification handling rather than being an Android distribution or operating system.
- Telemetry: first chunk **1,741 ms**; after first **5,382 ms**; total **7,124 ms**; chunks **53**; chars **261**.
- QuietOS grounding: **PASS**.

Runtime recovery observation:
- The previous zero-chunk exception did **not** recur during these two regression prompts, so no recovery path was triggered in this test. Therefore the new recovery code remains implemented but not yet physically exercised.

Minor issue:
- The QuietOS answer stopped at `and keeping`, leaving the final sentence incomplete despite only 53 chunks, so this is not evidence of the 128-token cap being reached. Marked as a response-completion quality issue for later tuning, not a grounding failure.


## 2026-09-19 - Run #70 physical identity-memory PASS; broader DeanWay memory batched

Physical Motorola test of deterministic QuietOS-owned identity recall.

Observed probes:
- `Who are you?` -> `I am Gemma, Jon Dean's personal assistant inside QuietOS.`
- `Who am I?` -> correctly identified Jon Dean, DeanWay LLC / DeanWay Labs ownership, QuietOS builder/product-owner role, and primary-user status.
- `What is my role in QuietOS?` -> correctly returned owner, builder, product owner, primary user, and physical-device tester.

Telemetry for deterministic memory answers:
- first chunk: **0 ms**
- after first: **0 ms**
- total: **0 ms**
- chunks: **0**
- Responses came from QuietOS memory retrieval rather than local-model generation.

Assessment:
- Identity recall: **PASS**.
- Jon/Gemma perspective separation for these known questions: **PASS**.
- Architecture principle physically demonstrated: QuietOS handles verified facts it already knows; Gemma is reserved for understanding/generation work.

Next memory expansion:
- Broadened the same deterministic memory layer to cover stable DeanWay topics in one batched change: DeanWay LLC, DeanWay Labs, DeanWay Travels, QuietOS purpose, QuietOS Network, GhostMode lineage, DeanWay Absolutes, locked QuietOS pricing baseline, and the QuietOS/Gemma permissions boundary.
- Kept the always-loaded personal core concise; known factual probes are answered by QuietOS memory directly rather than forcing the 1B model to reconstruct them.
- Build strategy changed to reduce tester churn: batch several verified memory additions before generating the next APK instead of creating a new phone download for each tiny fact.


## 2026-09-19 - Run #73 physical memory-routing PASS; Attention Engine integration batch started

Physical Motorola evidence supplied by Jon Dean for Run #73:
- Prompt: `what are the 5 absolutes?`
- QuietOS deterministic memory returned the canonical five DeanWay Absolutes.
- Response telemetry: first chunk 0 ms; total 1 ms; chunks 0; chars 337.
- Prompt: `what is DeanWayLLC`
- QuietOS deterministic memory correctly returned DeanWay LLC as Jon Dean's umbrella business with DeanWay Labs and DeanWay Travels.
- Response telemetry: first chunk 0 ms; total 0 ms; chunks 0; chars 138.

Assessment:
- Canonical five-Absolutes retrieval: PHYSICAL PASS.
- DeanWay LLC deterministic recall: PHYSICAL PASS.
- Run #73 becomes the current verified memory-routing baseline.

### Attention Engine integration batch
Implemented source for the next functional QuietOS milestone:
- Android NotificationListenerService capture adapter.
- Deterministic Attention Engine categories NOW / SOON / DIGEST / QUIET.
- Explicit emergency precedence over lower-priority rules.
- Direct-request and promotional keyword inference.
- Local capped attention-record repository retaining the latest 100 captured records.
- Main UI controls for Android notification-listener settings and attention-log refresh.
- UI summary counts plus recent classifications and explainable reasons.
- Added unit coverage for emergency precedence, explicit/inferred direct requests, promotions, and default digest behavior.

Safety boundary:
- This increment is capture + classify only.
- QuietOS does NOT cancel, delay, suppress, or replace original Android notifications yet.
- Delivery intervention remains blocked until physical classification evidence exists.

Verification state before CI:
- Source implemented: YES.
- Unit tests/build: NOT YET VERIFIED.
- Notification listener physical capture: NOT YET VERIFIED.
- Classification behavior on Motorola: NOT YET VERIFIED.
- Notification suppression/delay: NOT IMPLEMENTED.


## 2026-09-19 - Run #74 physical notification-capture PASS; Android interception boundary exposed

Physical Motorola evidence:
- QuietOS notification listener captured two test notifications.
- Both were classified DIGEST with reason `default useful event`.
- Attention Engine UI showed captured 2 / DIGEST 2.
- Gemma remained loaded and responsive in the same build.

Assessment:
- Notification listener registration/capture: PHYSICAL PASS.
- Attention record storage/display: PHYSICAL PASS.
- Deterministic classification path: PHYSICAL PASS for ordinary DIGEST notifications.
- Pre-delivery interception: NOT AVAILABLE through standard NotificationListenerService.

Observed Android behavior:
- The original notification reached Android's notification UI before QuietOS recorded/classified it.
- This is expected from NotificationListenerService semantics: `onNotificationPosted` is delivered after Android posts the notification. It cannot provide true before-post interception for third-party notifications.

Next controlled experiment:
- Add an optional Interception Test Mode, default OFF.
- In test mode, NOW and SOON are never cancelled.
- DIGEST and QUIET are cancelled immediately after `onNotificationPosted` and classification.
- Record whether QuietOS cancelled the original.
- This measures the fastest suppression possible to a normal Android app while preserving deterministic emergency/direct-request safety.
- True pre-delivery interception remains an OS/platform-level capability, not a standard app capability.


## 2026-09-19 - Run #75 physical interception PASS; operating Attention Mode batch

Physical Motorola evidence supplied by Jon Dean:
- Run #75 installed and Interception Test Mode was enabled.
- QuietOS caught the tested notification quickly enough that it did not visibly establish itself in the Android notification bar.
- This matches the user-visible interception behavior previously proven by GhostMode.
- Therefore visible DIGEST/QUIET interception on this Motorola is a PHYSICAL PASS for Run #75.

Important wording correction:
- Android NotificationListenerService still receives the posted-notification callback rather than a privileged before-post hook.
- However, immediate classification/cancellation is fast enough on this device to prevent the notification from visibly landing in normal use.
- The prior registry wording that treated useful pre-bar interception as unavailable was too absolute and is superseded by this physical evidence.

### Run #76 integration batch
- Promote the user-facing toggle from "Interception Test Mode" to "QuietOS Attention Mode"; it remains user-controlled and is not silently enabled.
- Preserve policy: NOW and SOON stay in Android's normal flow; DIGEST and QUIET are intercepted/cancelled when Attention Mode is ON.
- Add a local Gemma digest bridge using at most five DIGEST records and bounded notification text so context remains controlled.
- Add a one-tap Clear Attention Log control.
- Keep raw notification storage local.
- Build/test evidence for this batch is pending CI and physical Motorola verification.


## 2026-09-19 - Run #76 physical Attention Mode + Gemma digest PASS with follow-up issues

Physical Motorola evidence supplied by Jon Dean:
- QuietOS Attention Mode was ON.
- Attention Engine showed 7 captured records: NOW 0, SOON 0, DIGEST 5, QUIET 2.
- All 7 captured DIGEST/QUIET records were cancelled by QuietOS before becoming visibly established in the normal notification bar flow.
- The UI explicitly reported `cancelled 7`.
- Gemma digest summarization completed locally; UI reported `digest total 27122 ms`.
- The attention log showed explainable classification reasons and interception actions.

Assessment:
- Attention Mode interception policy on Motorola: PHYSICAL PASS for the captured DIGEST/QUIET set.
- Attention repository/display: PHYSICAL PASS.
- Gemma digest bridge: PHYSICAL PASS for completion.
- User-visible interception speed: PHYSICAL PASS on this device for this run.

Issues exposed by physical use:
1. Duplicate notifications are being retained separately; two identical ZipRecruiter records were shown. Add deterministic duplicate collapse before storage/digest.
2. The ZipRecruiter job recommendation was classified QUIET using the promotional-content rule. That may be too aggressive for job/opportunity notifications and needs rule refinement rather than assuming all recommendation language is promotional noise.
3. Digest summarization completed but took 27,122 ms, which is functional but too slow for an ideal interactive experience. Optimize prompt size/item count and consider asynchronous/background presentation.
4. The long attention log pushes the conversation/digest result below the fold. The Attention UI needs a more usable layout before product polish.

Next batch should prioritize deduplication, safer category refinement, and digest/UI responsiveness without weakening the verified NOW/SOON pass-through safety behavior.


## 2026-09-19 - Duplicate notification collapse implemented

Jon confirmed duplicate capture should be fixed while leaving the current ZipRecruiter QUIET classification unchanged because that content is unwanted spam in actual use.

Implementation:
- AttentionRepository now collapses duplicate records from the same package when title and text match exactly within a 5-minute window.
- The newest matching record replaces the older duplicate in storage.
- Existing classification, interception, and safety behavior remain unchanged.
- This is intended to keep repeated identical pushes from bloating the attention log and Gemma digest.

Verification state:
- Source implemented.
- CI/build verification pending.
- Physical Motorola duplicate-collapse test pending.


## 2026-09-19 - Run #78 physical emergency bypass PASS; digest timing improved

Physical Motorola evidence supplied by Jon Dean:
- QuietOS Attention Mode was ON.
- Attention Engine showed 3 captured records: NOW 1, SOON 0, DIGEST 2, QUIET 0.
- 2 records were cancelled by QuietOS.
- The emergency test email was classified NOW with reason `deterministic emergency rule`.
- The emergency notification was left in Android's normal notification flow and reached the notification bar as intended.
- DIGEST notifications were intercepted/cancelled.
- Gemma digest summarization completed locally with reported timing: first chunk 11,423 ms, total 12,838 ms, chunks 13.
- This is materially faster than the prior observed 27,122 ms digest completion, though the first-token delay remains noticeable.

Assessment:
- Emergency classification: PHYSICAL PASS.
- Emergency bypass/pass-through: PHYSICAL PASS.
- DIGEST interception: PHYSICAL PASS in this sample.
- Local Gemma digest completion: PHYSICAL PASS.
- Duplicate-record collapse is not directly proven by this screenshot because no repeated stored duplicate pair is present.

Additional observation:
- Some Android notification fields contain overlapping EXTRA_TEXT / EXTRA_BIG_TEXT content, causing repeated phrases inside a single stored record even when repository-level duplicate collapse works. Future cleanup should prefer the richer field when one contains the other instead of concatenating both.


## 2026-09-19 - Duplicate cleanup v2 after physical duplicates remained

Physical feedback:
- Jon reported duplicate records were still hanging around after Run #78.

Root causes identified:
1. The first dedupe change only affected newly added notifications. Existing duplicate records already stored in SharedPreferences survived app updates.
2. Android can expose overlapping EXTRA_TEXT / EXTRA_BIG_TEXT / EXTRA_SUB_TEXT fields, so visually identical notifications can have slightly different stored strings.

Fix:
- Added one-time dedupe migration v2 for existing stored records. It collapses semantically identical package/title/text records already in the retained log.
- New-record dedupe now uses normalized lowercase/whitespace semantic keys plus the existing 5-minute live duplicate window.
- Notification capture now prefers the richest text field and only appends non-overlapping extra fields, preventing repeated phrases inside one captured record.
- Classification/interception behavior is unchanged.

Verification state:
- Source implemented.
- CI/build verification pending.
- Physical Motorola verification pending.


## 2026-09-19 - QuietOS Attention Engine keeper baseline declared

Jon Dean confirmed the current QuietOS Attention Engine behavior as a definite physical PASS on the Motorola after fresh-install testing.

Verified physical behavior across the current keeper sequence:
- DIGEST/QUIET interception works quickly enough to keep unwanted notifications from visibly settling in the notification bar.
- NOW/emergency notifications bypass suppression and remain in Android's notification flow.
- Deterministic emergency classification is physically verified.
- Duplicate notification clutter is resolved in current testing.
- Attention logging is functional.
- Gemma can locally summarize the collected DIGEST notifications.
- QuietOS Attention Mode operates as intended in real notification traffic, not just synthetic unit tests.

Milestone decision:
- Treat the current Attention Engine as a keeper baseline for Alpha 0.1.
- Preserve this known-good behavior before adding voice-command controls.
- Next functional direction: voice-first control layer, starting with push-to-talk command routing before hands-free wake-word behavior.
- Buttons remain fallback controls; Gemma interprets intent while QuietOS retains authority for permissions/actions.

Evidence rule remains active:
- New voice work must not regress emergency bypass, DIGEST/QUIET interception, dedupe, or digest functionality.


## 2026-09-19 - Voice-first control layer, Stage 1 implemented

Starting point:
- Attention Engine keeper baseline physically passed on Motorola.
- Jon approved voice-first control as the next direction while retaining buttons as fallback controls.

Implementation:
- Added a deterministic VoiceCommandRouter separate from Gemma model generation.
- Added push-to-talk "Talk to Gemma" control using Android speech recognition.
- Recognized speech is routed to QuietOS commands before any model call.
- Supported first-stage voice commands:
  - summarize digest
  - Attention Mode on
  - Attention Mode off
  - clear attention log
  - refresh attention
  - report/refresh what QuietOS caught
- Speech that is not a recognized QuietOS control falls through to the existing Gemma conversation path.
- QuietOS, not Gemma, executes state-changing commands.
- Existing buttons remain functional fallbacks.
- Attention Mode state writes were centralized so button and voice paths use the same implementation.

Tests:
- Added unit tests for each deterministic command route plus ordinary-conversation fallback.

Safety / regression boundary:
- Voice does not alter the verified Attention Engine classification policy.
- Emergency NOW/SOON pass-through and DIGEST/QUIET interception behavior remain unchanged.
- Always-listening wake-word behavior is NOT implemented in this stage.
- Physical speech recognition and command execution remain NOT VERIFIED until Motorola testing.


## 2026-09-19 - Voice Stage 1 CI failure 001 and router correction

Evidence:
- Run #82 reached the unit-test step and failed before APK assembly.
- The new voice-router tests exposed a phrase-matching defect in Attention Mode commands.

Root cause:
- The initial router searched for contiguous phrases such as `turn on` / `turn off`, but natural commands such as `turn Attention Mode on` insert the target words between the verb and state.

Fix:
- Attention Mode ON/OFF routing now explicitly accepts natural forms such as:
  - turn Attention Mode on/off
  - switch Attention Mode on/off
  - enable/disable Attention Mode
  - start/stop Attention Mode
  - phrases ending in on/off when Attention Mode is explicitly named.
- No Attention Engine behavior changed.

Verification:
- Fix committed; post-fix CI pending.


## 2026-09-19 - Voice vocabulary normalization after physical Motorola mismatch

Physical evidence from Jon Dean on Run #83:
- Spoken request intended as `what is DeanWay Labs` was transcribed by Android as `what is Greenway labs`.
- Another attempt was transcribed as `what is Dean way labs`.
- These malformed transcripts bypassed deterministic DeanWay memory matching and were sent to Gemma as ordinary conversation, causing incorrect generated answers.
- Jon also reported `Jon Dean` being recognized as `John Dean`.

Fix:
- Added a deterministic VoiceTextNormalizer that runs after Android speech-to-text and before QuietOS command/memory routing.
- Canonical replacements currently include:
  - John Dean -> Jon Dean
  - Dean way / Greenway Labs -> DeanWay Labs
  - Dean way / Greenway Travels -> DeanWay Travels
  - Dean way -> DeanWay
  - Quiet OS -> QuietOS
  - Ghost mode -> GhostMode
- When QuietOS changes the transcript, the corrected text is shown in the conversation for test visibility.
- Original recognized speech is still displayed so physical evidence remains visible.
- Added unit tests for canonical DeanWay vocabulary and non-target speech pass-through.

Scope:
- This corrects known personal/product vocabulary deterministically rather than asking Gemma to guess.
- Attention Engine logic is unchanged.
- Physical Motorola verification pending.


## 2026-09-19 - Voice vocabulary normalization physical PASS

Physical Motorola evidence supplied by Jon Dean on Run #87:
- "Dean way labs" was corrected to "DeanWay Labs" before routing.
- "Dean way travels" was corrected to "DeanWay Travels" before routing.
- "quiet OS" was corrected to "QuietOS" before routing.
- "Ghost mode" was corrected to "GhostMode" before routing.
- Deterministic DeanWay memory responses for DeanWay Labs, DeanWay Travels, GhostMode, and Gemma identity returned with 0 ms model-generation telemetry where applicable.

Assessment:
- Voice vocabulary normalization layer: PHYSICAL PASS.
- Canonical DeanWay vocabulary is now being corrected before command/memory/model routing.
- The original raw speech remains visible for evidence, and the corrected form is shown separately.
- Attention Engine behavior remains unchanged.

Separate downstream observation:
- The generated QuietOS explanation still rendered "Dean Way Labs" with a space and included an inaccurate statement that Gemma was initially trained by LiteRT-LM. This is a model-output grounding/canonicalization issue, not a speech-recognition or voice-normalization failure.
- Treat voice normalization as passed while tracking model-output canonicalization/grounding separately.


## 2026-09-19 - QuietOS identity grounding hardening after Run #87 screenshot review

Physical evidence:
- Voice normalization itself passed.
- Spoken "who is quiet OS" was corrected to "who is QuietOS".
- That phrasing did not match the existing deterministic QuietOS memory route, so it fell through to generative Gemma output.
- The generated answer introduced two defects: it rendered "Dean Way Labs" with a space and incorrectly said Gemma was initially trained by LiteRT-LM.

Fix:
- Expanded deterministic QuietOS identity routing to include "who is QuietOS" and "tell me about QuietOS" in addition to the existing "what is QuietOS" / purpose forms.
- Added unit tests proving these prompts route to grounded QuietOS memory and contain canonical "DeanWay Labs" wording.
- This avoids unnecessary model generation for a known factual identity question and removes the opportunity for the LiteRT-LM training hallucination on this path.

Scope:
- Voice normalization remains unchanged.
- Attention Engine remains unchanged.
- Physical Motorola verification pending.


## 2026-09-19 - Run #91 physical identity-grounding PASS

Physical Motorola screenshots supplied by Jon Dean show:
- Android speech transcription "who is John Dean" corrected by QuietOS to "who is Jon Dean".
- Deterministic Jon Dean identity answer returned with response 0 ms / chunks 0.
- Android speech transcription "what is quiet OS" corrected by QuietOS to "what is QuietOS".
- QuietOS returned the grounded canonical description beginning: "QuietOS is a local-first Android attention-intelligence and personal-assistant system by DeanWay Labs..."
- DeanWay Labs voice normalization and deterministic memory remained correct at 0 ms.
- "who are you" continued to return Gemma's deterministic identity at 0 ms.

Assessment:
- Voice normalization regression: PASS.
- Jon Dean identity routing: PASS.
- QuietOS identity grounding fix from Run #91: PHYSICAL PASS.
- DeanWay Labs deterministic routing: PASS.
- Gemma identity routing: PASS.
- No evidence in this test of regression to the Attention Engine; Attention Mode was OFF during the screenshots and attention behavior was not exercised.

Milestone decision:
- Treat Run #91 identity/voice grounding as the current verified baseline.


## 2026-09-20 - Run #92 hands-free voice activation checkpoint recovered

Status: DEVELOPMENT CHECKPOINT RECOVERED / IMPLEMENTATION NOT YET VERIFIED

### Starting evidence
- GitHub main currently ends at the physically verified Run #91 identity/voice grounding baseline.
- The Run #91 APK has been preserved by Jon Dean as the physical rollback build.
- Jon confirmed that development had already moved beyond push-to-talk and had just started the next stage: hands-free voice activation.
- The prior development chat entered a thinking loop before that hands-free work could be documented or committed.

### Evidence boundary
- Run #91 remains the latest committed and physically verified baseline.
- Push-to-talk voice routing is already implemented and is NOT the current development target.
- Voice vocabulary normalization and deterministic identity/memory grounding are already physically verified and must be preserved.
- Attention Engine keeper behavior remains a protected regression boundary: emergency/NOW pass-through, DIGEST/QUIET interception, deduplication, attention logging, and local Gemma digest behavior must not be weakened.
- Hands-free voice activation work had started conversationally, but no surviving committed source or physical evidence has yet been identified for that increment. Its exact implementation state is therefore NOT VERIFIED.

### Active development position
Run #92 is reserved for the hands-free voice activation stage. Work must resume from the Run #91 keeper source unless concrete surviving post-Run-91 implementation evidence is found.

### Documentation rule for Run #92
Before any hands-free behavior is claimed complete, record:
1. exact wake/listening architecture and Android APIs used;
2. files changed;
3. permission and lifecycle behavior;
4. battery/background implications;
5. command-routing integration with the existing deterministic voice path;
6. unit/CI evidence;
7. Motorola physical evidence;
8. regressions against the Run #91 voice baseline and Attention Engine keeper baseline;
9. failures, fixes, and rollback point.

No undocumented prior hands-free behavior is to be reconstructed as fact. Evidence comes before claims.


## 2026-09-20 - Run #92 Step 1: hands-free listening state foundation

Status: SOURCE IMPLEMENTED / CI PENDING / PHYSICAL BEHAVIOR NOT YET IMPLEMENTED

Starting state:
- Run #91 remains the physically verified keeper baseline.
- Existing push-to-talk, VoiceTextNormalizer, VoiceCommandRouter, Gemma routing, and Attention Engine were inspected and left unchanged.
- MainActivity currently launches Android RecognizerIntent only from the manual Talk to Gemma button.
- AndroidManifest currently contains no hands-free voice service or microphone permission.

Step 1 implementation:
- Added voice/HandsFreeListeningController.kt.
- Added a deliberately small state machine with OFF, READY, LISTENING, PROCESSING, and ERROR states.
- The controller currently owns state only. It does not yet open the microphone, create a SpeechRecognizer, run a foreground service, or alter existing routing.
- Added unit tests for enable/ready, listening/processing/ready lifecycle, disabled-event protection, and disable/off behavior.

Protected boundaries:
- No Attention Engine files changed.
- No Gemma/model files changed.
- No VoiceCommandRouter or VoiceTextNormalizer behavior changed.
- No MainActivity behavior changed.
- No Android permissions changed.

Evidence:
- Source commit: 4bf0b8b140823113e1c9f40a45a1479388c656e8.
- Test commit: 02c869937bf7e00cb7f333c59ba34246025b9f7d.
- CI result: PENDING.
- Motorola physical result: NOT APPLICABLE yet because Step 1 intentionally adds no active microphone behavior.

Next gate:
Verify CI. Only after a green build should Run #92 Step 2 connect this state foundation to Android speech-recognition lifecycle/permission behavior.


## 2026-09-20 - Run #92 Step 1 CI PASS / voice-command physical gate

Status: CI PASS / PHYSICAL VOICE-COMMAND VERIFICATION NEXT

CI evidence:
- GitHub Actions run #96, run ID 35464853219, completed SUCCESS.
- Tested head commit: 63a5a6e23dfce16bd8717c4cf3064063fdddbe0f.
- Unit tests: PASS.
- Debug APK build: PASS.
- Artifact upload: PASS.
- Artifact: QuietOS-alpha-debug, artifact ID 10590029920.
- Artifact SHA-256: 3470737d597b945d2e05822d96dab7f9fc987ce2661b70b1471bba272a62c7ac.

Recovered roadmap gate:
Before advancing the hands-free wake path, physically verify the existing voice-control layer on the Motorola:
1. Attention Mode ON.
2. Attention Mode OFF.
3. Summarize digest.
4. Clear attention log.
5. Refresh attention.
6. "What did QuietOS catch?"

The Run #92 Step 1 listening-state controller remains isolated and is parked until this physical gate is complete. No further hands-free implementation is claimed or started at this checkpoint.
