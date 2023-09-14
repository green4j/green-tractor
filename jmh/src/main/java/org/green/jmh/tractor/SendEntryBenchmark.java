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
package org.green.jmh.tractor;

import org.green.tractor.TractorClosedException;
import org.green.tractor.EntryEnvelope;
import org.green.tractor.EntrySender;
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
public class SendEntryBenchmark extends TractorBenchmark {
    @State(Scope.Thread)
    public static class EntrySenderSetup {
        private EntrySender<LongEntry> entrySender;

        public void doSetup(final AbstractProcessSetup processSetup) {
            entrySender = processSetup.process.newEntrySender(LongEntry.class);
        }
    }

    @Benchmark
    @Threads(1)
    public void oneSenderWithCabBlocking(
            final CabBlockingBasedProcessSetup processSetup,
            final EntrySenderSetup entrySetup)
            throws TractorClosedException, InterruptedException {

        if (entrySetup.entrySender == null) {
            entrySetup.doSetup(processSetup);
        }

        final EntryEnvelope<LongEntry> envelope = entrySetup.entrySender.nextEnvelope();
        envelope.entry().value = 100;
        envelope.send();
    }

    @Benchmark
    @Threads(2)
    public void twoSendersWithCabBlocking(
            final CabBlockingBasedProcessSetup processSetup,
            final EntrySenderSetup entrySetup)
            throws TractorClosedException, InterruptedException {

        if (entrySetup.entrySender == null) {
            entrySetup.doSetup(processSetup);
        }

        final EntryEnvelope<LongEntry> envelope = entrySetup.entrySender.nextEnvelope();
        envelope.entry().value = 100;
        envelope.send();
    }

    @Benchmark
    @Threads(1)
    public void oneSenderWithCabBackingOff(
            final CabBackingOffBasedProcessSetup processSetup,
            final EntrySenderSetup entrySetup)
            throws TractorClosedException, InterruptedException {

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
            throws TractorClosedException, InterruptedException {

        if (entrySetup.entrySender == null) {
            entrySetup.doSetup(processSetup);
        }

        final EntryEnvelope<LongEntry> envelope = entrySetup.entrySender.nextEnvelope();
        envelope.entry().value = 100;
        envelope.send();
    }

    @Benchmark
    @Threads(1)
    public void oneSenderWithCabYielding(
            final CabYieldingBasedProcessSetup processSetup,
            final EntrySenderSetup entrySetup)
            throws TractorClosedException, InterruptedException {

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
            throws TractorClosedException, InterruptedException {

        if (entrySetup.entrySender == null) {
            entrySetup.doSetup(processSetup);
        }

        final EntryEnvelope<LongEntry> envelope = entrySetup.entrySender.nextEnvelope();
        envelope.entry().value = 100;
        envelope.send();
    }
}