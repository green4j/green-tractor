package org.green.jmh.cproc;

import org.green.cab.Cab;
import org.green.cab.CabBackingOff;
import org.green.cab.CabBlocking;
import org.green.cab.CabYielding;
import org.green.cproc.ConcurrentProcessClosedException;
import org.green.cproc.ConcurrentProcessListener;
import org.green.cproc.DefaultConcurrentProcess;
import org.green.cproc.DefaultExecutor;
import org.green.cproc.Execution;
import org.green.cproc.Executor;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

public class ExecuteCommandBenchmark {
    public abstract static class AbstractProcessSetup {
        private Executor<LongEntry> executor;

        private ConcurrentProcessListener<LongEntry, Executor<LongEntry>> listener;

        DefaultConcurrentProcess
            <LongEntry, Executor<LongEntry>, ConcurrentProcessListener<LongEntry, Executor<LongEntry>>> process;

        @Setup(Level.Trial)
        public void doSetup() {
            executor = new DefaultExecutor(ExecuteCommandBenchmark.class.getSimpleName() + "'s exec") {
            };

            listener = new ConcurrentProcessListener<LongEntry, Executor<LongEntry>>() {
                @Override
                public void onAddProcessListener(
                    final long executionId,
                    final Executor<LongEntry> executor,
                    final ConcurrentProcessListener addedListener,
                    final Exception errorIfHappened) {
                }

                @Override
                public void onRemoveProcessListener(
                    final long executionId,
                    final Executor<LongEntry> executor,
                    final ConcurrentProcessListener removedListener,
                    final Exception errorIfHappened) {
                }

                @Override
                public void onStart(
                    final long executionId,
                    final Executor<LongEntry> executor,
                    final Exception errorIfHappened) {
                }

                @Override
                public void onStop(
                    final long executionId,
                    final Executor<LongEntry> executor,
                    final Exception errorIfHappened) {
                }
            };

            process = new DefaultConcurrentProcess<>(prepareCab(), executor);
        }

        @TearDown(Level.Trial)
        public void doTearDown() throws InterruptedException {
            process.close();
        }

        protected abstract Cab<LongEntry, Execution> prepareCab();
    }

    @State(Scope.Benchmark)
    public static class CabBlockingBasedProcessSetup extends SendEntryBenchmark.AbstractProcessSetup {
        @Override
        protected Cab<LongEntry, Execution> prepareCab() {
            return new CabBlocking<>(1000);
        }
    }

    @State(Scope.Benchmark)
    public static class CabBackingOffBasedProcessSetup extends SendEntryBenchmark.AbstractProcessSetup {
        @Override
        protected Cab<LongEntry, Execution> prepareCab() {
            return new CabBackingOff<>(1000, 100, 1000);
        }
    }

    @State(Scope.Benchmark)
    public static class CabYieldingBasedProcessSetup extends SendEntryBenchmark.AbstractProcessSetup {
        @Override
        protected Cab<LongEntry, Execution> prepareCab() {
            return new CabYielding<>(1000);
        }
    }

    @Benchmark
    @Threads(1)
    @Fork(3)
    @Measurement(iterations = 3)
    @Warmup(iterations = 3)
    @BenchmarkMode(Mode.Throughput)
    public void executeStartWithBlockingCab(
        final CabBlockingBasedProcessSetup processSetup)
        throws ConcurrentProcessClosedException, InterruptedException {

        processSetup.process.start().execute();
    }

    @Benchmark
    @Threads(1)
    @Fork(3)
    @Measurement(iterations = 3)
    @Warmup(iterations = 3)
    @BenchmarkMode(Mode.Throughput)
    public void executeStartWithBackingOffCab(
        final CabBackingOffBasedProcessSetup processSetup)
        throws ConcurrentProcessClosedException, InterruptedException {

        processSetup.process.start().execute();
    }

    @Benchmark
    @Threads(1)
    @Fork(3)
    @Measurement(iterations = 3)
    @Warmup(iterations = 3)
    @BenchmarkMode(Mode.Throughput)
    public void executeStartWithYieldingCab(
        final CabYieldingBasedProcessSetup processSetup)
        throws ConcurrentProcessClosedException, InterruptedException {

        processSetup.process.start().execute();
    }
}