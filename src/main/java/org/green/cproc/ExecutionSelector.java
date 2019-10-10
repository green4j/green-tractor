package org.green.cproc;

public class ExecutionSelector<E extends Entry, X extends Executor<E>, L extends ConcurrentProcessListener<E, X>>
        implements ConcurrentProcessListener<E, X> {

    private final ConcurrentProcess process;

    private long executionId;

    protected L delegate;

    public ExecutionSelector(final ConcurrentProcess process) {
        this.process = process;
    }

    public final void executeSync(
            final Execution execution,
            final L delegate)
            throws InterruptedException, ConcurrentProcessClosedException {

        try {
            process.addListener(this).executeSync();

            this.executionId = execution.id();
            this.delegate = delegate;

            execution.executeSync();
        } finally {
            process.removeListener(this).executeSync();
        }
    }

    protected final boolean skipExecution(final long executionId) {
        return this.executionId != executionId;
    }

    @Override
    public final void onAddProcessListener(
            final long executionId,
            final X executor,
            final ConcurrentProcessListener addedListener) {

        if (skipExecution(executionId)) {
            return;
        }
        delegate.onAddProcessListener(executionId, executor, addedListener);
    }

    @Override
    public final void onRemoveProcessListener(
            final long executionId,
            final X executor,
            final ConcurrentProcessListener removedListener) {

        if (skipExecution(executionId)) {
            return;
        }
        delegate.onRemoveProcessListener(executionId, executor, removedListener);
    }

    @Override
    public final void onStart(
            final long executionId,
            final X executor,
            final Exception errorIfHappened) {

        if (skipExecution(executionId)) {
            return;
        }
        delegate.onStart(executionId, executor, errorIfHappened);
    }

    @Override
    public final void onStop(
            final long executionId,
            final X executor,
            final Exception errorIfHappened) {

        if (skipExecution(executionId)) {
            return;
        }
        delegate.onStop(executionId, executor, errorIfHappened);
    }
}
