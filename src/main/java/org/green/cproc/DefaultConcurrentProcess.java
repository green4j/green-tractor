package org.green.cproc;

import org.green.cab.Cab;

public class DefaultConcurrentProcess
        <E extends Entry, X extends Executor<E>, L extends ConcurrentProcessListener<E, X>>
        extends AbstractConcurrentProcess<E, X, L> {

    public DefaultConcurrentProcess(final Cab<E, Execution> cab, final Executor<E> executor) {
        super(cab, executor);
    }

    @Override
    public final Execution addListener(final L listener) {
        final CommandExecution<AddListener> result = prepareCommandExecution(AddListener.class);
        result.command().setListener(listener);
        return result;
    }

    @Override
    public final Execution removeListener(final L listener) {
        final CommandExecution<RemoveListener> result = prepareCommandExecution(RemoveListener.class);
        result.command().setListener(listener);
        return result;
    }

    @Override
    public final Execution start() {
        return prepareCommandExecution(Start.class);
    }

    @Override
    public final Execution stop() {
        return prepareCommandExecution(Stop.class);
    }
}