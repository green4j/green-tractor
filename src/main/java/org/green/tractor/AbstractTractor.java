/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2023 Anatoly Gudkov
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
package org.green.tractor;

import org.green.cab.Cab;
import org.green.cab.ConsumerInterruptedException;

import java.util.IdentityHashMap;
import java.util.function.BooleanSupplier;

public abstract class AbstractTractor
        <E extends Executor, L extends TractorListener<E>>
        implements Tractor<E, L> {

    private static final int SIMULTANEOUS_COMMANDS_PER_THREAD_MAX = 10;

    private static final ThreadLocal<IdentityHashMap<
            Class<? extends Command<?>>,
            SbsrConsatantObjectPool<? extends Command<?>>>> COMMAND_POOLS_THREAD_LOCAL
            = ThreadLocal.withInitial(() -> new IdentityHashMap<>());

    private final BooleanSupplier closedMutex = new BooleanSupplier() {
        @Override
        public boolean getAsBoolean() {
            return closed;
        }
    };

    private final Cab<Entry, Command<?>> cab;
    private final Executor executor;

    protected final ErrorHandler exceptionHandler;

    private final Worker worker;

    private boolean closing; // guarded by this
    private volatile boolean closed;

    protected AbstractTractor(final Cab<Entry, Command<?>> cab, final Executor executor) {
        this(cab, executor, new JulLoggingErrorHandler(AbstractTractor.class));
    }

    protected AbstractTractor(
            final Cab<Entry, Command<?>> cab,
            final Executor executor,
            final ErrorHandler exceptionHandler) {

        this.cab = cab;
        this.executor = executor;
        this.exceptionHandler = exceptionHandler;

        worker = new Worker();
        worker.start();
    }

    @Override
    public final <E extends Entry> EntrySender<E> newEntrySender(final Class<E> classOfEntry) {
        return new EntrySenderImpl<>(classOfEntry);
    }

    @Override
    public void closeSync(final long timeout) throws InterruptedException {
        synchronized (this) {
            if (closing) {
                return;
            }
            closing = true;
        }

        worker.interrupt();
        worker.join(timeout);
    }

    @Override
    public void closeSync() throws InterruptedException {
        closeSync(0);
    }

    @Override
    public void close() {
        try {
            closeSync(3_000);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected final <C extends Command<?>> C prepareCommand(final Class<C> ofClass) {
        final IdentityHashMap<Class<? extends Command<?>>, SbsrConsatantObjectPool<? extends Command<?>>> pools
                = COMMAND_POOLS_THREAD_LOCAL.get();

        SbsrConsatantObjectPool<C> pool = (SbsrConsatantObjectPool<C>) pools.get(ofClass); // unchecked
        if (pool == null) {
            pool = SbsrConsatantObjectPool.constructorBasedPool(ofClass, SIMULTANEOUS_COMMANDS_PER_THREAD_MAX);
            pools.put(ofClass, pool);
        }
        try {
            final C result = pool.borrow();
            result.set(cab, closedMutex);
            return result;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        }
    }

    protected final <C extends Command<?>> C executeCommand(final C command)
            throws TractorClosedException, InterruptedException {
        try {
            command.execute();
        } catch (final ConsumerInterruptedException e) {
            throw new TractorClosedException();
        }
        return command;
    }

    @SuppressWarnings("unchecked")
    private void releaseCommandExecution(final Command<?> execution) {
        execution.executed();
        execution.owner().release(execution); // unchecked
    }

    @SuppressWarnings("unchecked")
    private void releaseEntry(final Entry entry) {
        entry.owner().release(entry); // unchecked
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
                        final Command<?> ce = cab.getMessage();

                        try {
                            executor.executeCommand(ce);
                        } catch (final Exception e) {
                            exceptionHandler.onError(this, "An error while executing the command: " + ce, e);
                        }

                        releaseCommandExecution(ce);
                    } else {
                        final Entry entry = cab.getEntry(cs);

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

    private class EntrySenderImpl<E extends Entry> implements EntrySender<E>, EntryEnvelope<E> {
        private final SbsrConsatantObjectPool<E> entryPool;
        private final Thread creator;

        private E nextEntry;

        EntrySenderImpl(final Class<E> classOfEntry) {
            entryPool = SbsrConsatantObjectPool.constructorBasedPool(classOfEntry, cab.bufferSize());
            creator = Thread.currentThread();
        }

        @Override
        public EntryEnvelope<E> nextEnvelope() {
            checkCurrentThread();
            try {
                nextEntry = entryPool.borrow();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted", e);
            }
            return this;
        }

        @Override
        public E entry() {
            checkCurrentThread();
            return nextEntry;
        }

        @Override
        public void send() throws TractorClosedException, InterruptedException {
            checkCurrentThread();
            try {
                final long ps = cab.producerNext();
                cab.setEntry(ps, nextEntry);
                cab.producerCommit(ps);
            } catch (final ConsumerInterruptedException e) {
                throw new TractorClosedException();
            }
        }

        private void checkCurrentThread() {
            if (creator != Thread.currentThread()) {
                throw new IllegalStateException("Cannot be used from another thread");
            }
        }
    }
}