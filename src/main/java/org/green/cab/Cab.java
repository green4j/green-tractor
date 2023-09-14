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
package org.green.cab;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import static org.green.cab.Utils.ARRAY_PAD;
import static org.green.cab.Utils.INT_ARRAY_HANDLE;
import static org.green.cab.Utils.OBJECT_ARRAY_HANDLE;
import static org.green.cab.Utils.nextPowerOfTwo;

abstract class CabPad0 {
    protected long p00, p01, p02, p03, p04, p05, p06, p07;
    protected long p08, p09, p010, p011, p012, p013, p014, p015;
}

abstract class ConsumerSequence extends CabPad0 {
    protected static final AtomicLongFieldUpdater<ConsumerSequence> CONSUMER_SEQUENCE_UPDATER =
            AtomicLongFieldUpdater.newUpdater(ConsumerSequence.class, "consumerSequence");

    protected volatile long consumerSequence;
}

abstract class CabPad1 extends ConsumerSequence {
    protected long p10, p11, p12, p13, p14, p15, p16, p17;
    protected long p18, p19, p110, p111, p112, p113, p114, p115;
}

abstract class UncommittedProducersSequence extends CabPad1 {
    protected static final AtomicLongFieldUpdater<UncommittedProducersSequence> UNCOMMITTED_PRODUCER_SEQUENCE_UPDATER =
            AtomicLongFieldUpdater.newUpdater(UncommittedProducersSequence.class, "uncommittedProducersSequence");

    protected volatile long uncommittedProducersSequence;
}

abstract class CabPad2 extends UncommittedProducersSequence {
    protected long p20, p21, p22, p23, p24, p25, p26, p27;
    protected long p28, p29, p210, p211, p212, p213, p214, p215;
}

abstract class Message extends CabPad2 {
    protected static final AtomicReferenceFieldUpdater<Message, Object> MESSAGE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(Message.class, Object.class, "message");

    protected volatile Object message;
}

abstract class CabPad3 extends Message {
    protected long p30, p31, p32, p33, p34, p35, p36, p37;
    protected long p38, p39, p310, p311, p312, p313, p314, p315;
}

abstract class MessageCache extends CabPad3 {
    protected Object messageCache; // used by Consumer only, no any membars required
}

abstract class CabPad4 extends MessageCache {
    protected long p40, p41, p42, p43, p44, p45, p46, p47;
    protected long p48, p49, p410, p411, p412, p413, p414, p415;
}

abstract class NotifyAllRequired extends CabPad4 {
    protected static final AtomicIntegerFieldUpdater<NotifyAllRequired> MUTEX_SIGNAL_REQUIRED_HANDLE =
            AtomicIntegerFieldUpdater.newUpdater(NotifyAllRequired.class, "mutexSignalRequired");

    protected volatile int mutexSignalRequired;
}

abstract class CabPad5 extends NotifyAllRequired {
    protected long p50, p51, p52, p53, p54, p55, p56, p57;
    protected long p58, p59, p510, p511, p512, p513, p514, p515;
}

abstract class NumberOfOverloads extends CabPad5 {
    protected static final AtomicLongFieldUpdater<NumberOfOverloads> NUMBER_OF_OVERLOADS_UPDATER =
            AtomicLongFieldUpdater.newUpdater(NumberOfOverloads.class, "numberOfOverloads");

    protected volatile long numberOfOverloads;
}

abstract class CabPad6 extends NumberOfOverloads {
    protected long p50, p51, p52, p53, p54, p55, p56, p57;
    protected long p58, p59, p510, p511, p512, p513, p514, p515;
}

/**
 * This class presents a pair of CSP-style Channel and Ring Buffer (CAB - Channel And Buffer). This structure aims to be
 * a building block of concurrent data processing applications.
 * <p>
 * The pattern of usage is the following: a number of data producing threads put their data entries into
 * the Ring Buffer, some controlling threads send their commands as messages to the Channel and ONLY ONE single
 * worker thread (the consumer) consumes all incoming data entries and messages and processes them one by one.
 * This makes the flow of entries and messages linearized.
 * Such technique may be very useful, for example, to implement an event loop in a thread of the consumer.
 * <p>
 * For example:
 * <p>
 * An entry producer:
 * <pre>
 *      Cab cab = ...
 *
 *      long sequence;
 *      try {
 *          sequence = cab.producerNext();
 *      } catch (ConsumerInterruptedException e) {
 *          // happens if the consumer was interrupted by consumerInterrupt() and cannot process incoming entries
 *      } catch (InterruptedException e) {
 *          // happens if the current thread was interrupted
 *      }
 *
 *      Object entry = cab.getEntry(sequence);
 *
 *      // ... modify the entry ...
 *
 *      cab.producerCommit(sequence);
 * </pre>
 * <p>
 * A message sender:
 * <pre>
 *      Cab cab = ...
 *
 *      try {
 *          cab.send(message);,
 *      } catch (ConsumerInterruptedException e) {
 *          // happens if the consumer was interrupted by consumerInterrupt() and cannot process incoming messages
 *      } catch (InterruptedException e) {
 *          // happens if the current thread was interrupted
 *      }
 * </pre>
 * <p>
 * A consumer:
 * <pre>
 *      Cab cab = ...
 *
 *      long sequence;
 *      try {
 *          sequence = cab.consumerNext();
 *      } catch (InterruptedException e) {
 *          // happens if the current thread was interrupted
 *      }
 *
 *      if (sequence == Cab.MESSAGE_RECEIVED_SEQUENCE) { // a message can be read from the Channel
 *          Object message = cab.getMessage();
 *
 *          // ... process the message ...
 *
 *      } else { // an entry can be read from the Ring Buffer
 *          Object entry = cab.getEntry(sequence);
 *
 *          // ... process the entry ...
 *      }
 *
 *      cab.consumerCommit(sequence);
 * </pre>
 *
 * @param <E> types of entries in the Ring Buffer
 * @param <M> type of message in the Channel
 */
public abstract class Cab<E, M> extends CabPad6 {

    enum WaitingStaregy {
        BUSY_SPINNING, YIELDING, BACKING_OFF, BLOCKING
    }

    public static final long MESSAGE_RECEIVED_SEQUENCE = Long.MAX_VALUE;

    public static final long CONSUMER_INTERRUPTED_SEQUENCE = Long.MIN_VALUE;

    private static final long INITIAL_SEQUENCE = -1;

    private static final int BACKING_OFF_SPINNING_STATE = 0;
    private static final int BACKING_OFF_YIELDING_STATE = 1;
    private static final int BACKING_OFF_WAIT_ON_MUTEX_STATE = 2;

    private static final String BUFFER_SIZE_MUST_NOT_BE_LESS_THAN_1_MESSAGE = "bufferSize must not be less than 1";
    private static final String CONSUMER_WAS_CLOSED_MESSAGE = "Consumer was closed";

    private final long indexMask;

    private final int bufferSize;

    private final Object[] entries;
    private final int[] entryStates;

    private final WaitingStaregy waitingStaregy;

    private final Object mutex = new Object();

    private final long maxSpins;
    private final long maxYields;

    protected Cab(
            final int bufferSize,
            final WaitingStaregy waitingStaregy,
            final long maxSpins,
            final long maxYields,
            final Supplier<E> supplier) {

        if (bufferSize < 1) {
            throw new IllegalArgumentException(BUFFER_SIZE_MUST_NOT_BE_LESS_THAN_1_MESSAGE);
        }
        final int normalizedBufferSize = nextPowerOfTwo(bufferSize);

        this.indexMask = normalizedBufferSize - 1;

        this.bufferSize = normalizedBufferSize;
        this.entries = new Object[normalizedBufferSize + 2 * ARRAY_PAD];
        this.entryStates = new int[entries.length];

        this.waitingStaregy = waitingStaregy;
        this.maxSpins = maxSpins;
        this.maxYields = maxYields;

        CONSUMER_SEQUENCE_UPDATER.set(this, INITIAL_SEQUENCE);
        UNCOMMITTED_PRODUCER_SEQUENCE_UPDATER.set(this, INITIAL_SEQUENCE);

        if (supplier != null) {
            for (int i = 0; i < normalizedBufferSize; i++) {
                setEntry(i, supplier.get());
            }
        }

        MESSAGE_UPDATER.set(this, null);
    }

    /**
     * Returns actual Ring Buffer's size which is the next power of two of a value passed to the constructor.
     *
     * @return actual buffer size
     */
    public int bufferSize() {
        return bufferSize;
    }

    public long numberOfOverloads() {
        return NUMBER_OF_OVERLOADS_UPDATER.get(this);
    }

    /**
     * Returns a sequence for a producer thread to address the next available entry with getEntry(sequence),
     * setEntry(sequence) or removeEntry(sequence).
     *
     * @return sequence to address available entry
     * @throws ConsumerInterruptedException if the consumer was interrupted
     * @throws InterruptedException         if the current thread was interrupted
     */
    public long producerNext() throws ConsumerInterruptedException, InterruptedException {
        final long nextSequence = UNCOMMITTED_PRODUCER_SEQUENCE_UPDATER.incrementAndGet(this);

        boolean overloaded = false;

        while (true) {
            final long consumerSequence = CONSUMER_SEQUENCE_UPDATER.get(this);

            if (consumerSequence == CONSUMER_INTERRUPTED_SEQUENCE) {
                throw new ConsumerInterruptedException();
            }

            if (nextSequence - consumerSequence <= bufferSize) { // there is some free space in the buffer
                if (overloaded) {
                    NUMBER_OF_OVERLOADS_UPDATER.incrementAndGet(this);
                }
                break;
            }

            overloaded = true;

            // we are here because the buffer is full, so...
            LockSupport.parkNanos(1); // let's give a good chance to the consumer

            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }

        return nextSequence;
    }

    /**
     * Commits the sequence to make it available for the consumer thread to be read.
     *
     * @param sequence to be committed
     */
    public void producerCommit(final long sequence) {
        INT_ARRAY_HANDLE.setRelease(entryStates, arrayIndex(sequence), 1);

        switch (waitingStaregy) {
            case BUSY_SPINNING:
            case YIELDING:
                break;

            case BACKING_OFF:
            case BLOCKING:
                final Object mtx = mutex;

                synchronized (mtx) {
                    if (MUTEX_SIGNAL_REQUIRED_HANDLE.compareAndSet(this, 1, 0)) {
                        mtx.notifyAll();
                    }
                }
                break;

            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Sends a message to the Channel.
     *
     * @param msg a message to be sent
     * @throws ConsumerInterruptedException if the consumer was interrupted
     * @throws InterruptedException         if the current thread was interrupted
     */
    public void send(final M msg) throws ConsumerInterruptedException, InterruptedException {
        long consumerSequence = CONSUMER_SEQUENCE_UPDATER.get(this);
        if (consumerSequence == CONSUMER_INTERRUPTED_SEQUENCE) {
            throw new ConsumerInterruptedException();
        }

        switch (waitingStaregy) {
            case BUSY_SPINNING: {
                while (!MESSAGE_UPDATER.compareAndSet(this, null, msg)) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    consumerSequence = CONSUMER_SEQUENCE_UPDATER.get(this);
                    if (consumerSequence == CONSUMER_INTERRUPTED_SEQUENCE) {
                        throw new ConsumerInterruptedException();
                    }
                }
                break;
            }

            case YIELDING: {
                while (!MESSAGE_UPDATER.compareAndSet(this, null, msg)) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    consumerSequence = CONSUMER_SEQUENCE_UPDATER.get(this);
                    if (consumerSequence == CONSUMER_INTERRUPTED_SEQUENCE) {
                        throw new ConsumerInterruptedException();
                    }

                    Thread.yield();
                }
                break;
            }

            case BACKING_OFF: {
                int state = BACKING_OFF_SPINNING_STATE;
                long spins = 0;
                long yields = 0;

                final Object mtx = mutex;

                _endOfWaiting:
                while (!MESSAGE_UPDATER.compareAndSet(this, null, msg)) {

                    switch (state) {
                        case BACKING_OFF_SPINNING_STATE:
                            if (++spins > maxSpins) {
                                state = BACKING_OFF_YIELDING_STATE;
                            }
                            break;

                        case BACKING_OFF_YIELDING_STATE:
                            if (++yields > maxYields) {
                                state = BACKING_OFF_WAIT_ON_MUTEX_STATE;
                            } else {
                                Thread.yield();
                            }
                            break;

                        case BACKING_OFF_WAIT_ON_MUTEX_STATE:
                            synchronized (mtx) {
                                while (!MESSAGE_UPDATER.compareAndSet(this, null, msg)) {

                                    consumerSequence = CONSUMER_SEQUENCE_UPDATER.get(this);
                                    if (consumerSequence == CONSUMER_INTERRUPTED_SEQUENCE) {
                                        throw new ConsumerInterruptedException();
                                    }

                                    MUTEX_SIGNAL_REQUIRED_HANDLE.set(this, 1);
                                    mtx.wait();
                                }
                            }
                            break _endOfWaiting;

                        default:
                            throw new IllegalStateException();
                    }

                    // we are here while spinning and yielding

                    consumerSequence = CONSUMER_SEQUENCE_UPDATER.get(this);
                    if (consumerSequence == CONSUMER_INTERRUPTED_SEQUENCE) {
                        throw new ConsumerInterruptedException();
                    }

                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                }

                synchronized (mtx) {
                    if (MUTEX_SIGNAL_REQUIRED_HANDLE.compareAndSet(this, 1, 0)) {
                        mtx.notifyAll();
                    }
                }

                break;
            }

            case BLOCKING: {
                final Object mtx = mutex;
                synchronized (mtx) {
                    while (!MESSAGE_UPDATER.compareAndSet(this, null, msg)) {

                        consumerSequence = CONSUMER_SEQUENCE_UPDATER.get(this);
                        if (consumerSequence == CONSUMER_INTERRUPTED_SEQUENCE) {
                            throw new ConsumerInterruptedException();
                        }

                        MUTEX_SIGNAL_REQUIRED_HANDLE.set(this, 1);
                        mtx.wait();
                    }

                    if (MUTEX_SIGNAL_REQUIRED_HANDLE.compareAndSet(this, 1, 0)) {
                        mtx.notifyAll();
                    }
                }
                break;
            }

            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Returns a sequence for the consumer thread to address next available message or entry.
     *
     * @return sequence to be read. If the value is MESSAGE_RECEIVED_SEQUENCE, a message is ready to be read
     * with getMessage(), otherwise new entry can be accessed with getEntry(sequence).
     * <p>
     * This method can be called from one single consumer thread only.
     * @throws InterruptedException if the current thread was interrupted
     */
    public long consumerNext() throws InterruptedException {
        long nextConsumerSequence = consumerSequence;

        if (nextConsumerSequence == CONSUMER_INTERRUPTED_SEQUENCE) {
            throw new IllegalStateException(CONSUMER_WAS_CLOSED_MESSAGE, new ConsumerInterruptedException());
        }

        // check the message first
        Object msg;

        msg = MESSAGE_UPDATER.get(this);
        if (msg != null) {
            messageCache = msg;
            return MESSAGE_RECEIVED_SEQUENCE;
        }

        // continue with the buffer and the message again
        nextConsumerSequence++;

        final int stateIndex = arrayIndex(nextConsumerSequence);

        final int[] states = entryStates;

        switch (waitingStaregy) {
            case BUSY_SPINNING: {
                while (true) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    if ((int) INT_ARRAY_HANDLE.getVolatile(states, stateIndex) != 0) {
                        break;
                    }

                    msg = MESSAGE_UPDATER.get(this);
                    if (msg != null) {
                        messageCache = msg;
                        return MESSAGE_RECEIVED_SEQUENCE;
                    }
                }
                break;
            }

            case YIELDING: {
                while (true) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    if ((int) INT_ARRAY_HANDLE.getVolatile(states, stateIndex) != 0) {
                        break;
                    }

                    msg = MESSAGE_UPDATER.get(this);
                    if (msg != null) {
                        messageCache = msg;
                        return MESSAGE_RECEIVED_SEQUENCE;
                    }

                    Thread.yield();
                }
                break;
            }

            case BACKING_OFF: {
                int state = BACKING_OFF_SPINNING_STATE;
                long spins = 0;
                long yields = 0;

                _endOfBackingOff:
                while (true) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    if ((int) INT_ARRAY_HANDLE.getVolatile(states, stateIndex) != 0) {
                        break;
                    }

                    msg = MESSAGE_UPDATER.get(this);
                    if (msg != null) {
                        messageCache = msg;
                        return MESSAGE_RECEIVED_SEQUENCE;
                    }
                    Thread.yield();

                    switch (state) {
                        case BACKING_OFF_SPINNING_STATE:
                            if (++spins > maxSpins) {
                                state = BACKING_OFF_YIELDING_STATE;
                            }
                            break;

                        case BACKING_OFF_YIELDING_STATE:
                            if (++yields > maxYields) {
                                state = BACKING_OFF_SPINNING_STATE;
                            } else {
                                Thread.yield();
                            }
                            break;

                        case BACKING_OFF_WAIT_ON_MUTEX_STATE:
                            final Object mtx = mutex;

                            synchronized (mtx) {
                                while (true) {
                                    msg = MESSAGE_UPDATER.get(this);
                                    if (msg != null) {
                                        messageCache = msg;
                                        return MESSAGE_RECEIVED_SEQUENCE;
                                    }

                                    if ((int) INT_ARRAY_HANDLE.getVolatile(states, stateIndex) != 0) {
                                        break;
                                    }

                                    MUTEX_SIGNAL_REQUIRED_HANDLE.set(this, 1);
                                    mtx.wait();
                                }
                            }
                            break _endOfBackingOff;

                        default:
                            throw new IllegalStateException();
                    }

                }
                break;
            }

            case BLOCKING: {
                final Object mtx = mutex;

                if ((int) INT_ARRAY_HANDLE.getVolatile(states, stateIndex) != 0) {
                    break;
                }
                synchronized (mtx) {
                    while (true) {
                        msg = MESSAGE_UPDATER.get(this);
                        if (msg != null) {
                            messageCache = msg;
                            return MESSAGE_RECEIVED_SEQUENCE;
                        }

                        if ((int) INT_ARRAY_HANDLE.getVolatile(states, stateIndex) != 0) {
                            break;
                        }

                        MUTEX_SIGNAL_REQUIRED_HANDLE.set(this, 1);
                        mtx.wait();
                    }
                }
                break;
            }

            default:
                throw new IllegalStateException();
        }

        return nextConsumerSequence;
    }

    /**
     * Commits the current consumer's sequence to signal the consumer ir ready to process next message or next entry.
     * <p>
     * This method can be called from one single consumer thread only.
     *
     * @param sequence to be committed
     */
    public void consumerCommit(final long sequence) {
        if (sequence == MESSAGE_RECEIVED_SEQUENCE) {
            MESSAGE_UPDATER.set(this, null);
        } else {
            INT_ARRAY_HANDLE.set(entryStates, arrayIndex(sequence), 0);
            CONSUMER_SEQUENCE_UPDATER.set(this, sequence);
        }

        switch (waitingStaregy) {
            case BUSY_SPINNING:
            case YIELDING:
                break;

            case BACKING_OFF:
            case BLOCKING:
                final Object mtx = mutex;

                synchronized (mtx) {
                    if (MUTEX_SIGNAL_REQUIRED_HANDLE.compareAndSet(this, 1, 0)) {
                        mtx.notifyAll();
                    }
                }
                break;

            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Interrupts the consumer. Entry producers and message senders will get an {@link ConsumerInterruptedException}
     * after this call.
     */
    public void consumerInterrupt() {
        CONSUMER_SEQUENCE_UPDATER.set(this, CONSUMER_INTERRUPTED_SEQUENCE);

        final Object mtx = mutex;
        synchronized (mtx) {
            mtx.notifyAll();
        }
    }

    /**
     * Returns an entry from the position identified by the sequence from the Ring Buffer.
     *
     * @param sequence identifier of the entry's position
     * @return the entry
     */
    @SuppressWarnings("unchecked")
    public E getEntry(final long sequence) {
        return (E) OBJECT_ARRAY_HANDLE.getVolatile(entries, arrayIndex(sequence));
    }

    /**
     * Removes an entry from the position identified by the sequence from the Ring Buffer.
     *
     * @param sequence identifier of the entry's position
     * @return removed entry
     */
    @SuppressWarnings("unchecked")
    public E removeEntry(final long sequence) {
        final int entryIndex = arrayIndex(sequence);
        return (E) OBJECT_ARRAY_HANDLE.getAndSet(entries, entryIndex, null);
    }

    /**
     * Sets an entry to the position identified by the sequence in the Ring Buffer.
     *
     * @param sequence identifier of the entry's position
     * @param entry    to be set
     */
    public void setEntry(final long sequence, final E entry) {
        OBJECT_ARRAY_HANDLE.setVolatile(entries, arrayIndex(sequence), entry);
    }

    /**
     * Returns currently available message from the Channel
     *
     * @return a message
     */
    @SuppressWarnings("unchecked")
    public M getMessage() {
        return (M) messageCache;
    }

    private int arrayIndex(final long sequence) {
        return ARRAY_PAD + (int) (sequence & indexMask);
    }
}
