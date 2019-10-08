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
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractConcurrentProcess
        <E extends Entry, X extends Executor<E>, L extends ConcurrentProcessListener<E, X>>
        implements ConcurrentProcess<E, X, L> {

    private static final int SIMULTANEOUS_COMMAND_EXECUTIONS_PER_THREAD = 2; // second execute() call
    // will not return the control back until the fhe first one is executed completely and
    // returned back to the owner's pool

    private final ThreadLocal<IdentityHashMap<Class<? extends Command>, MbsrConsatantObjectPool<? extends Command>>>
            commandsPools = ThreadLocal.withInitial(() -> new IdentityHashMap<>());

    private final ThreadLocal<MbsrConsatantObjectPool<CommandExecutionImpl>> commandExecutionsPools =
            ThreadLocal.withInitial(() ->
                    new MbsrConsatantObjectPool<>(SIMULTANEOUS_COMMAND_EXECUTIONS_PER_THREAD, () ->
                            new CommandExecutionImpl()));

    private final Object executionSyncMutex = new Object();

    private final AtomicLong executionIdSequence = new AtomicLong();

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

    protected final <C extends Command> CommandExecution<C> prepareCommandExecution(final Class<C> ofCommandClass) {
        final C command = borrowCommand(ofCommandClass);
        final CommandExecutionImpl<C> result = commandExecutionsPools.get().borrow();
        result.setCommand(command);
        return result;
    }

    private void releaseCommandExecution(final CommandExecutionImpl execution) {
        final Command command = execution.command;
        command.owner().release(command);
        execution.owner().release(execution);
    }

    private void releaseEntry(final E entry) {
        entry.owner().release(entry);
    }

    @SuppressWarnings("unchecked")
    private <C extends Command> C borrowCommand(final Class<C> ofClass) {
        final IdentityHashMap<Class<? extends Command>, MbsrConsatantObjectPool<? extends Command>> pools
                = commandsPools.get();

        MbsrConsatantObjectPool<C> pool = (MbsrConsatantObjectPool<C>) pools.get(ofClass);
        if (pool == null) {
            pool = MbsrConsatantObjectPool.constructorBasedPool(ofClass, SIMULTANEOUS_COMMAND_EXECUTIONS_PER_THREAD);
            pools.put(ofClass, pool);
        }
        return pool.borrow();
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
                        final CommandExecutionImpl ce = (CommandExecutionImpl) cab.getMessage();

                        try {
                            executor.executeCommand(ce.id(), ce.command());
                        } catch (final Exception e) {
                            exceptionHandler.onError(this, "An error while executing the command: " + ce.command(), e);
                        }

                        ce.executed();
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

            synchronized (executionSyncMutex) {
                executionSyncMutex.notifyAll();
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

    private class CommandExecutionImpl<C extends Command> extends PoolableObject implements CommandExecution<C> {
        private static final int NO_EXECUTION_STATE = 0;
        private static final int ASYNC_EXECUTION_STATE = 1;
        private static final int SYNC_EXECUTION_STATE = 2;
        private static final int EXECUTED_STATE = 3;

        private static final long NO_ID = -1;
        public static final String POSSIBLE_LEAK_DETECTED_WHILE_EXECUTION_MESSAGE =
                "Possible leak detected. Previous execution wasn't finished successfully";

        private long id = NO_ID;
        private C command;

        private volatile int executedState = NO_EXECUTION_STATE;

        CommandExecutionImpl() {
        }

        @Override
        public C command() {
            return command;
        }

        @Override
        public long id() {
            return id;
        }

        @Override
        public void execute() throws ConcurrentProcessClosedException, InterruptedException {
            if (executedState != NO_EXECUTION_STATE) {
                throw new IllegalStateException(POSSIBLE_LEAK_DETECTED_WHILE_EXECUTION_MESSAGE);
            }

            executedState = ASYNC_EXECUTION_STATE;

            try {
                cab.send(this);
            } catch (final ConsumerInterruptedException e) {
                throw new ConcurrentProcessClosedException();
            }
        }

        @Override
        public void executeSync() throws ConcurrentProcessClosedException, InterruptedException {
            if (executedState != NO_EXECUTION_STATE) {
                throw new IllegalStateException(POSSIBLE_LEAK_DETECTED_WHILE_EXECUTION_MESSAGE);
            }

            executedState = SYNC_EXECUTION_STATE;

            try {
                cab.send(this);
            } catch (final ConsumerInterruptedException e) {
                throw new ConcurrentProcessClosedException();
            }

            if (executedState != EXECUTED_STATE) {
                synchronized (executionSyncMutex) {
                    while (true) {
                        if (executedState == EXECUTED_STATE) {
                            break;
                        }

                        if (closed) {
                            cleanupAndRelease();

                            throw new ConcurrentProcessClosedException();
                        }

                        executionSyncMutex.wait();
                    }
                }
            }

            cleanupAndRelease();
        }

        void setCommand(final C command) {
            this.command = command;
            id = executionIdSequence.incrementAndGet();
        }

        void executed() {
            if (executedState == ASYNC_EXECUTION_STATE) { // for execute() we release it in the worker's thread
                cleanupAndRelease();
                return;
            }

            // for executeSync() we will release it in the original thread
            // after it is notified
            executedState = EXECUTED_STATE;

            synchronized (executionSyncMutex) {
                executionSyncMutex.notifyAll();
            }
        }

        private void cleanupAndRelease() {
            id = NO_ID;
            executedState = 0;
            // a reference to the command will be removed in releaseCommandExecution()
            // after the command released
            releaseCommandExecution(this);
        }

        @Override
        void onReleased() {
            command = null;
        }
    }
}