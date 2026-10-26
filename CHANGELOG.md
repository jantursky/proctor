# Changelog

All notable changes to `io.github.jantursky:proctor` are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `proctor`: suite DSL (`evalSuite`), graders (exact match, regex, JSON schema, semantic similarity, LLM judge), a runner with pacing, quota backoff and repeats, a fake backend, and the result JSON format (schema version 1).
- `proctor-android`: Gemini Nano through the ML Kit GenAI Prompt API, with stable and preview model selection, a foreground guard and a host activity.
- `proctor-ios`: Apple Foundation Models through a small Swift bridge.
- `proctor-report`: HTML and Markdown reports that compare platforms, model versions and prompt versions.
- `io.github.jantursky.proctor` Gradle plugin with the `proctorReport` task and CI pass conditions.
- Sample ticket-triage suites, an Android instrumented runner and a headless iOS simulator runner.
