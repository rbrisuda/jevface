package io.github.rbrisuda.jevface.examples.shopping;

import io.github.rbrisuda.jevface.annotation.Option;

public enum Topic {
    @Option(description = "Material, build quality, durability")
    QUALITY,
    @Option(label = "size-fit", description = "Size, fit, dimensions")
    SIZE_FIT,
    @Option(description = "Shipping speed, packaging, delivery problems")
    DELIVERY,
    @Option(description = "Price, value for money, discounts")
    PRICE,
    @Option(label = "customer-service", description = "Support, returns, how the shop treated the customer")
    CUSTOMER_SERVICE
}
