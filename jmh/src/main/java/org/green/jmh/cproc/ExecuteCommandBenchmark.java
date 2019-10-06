package org.green.jmh.cproc;

import org.green.cproc.ConcurrentProcessClosedException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

@Fork(3)
@Measurement(iterations = 3)
@Warmup(iterations = 3)
@BenchmarkMode(Mode.Throughput)
public class ExecuteCommandBenchmark extends ConcurrentProcessBenchmark {

    @Benchmark
    @Threads(1)
    public void oneStartExecuteCallerWithCabBlocking(
            final CabBlockingBasedProcessSetup processSetup)
            throws ConcurrentProcessClosedException, InterruptedException {

        processSetup.process.start().execute();
    }

    @Benchmark
    @Threads(2)
    public void twoStartExecuteCallersWithCabBlocking(
            final CabBlockingBasedProcessSetup processSetup)
            throws ConcurrentProcessClosedException, InterruptedException {

        processSetup.process.start().execute();
    }

    @Benchmark
    @Threads(1)
    public void oneStartExecuteCallerWithCabBackingOff(
            final CabBackingOffBasedProcessSetup processSetup)
            throws ConcurrentProcessClosedException, InterruptedException {

        processSetup.process.start().execute();
    }

    @Benchmark
    @Threads(2)
    public void twoStartExecuteCallersWithCabBackingOff(
            final CabBackingOffBasedProcessSetup processSetup)
            throws ConcurrentProcessClosedException, InterruptedException {

        processSetup.process.start().execute();
    }

    @Benchmark
    @Threads(1)
    public void oneStartExecuteCallerWithCabYielding(
            final CabYieldingBasedProcessSetup processSetup)
            throws ConcurrentProcessClosedException, InterruptedException {

        processSetup.process.start().execute();
    }

    @Benchmark
    @Threads(2)
    public void twoStartExecuteCallersWithCabYielding(
            final CabYieldingBasedProcessSetup processSetup)
            throws ConcurrentProcessClosedException, InterruptedException {

        processSetup.process.start().execute();
    }
}