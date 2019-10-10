/**
 * MIT License
 * <p>
 * Copyright (c) 2019 Anatoly Gudkov
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE  LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.green.cproc;

import org.green.cab.Cab;
import org.green.cab.ConsumerInterruptedException;

import java.util.IdentityHashMap;
import java.util.function.BooleanSupplier;

public abstract class AbstractConcurrentProcess
        <E extends Entry, X extends Executor<E>, L extends ConcurrentProcessListener<E, X>>
        implements ConcurrentProcess<E, X, L> {

    private static final int SIMULTANEOUS_COMMAND_EXECUTIONS_PER_THREAD = 2;

    private final ThreadLocal<IdentityHashMap<Class<? extends Command>, MbsrConsatantObjectPool<? extends Command>>>
            commandExecutionsPools = ThreadLocal.withInitial(() -> new IdentityHashMap<>());

    private final BooleanSupplier closedMutex = new BooleanSupplier() {
        @Override
        public boolean getAsBoolean() {
            return closed;
        }
    };

    private final Cab<E, Execution> cab;
    private final Executor<E> executor;

    protected final ErrorHandler exceptionHandler;

    private final Worker worker;

    private boolean closing; // guarded by this
    private volatile boolean closed;

    protected AbstractConcurrentProcess(final Cab<E, Execution> cab, final Executor<E> executor) {
        this(cab, executor, new JulLoggingErrorHandler(AbstractConcurrentProcess.class));
    }

    protected AbstractConcurrentProcess(
            final Cab<E, Execution> cab,
            final Executor<E> executor,
            final ErrorHandler exceptionHandler) {

        this.cab = cab;
        this.executor = executor;
        this.exceptionHandler = exceptionHandler;

        worker = new Worker();
        worker.start();
    }

    @Override
    public final <EE extends E> EntrySender<EE> newEntrySender(final Class<EE> classOfEntry) {
        return new EntrySenderImpl(classOfEntry);
    }

    @Override
    public void close() throws InterruptedException {
        synchronized (this) {
            if (closing) {
                return;
            }
            closing = true;
        }

        worker.interrupt();
        worker.join();
    }

    protected final <C extends Command> C prepareCommand(final Class<C> ofClass) {
        final IdentityHashMap<Class<? extends Command>, MbsrConsatantObjectPool<? extends Command>> pools
                = commandExecutionsPools.get();

        MbsrConsatantObjectPool<C> pool = (MbsrConsatantObjectPool<C>) pools.get(ofClass);
        if (pool == null) {
            pool = MbsrConsatantObjectPool.constructorBasedPool(ofClass, SIMULTANEOUS_COMMAND_EXECUTIONS_PER_THREAD);
            pools.put(ofClass, pool);
        }
        final C result = pool.borrow();
        result.set(cab, closedMutex);
        return result;
    }

    protected final void executeCommand(final Command execution)
            throws ConcurrentProcessClosedException, InterruptedException {
        try {
            execution.execute();
        } catch (final ConsumerInterruptedException e) {
            throw new ConcurrentProcessClosedException();
        }
    }

    private void releaseCommandExecution(final Command execution) {
        execution.executed();
        execution.owner().release(execution);
    }

    private void releaseEntry(final E entry) {
        entry.owner().release(entry);
    }

    private class Worker extends Thread {
        Worker() {
            super("Worker@" + executor.name());
        }

        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            try {
                while (true) {
                    final long cs = cab.consumerNext();

                    if (cs == Cab.MESSAGE_RECEIVED_SEQUENCE) {
                        final Command ce = (Command) cab.getMessage();

                        try {
                            executor.executeCommand(ce);
                        } catch (final Exception e) {
                            exceptionHandler.onError(this, "An error while executing the command: " + ce, e);
                        }

                        releaseCommandExecution(ce);
                    } else {
                        final E entry = cab.getEntry(cs);

                        try {
                            executor.processEntry(entry);
                        } catch (final Exception e) {
                            exceptionHandler.onError(this, "An error while processing the entry: " + entry, e);
                        }

                        releaseEntry(entry);
                    }

                    cab.consumerCommit(cs);
                }
            } catch (final InterruptedException e) {
                // ignore
            } catch (final Throwable t) {
                exceptionHandler.onError(this, "An error in " + getName() + ": " + t.getLocalizedMessage(), t);
            }

            closed = true;

            cab.consumerInterrupt();

            synchronized (closedMutex) {
                closedMutex.notifyAll();
            }
        }
    }

    private class EntrySenderImpl<EE extends E> implements EntrySender<EE>, EntryEnvelope<EE> {
        private final MbsrConsatantObjectPool<EE> entryPool;
        private final Thread creator;

        private EE nextEntry;

        EntrySenderImpl(final Class<EE> classOfEntry) {
            entryPool = MbsrConsatantObjectPool.constructorBasedPool(classOfEntry, cab.bufferSize());
            creator = Thread.currentThread();
        }

        @Override
        public EntryEnvelope<EE> nextEnvelope() {
            checkCurrentThread();
            nextEntry = entryPool.borrow();
            return this;
        }

        @Override
        public EE entry() {
            checkCurrentThread();
            return nextEntry;
        }

        @Override
        public void send() throws ConcurrentProcessClosedException, InterruptedException {
            checkCurrentThread();
            try {
                final long ps = cab.producerNext();
                cab.setEntry(ps, nextEntry);
                cab.producerCommit(ps);
            } catch (final ConsumerInterruptedException e) {
                throw new ConcurrentProcessClosedException();
            }
        }

        private void checkCurrentThread() {
            if (creator != Thread.currentThread()) {
                throw new IllegalStateException("Cannot be used from another thread");
            }
        }
    }
}