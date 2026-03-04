# Testing

## Quick Reference

| What              | Command                                    | Framework        |
|-------------------|--------------------------------------------|------------------|
| All checks        | `./gradlew check`                          | Gradle composite |
| Java/Kotlin unit  | `./gradlew :plugin-core:test`              | JUnit 5          |
| JS chat-ui unit   | `./gradlew :plugin-core:jsTest`            | Vitest + happy-dom |
| JS watch mode     | `cd plugin-core/js-tests && npm run test:watch` | Vitest      |
| Integration tests | `./gradlew :integration-tests:test`        | JUnit 5          |

## JavaScript Tests (Chat UI)

The chat panel is rendered via JCEF using custom web components built in TypeScript.
Tests live in `plugin-core/js-tests/` and run against the source in `plugin-core/chat-ui/src/`.

### Run once

```bash
./gradlew :plugin-core:jsTest
```

Or directly with npm:

```bash
cd plugin-core/js-tests
npm test
```

### Watch mode (re-runs on file change)

```bash
cd plugin-core/js-tests
npm run test:watch
```

### Test files

| File                          | Covers                                          |
|-------------------------------|--------------------------------------------------|
| `chat-components.test.js`    | Web components (tool-section, chat-message, etc.) |
| `chat-controller.test.js`    | ChatController API, streaming, tool calls         |

### Setup

Tests require Node.js. Install dependencies once:

```bash
cd plugin-core/js-tests && npm install
cd plugin-core/chat-ui && npm install
```

The test environment uses [happy-dom](https://github.com/nicedayfor/happy-dom) for
lightweight DOM simulation — no browser needed.

## Java / Kotlin Unit Tests

Unit tests use JUnit 5 and run via Gradle. Integration-tagged tests are excluded by default.

```bash
./gradlew :plugin-core:test
```

Run a single test class:

```bash
./gradlew :plugin-core:test --tests "com.github.catatafishen.ideagentforcopilot.SomeTest"
```

In IntelliJ, right-click any test class or method and select **Run**.

## Integration Tests

The `integration-tests` module is scaffolded for end-to-end tests that depend on `plugin-core`.

```bash
./gradlew :integration-tests:test
```

## Running Everything

```bash
./gradlew check
```

This runs Java/Kotlin unit tests **and** JavaScript tests. Build failures in either will fail the check.

## Manual Plugin Testing

For manual smoke-testing of the installed plugin, see [QUICK-START.md](QUICK-START.md).
