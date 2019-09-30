package org.green.cproc;

import org.green.cab.Cab;
import org.green.cab.ConsumerInterruptedException;

import java.util.IdentityHashMap;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractConcurrentProcess
    <E extends Entry, X extends Executor<E>, L extends ConcurrentProcessListener<E, X>>
    implements ConcurrentProcess<E, X, L> {

    private final IdentityHashMap<Class<? extends PoolableObject>, ObjectPool<? extends PoolableObject>> objectPools =
        new IdentityHashMap<>(); // guarded by this

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

    protected AbstractConcurrentProcess(final Cab<E, Execution> cab, final Executor<E> executor, final ErrorHandler exceptionHandler) {
        this.cab = cab;
        this.executor = executor;
        this.exceptionHandler = exceptionHandler;

        objectPools.put(CommandExecutionImpl.class, new ObjectPool<>(() -> new CommandExecutionImpl()));

        worker = new Worker();
        worker.start();
    }

    @Override
    public final EntrySender<E> newEntrySender(final Class<E> classOfEntry) {
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
        synchronized (this) {
            final C command = borrowObject(ofCommandClass);
            final CommandExecutionImpl<C> result = borrowObject(CommandExecutionImpl.class);
            result.setCommand(command);
            return result;
        }
    }

    private void releaseCommandExecution(final CommandExecutionImpl execution) {
        synchronized (this) {
            execution.command.release();
            execution.command = null;
            execution.release();
        }
    }

    private void releaseEntry(final E entry) {
        synchronized (this) {
            entry.release();
        }
    }

    @SuppressWarnings("unchecked")
    private <C extends PoolableObject> C borrowObject(final Class<C> ofClass) {
        assert Thread.holdsLock(this);

        ObjectPool<C> pool = (ObjectPool<C>) objectPools.get(ofClass);
        if (pool == null) {
            pool = ObjectPool.constructorBasedPool(ofClass);
            objectPools.put(ofClass, pool);
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

    private class EntrySenderImpl implements EntrySender<E>, EntryEnvelope<E> {
        private final ObjectPool<E> entryPool;
        private final Thread creator;
        private E nextEntry;

        EntrySenderImpl(final Class<E> classOfEntry) {
            this.entryPool = ObjectPool.constructorBasedPool(classOfEntry);
            creator = Thread.currentThread();
        }

        @Override
        public EntryEnvelope<E> nextEnvelope() {
            checkCurrentThread();
            nextEntry = entryPool.borrow();
            return this;
        }

        @Override
        public E entry() {
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
    }
}