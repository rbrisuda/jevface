# Example: customer support routing (Spring Boot)

A help desk that routes incoming tickets using the [`SupportTriage`](src/main/java/sk/rbr/jevface/examples/support/SupportTriage.java)
Jev agent. [`TicketRouter`](src/main/java/sk/rbr/jevface/examples/support/TicketRouter.java) contains the
business rules; it has no prompts and no JSON.

```bash
export TYPESAFE_API_KEY=...        # optional: TYPESAFE_BASE_URL for a local Laya server
./gradlew :examples:customer-support:bootRun

curl -X POST localhost:8080/tickets -H 'Content-Type: application/json' \
     -d '{"message":"Help! My payouts have been failing for 3 days."}'
```

```json
{"queue":"billing","priority":"P2","department":"BILLING","departmentConfidence":0.81,
 "frustration":"ANNOYED","product":"PAYOUTS","refundRequested":false,"notes":[],"model":"jev-1.13.0"}
```

Without a key the app still starts, and `POST /tickets` returns `503` with setup instructions.
Evaluate tickets from the command line with `--args="'first ticket' 'second ticket'"`.

The tests use `StubJudgmentEngine` and need no key.
