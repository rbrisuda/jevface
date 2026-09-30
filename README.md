# Jevface

**Typed judgments for Java. Declare the questions as an interface and let [Jev](https://typesafe.ai) answer them.**

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
![Java](https://img.shields.io/badge/java-21%2B-orange)
![Spring Boot](https://img.shields.io/badge/spring%20boot-4.1-6db33f)
![Status](https://img.shields.io/badge/status-experimental-yellow)

Jevface turns a plain Java interface into a set of typed questions for TypeSafe's **Jev**, a "System One"
judgment model. Jev never generates text. It returns a probability, one option out of N, or a position
on a scale. Jevface sends every question in a single request and gives you back an object that
**implements your interface**. You write no prompt templates, parse no JSON, and never use `Map<String, Object>`.

```java
@JevAgent
public interface SupportTriage {

    @NoulQuestion("Does the customer need help urgently?")
    boolean isUrgent();

    @ChoiceQuestion("Which team should handle this ticket?")
    Department department();                  // enum constants are the options

    @ScoreQuestion("How frustrated is the customer?")
    Frustration frustration();                // enum constants are the levels, lowest first

    default boolean escalate() {              // your own rules, in plain Java
        return isUrgent() && frustration() == Frustration.ANGRY;
    }
}
```

```java
SupportTriage triage = jevface.evaluate(SupportTriage.class, "Help! My payouts have been failing for 3 days.");

if (triage.escalate()) { ... }
switch (triage.department()) { case BILLING -> ...; case TECHNICAL -> ...; }
```

## Why

Using an LLM to make a decision usually means writing a prompt, asking for JSON, parsing it, validating
it, and hoping the model picked a value you know about. Jev removes the free-text part: it answers with
typed values and calibrated probabilities. Jevface removes the plumbing:

- **Your interface is the contract.** Options come from enum constants, and levels come from ordered enums.
  The model can only answer with values your code already handles, so `switch` statements are exhaustive.
- **One round trip.** All questions of an agent go to Jev in one `POST /v1/systemone`.
- **Confidence is first-class.** Read the probability of every option, set confidence floors, and get
  `Optional.empty()` when the text doesn't mention something at all.
- **Fails early.** Annotation mistakes are reported all at once, at startup in Spring Boot, and never on
  the first production request.
- **Testable without a model.** Stub answers with method references. No API key and no network needed.
- **Null-safe.** The codebase is `@NullMarked` with [JSpecify](https://jspecify.dev) and checked with NullAway.

## Getting started

> Jevface is not on Maven Central yet. Run `./gradlew publishToMavenLocal` and add `mavenLocal()` to
> your repositories.

**Spring Boot 4:**

```kotlin
dependencies {
    implementation("io.github.rbrisuda.jevface:jevface-spring-boot-starter:0.1.0-SNAPSHOT")
    testImplementation("io.github.rbrisuda.jevface:jevface-test:0.1.0-SNAPSHOT")
}
```

```java
@Service
class TicketRouter {

    private final JevClient<SupportTriage> triage;   // one bean per @JevAgent interface, injected by type

    TicketRouter(JevClient<SupportTriage> triage) {
        this.triage = triage;
    }

    Queue route(String ticket) {
        return Queue.of(triage.evaluate(ticket).department());
    }
}
```

**Plain Java:**

```kotlin
implementation("io.github.rbrisuda.jevface:jevface-typesafe:0.1.0-SNAPSHOT")
```

```java
Jevface jevface = Jevface.create(TypeSafeJudgmentEngine.fromEnvironment());
SupportTriage triage = jevface.evaluate(SupportTriage.class, ticketText);
```

**API key.** Get one at [typesafe.ai](https://typesafe.ai) and export it:

| Environment variable | |
|---|---|
| `TYPESAFE_API_KEY` | Required for real calls. |
| `TYPESAFE_BASE_URL` | Optional. Defaults to `https://api.typesafe.ai`, e.g. a local Laya server. |
| `TYPESAFE_DEFAULT_MODEL` | Optional. Defaults to `jev-latest`. |

In Spring Boot, `spring.ai.typesafe.api-key` (from the Spring AI TypeSafe starter) works too and takes
precedence. Without a key the application still starts, and evaluations fail with a message that
explains what to configure.

## Defining questions

The **annotation** chooses the kind of question. The **return type** chooses how you receive the answer.

| Annotation | Return type | You get |
|---|---|---|
| `@NoulQuestion` | `boolean` | `probability >= threshold` (default `0.5`) |
| | `double` | the probability, `0..1` |
| | `NoulResult` | probability, threshold and `isTrue()` |
| `@ChoiceQuestion` | `enum` | the chosen constant. `@Option` on a constant sets its label or description |
| | `String` | the chosen label, from `options = {@Option(label = "...")}` |
| | `ChoiceResult<E>` | value, confidence and the probability of every option |
| `@ScoreQuestion` | `enum` / `String` | the nearest level, from the enum constants or `levels = {...}` |
| | `int` | the nearest level index |
| | `double` | the continuous score (`0` = first level) |
| | `ScoreResult<E>` | score, level, index, confidence and probabilities |
| choice or score | `Optional<...>` | empty if the `stated` gate says it isn't mentioned, or if confidence is below `minConfidence` |
| none | `default` method | never sent to Jev; your logic on top of the answers |

A richer example:

```java
@JevAgent(name = "support-triage", minConfidence = 0.3)
public interface SupportTriage {

    @NoulQuestion(value = "Does the customer need help urgently?",
                  whenTrue = "Outage, blocked work, money at risk or a deadline",
                  whenFalse = "No urgency expressed")
    boolean isUrgent();

    @ChoiceQuestion("Which team should handle this ticket?")
    ChoiceResult<Department> department();

    @ChoiceQuestion(value = "Which product is the ticket about?",
                    stated = "Does the customer mention a specific product?",
                    minConfidence = 0.5)
    Optional<Product> product();

    @NoulQuestion(value = "Is this spam?", threshold = 0.8)
    boolean spam();

    default boolean needsHumanReview() {
        return department().confidence() < 0.6;
    }
}

public enum Department {
    @Option(description = "Payments, invoices, refunds") BILLING,
    @Option(description = "Bugs, outages, login problems") TECHNICAL,
    @Option(label = "sales-team", description = "Upgrades and new accounts") SALES
}
```

- **Question keys** are the method name in snake_case, with a leading `get` removed (`isUrgent` → `is_urgent`).
  Override one with `key = "..."`.
- **Confidence:** below `minConfidence`, an `Optional` is empty and any other return type throws
  `LowConfidenceException` when read. Set it per agent, or per question.
- **Inspect without calling:** `Jevface.describe(SupportTriage.class)` validates the interface and shows the
  exact questions that will be sent.
- **Metadata:** `Jevface.evaluationOf(triage)` returns the model, request id, token usage, latency and raw answers.
- **Batches:** `jevface.client(SupportTriage.class).evaluateAll(texts)` evaluates many texts concurrently.

## Testing

`jevface-test` provides a type-safe stub engine. A question stubbed with the wrong kind of answer fails
immediately.

```java
StubJudgmentEngine engine = new StubJudgmentEngine();
engine.forAgent(SupportTriage.class)
      .noul(SupportTriage::isUrgent, 0.95)
      .choice(SupportTriage::department, Department.BILLING, 0.81)
      .stated(SupportTriage::product, 0.1);        // "no product mentioned"

SupportTriage triage = Jevface.create(engine).evaluate(SupportTriage.class, "any text");
```

In Spring Boot tests, declare the stub as a bean and it replaces the real engine:

```java
@TestConfiguration
class StubJev {
    @Bean
    StubJudgmentEngine stubJudgmentEngine() { ... }
}
```

## Modules

| Artifact | Description |
|---|---|
| `jevface-core` | Annotations, `Jevface`, the interface proxy and the `JudgmentEngine` SPI. Depends only on JSpecify. |
| `jevface-typesafe` | `TypeSafeJudgmentEngine`, built on the [Spring AI Community TypeSafe SDK](https://github.com/spring-ai-community/typesafe-java-sdk). |
| `jevface-spring-boot-starter` | Auto-configuration, `JevClient<Agent>` beans and `@EnableJevClients`. |
| `jevface-test` | `StubJudgmentEngine` for tests without an API key. |

`JudgmentEngine` is a one-method SPI, so other backends (a local Laya server, a cache, a recording proxy)
can be plugged in.

## Examples

- [`examples/customer-support`](examples/customer-support): a Spring Boot help desk that routes tickets
  over REST, with priorities, refund handling and a human-review fallback.
- [`examples/shopping-agent`](examples/shopping-agent): plain Java batch analysis of product reviews,
  with no Spring.

## Try it on a real model

The quickest way to experiment is to run the `customer-support` example and send it tickets with
`curl`. It needs a Jev-compatible model behind it. You can use either of these:

**Option A: hosted Jev.** Get an API key at [typesafe.ai](https://typesafe.ai).

```bash
export TYPESAFE_API_KEY=<your key>
```

**Option B: a local model, no account needed.** [Laya](https://github.com/NandhaKishorM/laya) is an
open-source (Apache 2.0) model that serves the same `/v1/systemone` API. Its checkpoints have 322M–421M
parameters. The first start downloads about 1–2 GB, including PyTorch. It runs on a laptop CPU or
Apple GPU, but it is less accurate than Jev.

With [uv](https://docs.astral.sh/uv/):

```bash
uv venv --python 3.10 && uv pip install "laya[serve]"

LAYA_MODELS=multilingual LAYA_PRELOAD=1 \
LAYA_API_KEY=local-test LAYA_PORT=8002 .venv/bin/laya-serve    # add LAYA_DEVICE=mps on Apple Silicon

curl -s localhost:8002/health      # ready once the checkpoint is loaded
```

`multilingual` is the smallest checkpoint and handles non-English tickets. Use `english` for
slightly better English accuracy. Then, in the terminal where you will start the app:

```bash
export TYPESAFE_BASE_URL=http://localhost:8002
export TYPESAFE_API_KEY=local-test   # must match LAYA_API_KEY
```

**Start the example app** (port 8080):

```bash
./gradlew :examples:customer-support:bootRun
```

**Send a ticket:**

```bash
curl -s -X POST localhost:8080/tickets \
  -H 'Content-Type: application/json' \
  -d '{"message": "Help! My payouts have been failing for 3 days and I cannot pay my suppliers. Fix this NOW."}' | jq
```

The response is the routing decision built from the typed answers:

```json
{
  "queue": "billing",
  "priority": "P1",
  "department": "BILLING",
  "departmentConfidence": 0.93,
  "frustration": "ANGRY",
  "product": "PAYOUTS",
  "refundRequested": false,
  "notes": [],
  "model": "jev-...",
  "latencyMillis": 41
}
```

The exact values depend on the model. Other messages to try:

| Message | What to look for |
|---|---|
| `My parcel never arrived and tracking has not moved for two weeks. I want my money back.` | `refundRequested: true` and a note to loop in billing |
| `It doesn't work.` | queue `human-review` (low department confidence) and a "no product identified" note |
| `We are growing to 50 people. What would an upgrade to the business plan cost us?` | the sales queue, low priority |
| `This is the third time your iOS app logged me out and lost my data. I'm cancelling tomorrow!` | high frustration, product `MOBILE_APP` |
| `Dobrý deň, od včera sa neviem prihlásiť do mobilnej aplikácie.` | a non-English ticket; with Laya it needs the `multilingual` checkpoint |

If the app starts without a key, `/tickets` answers `503` with a problem detail that explains what
to configure. The same setup works for the plain Java example:

```bash
./gradlew :examples:shopping-agent:run --args="'Great fit' 'Too small, returned it'"
```

To try your own questions, edit
[`SupportTriage`](examples/customer-support/src/main/java/sk/rbr/jevface/examples/support/SupportTriage.java)
and restart the app. No other code changes are needed.

## Building

Requires JDK 25 for the build. The published libraries target Java 21.

```bash
./gradlew build                  # compile with Error Prone + NullAway, run all tests (no key needed)
./gradlew publishToMavenLocal    # install io.github.rbrisuda.jevface:*:0.1.0-SNAPSHOT into ~/.m2
TYPESAFE_API_KEY=... ./gradlew liveTest   # tests tagged "live" call the real API
```

## Roadmap

- Multi-select questions: `Set<Enum>` as one noul per constant.
- Action dispatch: let Jev choose which `@Action` method of a bean to invoke.
- Compile-time validation with an annotation processor.
- Publishing to Maven Central.

## License

[Apache License 2.0](LICENSE).

Jevface is an independent project. It is not affiliated with or endorsed by TypeSafe AI. Jev is a
product of TypeSafe AI.
