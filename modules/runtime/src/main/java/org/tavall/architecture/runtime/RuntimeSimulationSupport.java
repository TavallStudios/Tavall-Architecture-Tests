package org.tavall.architecture.runtime;

import java.util.ArrayDeque;
import java.util.Deque;

public final class RuntimeSimulationSupport implements AutoCloseable {
    private final Deque<AutoCloseable> resources = new ArrayDeque<>();

    public <T extends AutoCloseable> T track(T resource) {
        resources.push(resource);
        return resource;
    }

    @Override
    public void close() throws Exception {
        Exception failure = null;
        while (!resources.isEmpty()) {
            try {
                resources.pop().close();
            } catch (Exception exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
