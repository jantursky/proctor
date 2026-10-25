## What and why

<!-- What this changes, and the problem it solves. Link the issue if there is one. -->

## Checklist

- [ ] `./gradlew spotlessCheck apiCheck :proctor-android:checkKotlinAbi koverVerify` passes
- [ ] `./gradlew :proctor:jvmTest :proctor-report:test :proctor-android:testAndroidHostTest :proctor-gradle-plugin:test` passes
- [ ] `./gradlew :proctor:iosSimulatorArm64Test` passes, if iOS code changed
- [ ] `./gradlew apiDump` and `:proctor-android:updateKotlinAbi` were run, if the public API changed
- [ ] New behavior has tests
- [ ] Anything not run on a real device or model is marked unverified in the docs
- [ ] `CHANGELOG.md` has an entry under `## [Unreleased]`
