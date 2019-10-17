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
package org.green.jmh.cproc;

import org.green.cab.Cab;
import org.green.cab.CabBackingOff;
import org.green.cab.CabBlocking;
import org.green.cab.CabYielding;
import org.green.cproc.ConcurrentProcess;
import org.green.cproc.ConcurrentProcessListener;
import org.green.cproc.DefaultConcurrentProcess;
import org.green.cproc.DefaultExecutor;
import org.green.cproc.Future;
import org.green.cproc.Executor;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

public class ConcurrentProcessBenchmark {
    public static final int CAB_SIZE = 1_000;
    public static final int BACKING_OFF_MAX_SPINS = 1_000;
    public static final int BACKING_OFF_MAX_YIELDS = 10_000;

    abstract static class AbstractProcessSetup {
        ConcurrentProcess
                <LongEntry, Executor<LongEntry>, ConcurrentProcessListener<LongEntry, Executor<LongEntry>>> process;

        @Setup(Level.Trial)
        public void doSetup() {
            process = new DefaultConcurrentProcess<>(prepareCab(),
                    new DefaultExecutor<>(ExecuteCommandBenchmark.class.getSimpleName() + "'s executor"));
        }

        @TearDown(Level.Trial)
        public void doTearDown() throws Exception {
            process.close();
        }

        protected abstract Cab<LongEntry, Future> prepareCab();
    }

    @State(Scope.Benchmark)
    public static class CabBlockingBasedProcessSetup extends AbstractProcessSetup {
        @Override
        protected Cab<LongEntry, Future> prepareCab() {
            return new CabBlocking<>(CAB_SIZE);
        }
    }

    @State(Scope.Benchmark)
    public static class CabBackingOffBasedProcessSetup extends AbstractProcessSetup {
        @Override
        protected Cab<LongEntry, Future> prepareCab() {
            return new CabBackingOff<>(CAB_SIZE, BACKING_OFF_MAX_SPINS, BACKING_OFF_MAX_YIELDS);
        }
    }

    @State(Scope.Benchmark)
    public static class CabYieldingBasedProcessSetup extends AbstractProcessSetup {
        @Override
        protected Cab<LongEntry, Future> prepareCab() {
            return new CabYielding<>(CAB_SIZE);
        }
    }
}