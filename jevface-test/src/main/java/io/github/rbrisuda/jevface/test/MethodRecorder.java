package io.github.rbrisuda.jevface.test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/** Finds out which interface method a method reference such as {@code Agent::isUrgent} points to. */
final class MethodRecorder {

    private MethodRecorder() {
    }

    static <T> Method capture(Class<T> type, Function<? super T, ?> reference) {
        AtomicReference<@Nullable Method> invoked = new AtomicReference<>();
        Object recorder = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            if (invoked.get() == null) {
                invoked.set(method);
            }
            return defaultValue(method.getReturnType());
        });
        var unused = reference.apply(type.cast(recorder)); // only the invoked method matters
        Method method = invoked.get();
        if (method == null) {
            throw new IllegalArgumentException("Pass a method reference to a question method, e.g. Agent::isUrgent");
        }
        return method;
    }

    private static @Nullable Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == double.class) {
            return 0.0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        return null;
    }
}
