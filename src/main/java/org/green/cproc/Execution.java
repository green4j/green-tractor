package org.green.cproc;

public interface Execution {
    long id();

    void execute() throws ConcurrentProcessClosedException, InterruptedException;

    void executeSync() throws ConcurrentProcessClosedException, InterruptedException;
}
