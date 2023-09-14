package org.green.jmh.cab;

import org.green.cab.Cab;

public class NilConsumer extends Thread implements AutoCloseable {
    private final Cab<Object, Object> cab;

    public NilConsumer(final Cab<Object, Object> cab) {
        this.cab = cab;
    }

    @Override
    public void run() {
        try {
            while (true) {
                cab.consumerCommit(cab.consumerNext());
            }
        } catch (final InterruptedException ignore) {
            cab.consumerInterrupt();
        }
    }

    @Override
    public void close() {
        try {
            interrupt();
            join(3_000);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}