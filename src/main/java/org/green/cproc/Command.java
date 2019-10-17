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

import java.util.function.BooleanSupplier;

public abstract class Command<R extends ErrorableResult> extends PoolableObject implements Execution<R> {
    protected final R result;

    private volatile boolean executed;

    // these fields are set by one single thread (owner) in the set() method
    private Cab cab; // the same thread reads this property in execute() and result()
    private BooleanSupplier closedMutex; // the worker's thread reads this in methods executed() after appropriate
    // membars happened in Cab structure (with strong CAS and volatile read) when this object was passed from
    // the original/owner thread to the worker

    protected Command(final R result) {
        this.result = result;
    }

    // called by the original thread
    final void set(final Cab cab, final BooleanSupplier closedMutex) {
        this.cab = cab;
        this.closedMutex = closedMutex;
    }

    // called by the original thread
    final void execute() throws ConsumerInterruptedException, InterruptedException {
        executed = false;

        cab.send(this);
    }

    // called by the worker's thread
    final void executed() {
        executed = true;

        final BooleanSupplier closed = closedMutex;
        synchronized (closed) {
            closed.notifyAll();
        }
    }

    public final R result() {
        return result;
    }

    @Override
    // optionally can be called by the original thread
    // all writes made by the worker will be seen after return from this method
    // because of at least 2 HB's in executed()/resultSync():
    // 1. volatile write/read (of executed field)
    // 2. synchronized on closedMutex
    public final R sync() throws InterruptedException {
        if (!executed) {
            final BooleanSupplier closed = closedMutex;
            synchronized (closed) {
                while (!executed) {
                    if (closed.getAsBoolean()) {
                        result.setError(new ConcurrentProcessClosedException());
                        break;
                    }

                    closed.wait();
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                " result=" + result;
    }
}