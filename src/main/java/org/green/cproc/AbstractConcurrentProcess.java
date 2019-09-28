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

    protected final Logger logger;

    private final Worker worker;

    private boolean closing; // guarded by this
    private volatile boolean closed;

    protected AbstractConcurrentProcess(final Cab<E, Execution> cab, final Executor<E> executor) {
        this(cab, executor, new JulLogger(AbstractConcurrentProcess.class));
    }

    protected AbstractConcurrentProcess(final Cab<E, Execution> cab, final Executor<E> executor, final Logger logger) {
        this.cab = cab;
        this.executor = executor;
        this.logger = logger;

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

    @SuppressWarnings(("uncheked"))
    private void releaseCommandExecution(final CommandExecutionImpl execution) {
        synchronized (this) {
            execution.command().release();
            execution.release();
        }
    }

    private class Worker extends Thread {
        Worker() {
            super("Worker@" + executor.name());
        }

        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            while (true) {
                try {
                    final long cs = cab.consumerNext();

                    if (cs == Cab.MESSAGE_RECEIVED_SEQUENCE) {
                        final CommandExecutionImpl ce = (CommandExecutionImpl) cab.getMessage();

                        try {
                            executor.executeCommand(ce.id(), ce.command());
                        } catch (final Exception e) {
                            logger.error("An error while executing the command: " + ce.command(), e);
                        }

                        ce.executed();
                    } else {
                        final E entry = cab.getEntry(cs);

                        try {
                            executor.processEntry(entry);
                        } catch (final Exception e) {
                            logger.error("An error while processing the entry: " + entry, e);
                        }

                        entry.release();
                    }

                    cab.consumerCommit(cs);
                } catch (final InterruptedException e) {
                    closed = true;

                    cab.consumerInterrupt();

                    synchronized (executionSyncMutex) {
                        executionSyncMutex.notifyAll();
                    }
                    break;
                }
            }
        }
    }

    private class EntrySenderImpl implements EntrySender<E>, EntryEnvelope<E> {
        private final ObjectPool<E> entryPool;
        private E nextEntry;

        EntrySenderImpl(final Class<E> classOfEntry) {
            this.entryPool = ObjectPool.constructorBasedPool(classOfEntry);
        }

        @Override
        public EntryEnvelope<E> nextEnvelope() {
            nextEntry = entryPool.borrow();
            return this;
        }

        @Override
        public E entry() {
            return nextEntry;
        }

        @Override
        public void send() throws ConcurrentProcessClosedException, InterruptedException {
            try {
                final long ps = cab.producerNext();
                cab.setEntry(ps, nextEntry);
                cab.producerCommit(ps);
            } catch (final ConsumerInterruptedException e) {
                throw new ConcurrentProcessClosedException();
            }
        }
    }

    private class CommandExecutionImpl<C extends Command> extends PoolableObject implements CommandExecution<C> {
        private long id;
        private C command;

        private volatile boolean executed;

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
            try {
                cab.send(this);
            } catch (final ConsumerInterruptedException e) {
                throw new ConcurrentProcessClosedException();
            }
        }

        @Override
        public void executeSync() throws ConcurrentProcessClosedException, InterruptedException {
            executed = false;

            try {
                cab.send(this);
            } catch (final ConsumerInterruptedException e) {
                throw new ConcurrentProcessClosedException();
            }

            if (executed) {
                return;
            }

            synchronized (executionSyncMutex) {
                while (true) {
                    if (executed) {
                        return;
                    }

                    if (closed) {
                        throw new ConcurrentProcessClosedException();
                    }

                    executionSyncMutex.wait();
                }
            }
        }

        void setCommand(final C command) {
            this.command = command;
            executionIdSequence.incrementAndGet();
        }

        void executed() {
            releaseCommandExecution(this);

            command = null;

            executed = true;

            synchronized (executionSyncMutex) {
                executionSyncMutex.notifyAll();
            }
        }
    }
}