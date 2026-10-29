# Contributing

Thanks for helping. Bug reports, docs fixes and pull requests are all welcome.

## Before you start

- For anything bigger than a small fix, open an issue first so we can agree on the approach.
- On-device model behavior depends on the device, the OS and the model version. If you can, say which device, OS and model version you tested on; the `environment` block of a result file has all three.
- Never claim a result you did not run. A backend, platform or model version that was not exercised on real hardware is marked unverified in the docs.

## Setup

You need JDK 17 and an Android SDK. The Gradle wrapper handles the rest. For iOS you need Xcode with the iOS 26 SDK or newer.

```sh
./gradlew :proctor:jvmTest                          # core, quickest feedback
./gradlew :proctor-report:test :proctor-gradle-plugin:test
./gradlew :proctor:iosSimulatorArm64Test            # needs macOS
```

Android emulators have no AICore with Gemini Nano. The Android backend is unit-tested against a fake client and verified only on a real, supported device.

## Where things live

| path | what |
| --- | --- |
| `proctor/` | Core: suite DSL, graders, runner, result format, `FakeBackend`. No LLM SDK. |
| `proctor-mlkit/` | Gemini Nano through the ML Kit Prompt API. |
| `proctor-report/` | Result collector, diffs, HTML and Markdown output, CI verdict. |
| `proctor-gradle-plugin/` | The `proctorReport` task. |
| `design/` | Design tokens and the logo. |

## Checks

```sh
./gradlew spotlessApply          # format
./gradlew spotlessCheck apiCheck :proctor-mlkit:checkKotlinAbi koverVerify
```

If the public API changed, run `./gradlew apiDump :proctor-mlkit:updateKotlinAbi` and commit the dumps. Coverage gates are 90% lines and 78% branches per module.

## Pull requests

Keep them small and focused, add a test for new behavior, and add a line under `## [Unreleased]` in `CHANGELOG.md`.
