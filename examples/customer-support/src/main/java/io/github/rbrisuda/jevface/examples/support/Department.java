package io.github.rbrisuda.jevface.examples.support;

import io.github.rbrisuda.jevface.annotation.Option;

/** The enum constants are the options Jev chooses from; descriptions become the choice criteria. */
public enum Department {
    @Option(description = "Payments, invoices, refunds, payouts, pricing disputes")
    BILLING,
    @Option(description = "Bugs, errors, outages, login problems, integrations")
    TECHNICAL,
    @Option(description = "Upgrades, new accounts, quotes, plan questions")
    SALES,
    @Option(description = "Delivery status, lost or damaged parcels, returns")
    SHIPPING
}
