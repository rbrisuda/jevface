package io.github.rbrisuda.jevface.examples.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.rbrisuda.jevface.Jevface;
import io.github.rbrisuda.jevface.test.AgentStub;
import io.github.rbrisuda.jevface.test.StubJudgmentEngine;
import org.junit.jupiter.api.Test;

/** Business rules tested with stubbed judgments: fast, deterministic, no API key. */
class TicketRouterTest {

    private final StubJudgmentEngine engine = new StubJudgmentEngine();
    private final TicketRouter router = new TicketRouter(Jevface.create(engine).client(SupportTriage.class));

    private AgentStub<SupportTriage> calmBillingTicket() {
        return engine.forAgent(SupportTriage.class)
                .noul(SupportTriage::isUrgent, 0.1)
                .choice(SupportTriage::department, Department.BILLING, 0.9)
                .scoreLevel(SupportTriage::frustration, Frustration.CALM, 0.9)
                .choice(SupportTriage::product, Product.CARDS, 0.9)
                .noul(SupportTriage::refundRequested, 0.0);
    }

    @Test
    void routesToTheChosenDepartment() {
        calmBillingTicket();

        RoutingDecision decision = router.route("Where can I download my invoice for the card fee?");

        assertThat(decision.queue()).isEqualTo("billing");
        assertThat(decision.priority()).isEqualTo(Priority.P3);
        assertThat(decision.product()).isEqualTo(Product.CARDS);
        assertThat(decision.notes()).isEmpty();
        assertThat(engine.requests()).singleElement()
                .satisfies(request -> assertThat(request.state()).contains("invoice"));
    }

    @Test
    void urgentAndAngryIsP1() {
        calmBillingTicket()
                .noul(SupportTriage::isUrgent, 0.97)
                .scoreLevel(SupportTriage::frustration, Frustration.ANGRY, 0.8);

        assertThat(router.route("Payouts failing for 3 days!!").priority()).isEqualTo(Priority.P1);
    }

    @Test
    void uncertainRoutingGoesToHumanReview() {
        calmBillingTicket().choice(SupportTriage::department, Department.TECHNICAL, 0.4);

        RoutingDecision decision = router.route("It does not work");

        assertThat(decision.queue()).isEqualTo(TicketRouter.HUMAN_REVIEW_QUEUE);
        assertThat(decision.department()).isEqualTo(Department.TECHNICAL);
        assertThat(decision.notes()).first().asString().startsWith("Routing is uncertain (40% technical)");
    }

    @Test
    void refundOutsideBillingAndUnknownProductAddNotes() {
        calmBillingTicket()
                .choice(SupportTriage::department, Department.SHIPPING, 0.9)
                .noul(SupportTriage::refundRequested, 0.8)
                .stated(SupportTriage::product, 0.1);

        RoutingDecision decision = router.route("My parcel never arrived, I want my money back");

        assertThat(decision.refundRequested()).isTrue();
        assertThat(decision.product()).isNull();
        assertThat(decision.notes()).containsExactly(
                "Refund requested: loop in billing", "No product identified: ask the customer");
    }

    @Test
    void lowConfidenceProductIsTreatedAsUnknown() {
        calmBillingTicket().choice(SupportTriage::product, Product.DASHBOARD, 0.3);

        assertThat(router.route("Something about a screen").product()).isNull();
    }
}
