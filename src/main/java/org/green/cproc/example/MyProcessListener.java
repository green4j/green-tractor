package org.green.cproc.example;

import org.green.cproc.ConcurrentProcessListener;

public class MyProcessListener implements ConcurrentProcessListener<MyEntry, MyExecutor> {
    @Override
    public void onAddProcessListener(
        final long executionId,
        final MyExecutor executor,
        final ConcurrentProcessListener listener,
        final Exception errorIfHappened) {

        System.out.println("Listener " + listener + " added");
    }

    @Override
    public void onRemoveProcessListener(
        final long executionId,
        final MyExecutor executor,
        final ConcurrentProcessListener listener,
        final Exception errorIfHappened) {

        System.out.println("Listener " + listener + " removed");
    }

    @Override
    public void onStart(
        final long executionId,
        final MyExecutor executor,
        final Exception errorIfHappened) {

        System.out.println("Started");
    }

    @Override
    public void onStop(
        final long executionId,
        final MyExecutor executor,
        final Exception errorIfHappened) {

        System.out.println("Stopped");
    }

    public void onSum(
        final long executionId,
        final MyExecutor executor,
        final int result,
        final Exception errorIfHappened) {

        System.out.println("Sum result=" + result);
    }

    public void onMultiply(
        final long executionId,
        final MyExecutor executor,
        final int result,
        final Exception errorIfHappened) {

        System.out.println("Multiply result=" + result);
    }
}
