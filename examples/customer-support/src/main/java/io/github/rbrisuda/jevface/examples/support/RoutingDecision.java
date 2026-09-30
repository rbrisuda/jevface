package io.github.rbrisuda.jevface.examples.support;

import java.util.List;
import org.jspecify.annotations.Nullable;

public record RoutingDecision(
        String queue,
        Priority priority,
        Department department,
        double departmentConfidence,
        Frustration frustration,
        @Nullable Product product,
        boolean refundRequested,
        List<String> notes,
        @Nullable String model,
        long latencyMillis) {}
