package org.green.cproc;

public interface EntryEnvelope<E extends Entry> {
    E entry();

    void send() throws ConcurrentProcessClosedException, InterruptedException;
}