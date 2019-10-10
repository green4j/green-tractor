package org.green.samples.cproc.myproc;

import org.green.cproc.ExecutionSelector;

public class MyExecutionSelector extends ExecutionSelector<MyEntry, MyExecutor, MyProcessListener>
        implements MyProcessListener {

    public MyExecutionSelector(final MyProcess process) {
        super(process);
    }

    @Override
    public void onSum(
            final long executionId,
            final MyExecutor executor,
            final int result,
            final Exception errorIfHappened) {

        if (skipExecution(executionId)) {
            return;
        }
        delegate.onSum(executionId, executor, result, errorIfHappened);
    }

    @Override
    public void onMultiply(
            final long executionId,
            final MyExecutor executor,
            final int result,
            final Exception errorIfHappened) {

        if (skipExecution(executionId)) {
            return;
        }
        delegate.onMultiply(executionId, executor, result, errorIfHappened);
    }
}
