package org.green.jmh.cproc;

import org.green.cab.Cab;
import org.green.cab.CabBackingOff;
import org.green.cab.CabBlocking;
import org.green.cab.CabYielding;
import org.green.cproc.ConcurrentProcessListener;
import org.green.cproc.DefaultConcurrentProcess;
import org.green.cproc.DefaultExecutor;
import org.green.cproc.Execution;
import org.green.cproc.Executor;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

public class ConcurrentProcessBenchmark {
    public static final int CAB_SIZE = 1000;
    public static final int BACKING_OFF_MAX_SPINS = 1000;
    public static final int BACKING_OFF_MAX_YIELDS = 10000;

    abstract static class AbstractProcessSetup {
        DefaultConcurrentProcess
                <LongEntry, Executor<LongEntry>, ConcurrentProcessListener<LongEntry, Executor<LongEntry>>> process;

        @Setup(Level.Trial)
        public void doSetup() {
            process = new DefaultConcurrentProcess<>(prepareCab(),
                    new DefaultExecutor(ExecuteCommandBenchmark.class.getSimpleName() + "'s executor"));
        }

        @TearDown(Level.Trial)
        public void doTearDown() throws InterruptedException {
            process.close();
        }

        protected abstract Cab<LongEntry, Execution> prepareCab();
    }

    @State(Scope.Benchmark)
    public static class CabBlockingBasedProcessSetup extends AbstractProcessSetup {
        @Override
        protected Cab<LongEntry, Execution> prepareCab() {
            return new CabBlocking<>(CAB_SIZE);
        }
    }

    @State(Scope.Benchmark)
    public static class CabBackingOffBasedProcessSetup extends AbstractProcessSetup {
        @Override
        protected Cab<LongEntry, Execution> prepareCab() {
            return new CabBackingOff<>(CAB_SIZE, BACKING_OFF_MAX_SPINS, BACKING_OFF_MAX_YIELDS);
        }
    }

    @State(Scope.Benchmark)
    public static class CabYieldingBasedProcessSetup extends AbstractProcessSetup {
        @Override
        protected Cab<LongEntry, Execution> prepareCab() {
            return new CabYielding<>(CAB_SIZE);
        }
    }
}