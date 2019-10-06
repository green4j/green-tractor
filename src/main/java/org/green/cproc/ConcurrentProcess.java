package org.green.cproc;

public interface ConcurrentProcess<E extends Entry, X extends Executor<E>, L extends ConcurrentProcessListener<E, X>>
        extends AutoCloseable {

    EntrySender<E> newEntrySender(Class<E> classOfEntry);

    Execution addListener(L listener);

    Execution removeListener(L listener);

    Execution start();

    Execution stop();

}