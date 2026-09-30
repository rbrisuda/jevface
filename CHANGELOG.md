# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- `@JevAgent` interfaces with `@NoulQuestion`, `@ChoiceQuestion` and `@ScoreQuestion` methods, evaluated in a
  single Jev request and returned as a proxy implementing the interface.
- Return-type driven mapping: `boolean`, `double`, `int`, enums, `String`, `NoulResult`, `ChoiceResult`,
  `ScoreResult` and `Optional` with `stated` gates and confidence thresholds.
- `default` methods as business rules on top of the answers.
- `jevface-typesafe`: `JudgmentEngine` for TypeSafe's Jev API via the Spring AI Community TypeSafe SDK.
- `jevface-spring-boot-starter`: auto-configuration and injectable `JevClient<Agent>` beans.
- `jevface-test`: `StubJudgmentEngine` for tests without an API key.
- Examples: Spring Boot customer support routing and plain Java review analysis.
