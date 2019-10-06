package org.green.jmh.cproc;

import org.green.cproc.ConcurrentProcessClosedException;
import org.green.cproc.EntryEnvelope;
import org.green.cproc.EntrySender;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

@Fork(3)
@Measurement(iterations = 3)
@Warmup(iterations = 3)
@BenchmarkMode(Mode.Throughput)
public class SendEntryBenchmark extends ConcurrentProcessBenchmark {
    @State(Scope.Thread)
    public static class EntrySenderSetup {
        private EntrySender<LongEntry> entrySender;

        public void doSetup(final AbstractProcessSetup processSetup) {
            entrySender = processSetup.process.newEntrySender(LongEntry.class);
        }
    }

    @Benchmark
    @Threads(1)
    public void singleSenderWithCabBlocking(
            final CabBlockingBasedProcessSetup processSetup,
            final EntrySenderSetup entrySetup)
            throws ConcurrentProcessClosedException, InterruptedException {

        if (entrySetup.entrySender == null) {
            entrySetup.doSetup(processSetup);
        }

        final EntryEnvelope<LongEntry> envelope = entrySetup.entrySender.nextEnvelope();
        envelope.entry().value = 100;
        envelope.send();
    }

    @Benchmark
    @Threads(2)
    public void twoSenderWithCabBlocking(
            final CabBlockingBasedProcessSetup processSetup,
            final EntrySenderSetup entrySetup)
            throws ConcurrentProcessClosedException, InterruptedException {

        if (entrySetup.entrySender == null) {
            entrySetup.doSetup(processSetup);
        }

        final EntryEnvelope<LongEntry> envelope = entrySetup.entrySender.nextEnvelope();
        envelope.entry().value = 100;
        envelope.send();
    }

    @Benchmark
    @Threads(1)
    public void singleSenderWithCabBackingOff(
            final CabBackingOffBasedProcessSetup processSetup,
            final EntrySenderSetup entrySetup)
            throws ConcurrentProcessClosedException, InterruptedException {

        if (entrySetup.entrySender == null) {
            entrySetup.doSetup(processSetup);
        }

        final EntryEnvelope<LongEntry> envelope = entrySetup.entrySender.nextEnvelope();
        envelope.entry().value = 100;
        envelope.send();
    }

    @Benchmark
    @Threads(2)
    public void twoSendersWithCabBackingOff(
            final CabBackingOffBasedProcessSetup processSetup,
            final EntrySenderSetup entrySetup)
            throws ConcurrentProcessClosedException, InterruptedException {

        if (entrySetup.entrySender == null) {
            entrySetup.doSetup(processSetup);
        }

        final EntryEnvelope<LongEntry> envelope = entrySetup.entrySender.nextEnvelope();
        envelope.entry().value = 100;
        envelope.send();
    }

    @Benchmark
    @Threads(1)
    public void singleSenderWithCabYielding(
            final CabYieldingBasedProcessSetup processSetup,
            final EntrySenderSetup entrySetup)
            throws ConcurrentProcessClosedException, InterruptedException {

        if (entrySetup.entrySender == null) {
            entrySetup.doSetup(processSetup);
        }

        final EntryEnvelope<LongEntry> envelope = entrySetup.entrySender.nextEnvelope();
        envelope.entry().value = 100;
        envelope.send();
    }

    @Benchmark
    @Threads(2)
    public void twoSendersWithCabYielding(
            final CabYieldingBasedProcessSetup processSetup,
            final EntrySenderSetup entrySetup)
            throws ConcurrentProcessClosedException, InterruptedException {

        if (entrySetup.entrySender == null) {
            entrySetup.doSetup(processSetup);
        }

        final EntryEnvelope<LongEntry> envelope = entrySetup.entrySender.nextEnvelope();
        envelope.entry().value = 100;
        envelope.send();
    }
}