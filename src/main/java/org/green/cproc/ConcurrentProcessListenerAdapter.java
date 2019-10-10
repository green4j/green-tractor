package org.green.cproc;

public class ConcurrentProcessListenerAdapter<E extends Entry, X extends Executor<E>>
        implements ConcurrentProcessListener<E, X> {

    public void onAddProcessListener(
            final long executionId,
            final X executor,
            final ConcurrentProcessListener addedListener) {
    }

    public void onRemoveProcessListener(
            final long executionId,
            final X executor,
            final ConcurrentProcessListener removedListener) {
    }

    public void onStart(
            final long executionId,
            final X executor,
            final Exception errorIfHappened) {
    }

    public void onStop(
            final long executionId,
            final X executor,
            final Exception errorIfHappened) {
    }
}