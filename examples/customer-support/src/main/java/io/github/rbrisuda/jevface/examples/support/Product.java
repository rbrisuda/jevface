package io.github.rbrisuda.jevface.examples.support;

import io.github.rbrisuda.jevface.annotation.Option;

public enum Product {
    @Option(description = "Merchant payouts to bank accounts")
    PAYOUTS,
    @Option(description = "Debit and credit cards")
    CARDS,
    @Option(label = "mobile-app", description = "The iOS or Android app")
    MOBILE_APP,
    @Option(description = "The online dashboard")
    DASHBOARD
}
