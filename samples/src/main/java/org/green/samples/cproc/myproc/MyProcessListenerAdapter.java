package org.green.samples.cproc.myproc;

import org.green.cproc.ConcurrentProcessListenerAdapter;

public class MyProcessListenerAdapter extends ConcurrentProcessListenerAdapter<MyEntry, MyExecutor>
        implements org.green.samples.cproc.myproc.MyProcessListener {

    @Override
    public void onSum(
            final long executionId,
            final MyExecutor executor,
            final int result,
            final Exception errorIfHappened) {
    }

    @Override
    public void onMultiply(
            final long executionId,
            final MyExecutor executor,
            final int result,
            final Exception errorIfHappened) {
    }
}