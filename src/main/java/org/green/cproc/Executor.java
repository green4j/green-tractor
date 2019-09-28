package org.green.cproc;

public interface Executor<E extends Entry> {

    String name();

    void processEntry(E entry);

    void executeCommand(long executionId, Command command);

}
