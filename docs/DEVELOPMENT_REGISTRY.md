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
