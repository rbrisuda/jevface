package io.github.rbrisuda.jevface.examples.support.model;

import java.util.List;

import io.github.rbrisuda.jevface.examples.support.agent.Department;
import io.github.rbrisuda.jevface.examples.support.agent.Frustration;
import io.github.rbrisuda.jevface.examples.support.agent.Product;
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
