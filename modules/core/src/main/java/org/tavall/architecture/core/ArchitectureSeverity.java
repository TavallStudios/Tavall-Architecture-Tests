package org.tavall.architecture.core;

public enum ArchitectureSeverity {
    BLOCKING,
    WARNING;

    public boolean blocking() {
        return this == BLOCKING;
    }
}
