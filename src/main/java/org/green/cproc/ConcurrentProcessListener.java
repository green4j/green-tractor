package org.green.cproc;

public interface ConcurrentProcessListener<E extends Entry, X extends Executor<E>> {

    void onAddProcessListener(
            long executionId,
            X executor,
            ConcurrentProcessListener addedListener,
            Exception errorIfHappened);

    void onRemoveProcessListener(
            long executionId,
            X executor,
            ConcurrentProcessListener removedListener,
            Exception errorIfHappened);

    void onStart(
            long executionId,
            X executor,
            Exception errorIfHappened);

    void onStop(
            long executionId,
            X executor,
            Exception errorIfHappened);

}