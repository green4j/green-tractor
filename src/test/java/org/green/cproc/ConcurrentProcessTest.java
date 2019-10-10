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

import org.green.cab.CabBackingOff;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConcurrentProcessTest {
    private static final boolean MAX_MODE = Boolean.getBoolean("org.green.cproc.text.max_mode");

    private static final int TEST_MULTIPLIER = MAX_MODE ? 20 : 1;
    private static final int TEST_TIMEOUT = 20 * TEST_MULTIPLIER;

    private static final int CAB_SIZE = 10_000;
    private static final int BACKING_OFF_MAX_SPINS = 1_000;
    private static final int BACKING_OFF_MAX_YIELDS = 10_000;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(TEST_TIMEOUT);

    @Test
    public void testExecuteSync() throws Exception {
        final int sleep = 2_000;

        final TestExecutor.Listener listener = new TestExecutor.Listener() {
            @Override
            public void onTestEntryAProcessed() {
            }

            @Override
            public void onTestEntryBProcessed() {
            }

            @Override
            public void onStartExecuted() {
                try {
                    Thread.sleep(sleep);
                } catch (final InterruptedException ignore) {
                }
            }

            @Override
            public void onStopExecuted() {
            }

            @Override
            public void onTestCommandAExecuted() {
            }

            @Override
            public void onTestCommandBExecuted() {
            }
        };

        try (TestProcess process =
                     new TestProcess(
                             new CabBackingOff<>(CAB_SIZE, BACKING_OFF_MAX_SPINS, BACKING_OFF_MAX_YIELDS), listener)) {

            final Execution execution = process.start();

            final long startTime = System.nanoTime();

            execution.sync();

            final long spentTime = (System.nanoTime() - startTime) / 1_000_000;

            assertTrue(spentTime >= sleep);
        }
    }

    @Test
    public void oneWorkerScenarioTest() throws Exception {
        nWorkersScenarioTest(targetForOneWorker());
    }

    @Test
    public void threeWorkersScenarioTest() throws Exception {
        nWorkersScenarioTest(targetForThreeWorkers());
    }

    private void nWorkersScenarioTest(final TestTarget target) throws Exception {
        try (TestProcess process =
                     new TestProcess(
                             new CabBackingOff<>(CAB_SIZE, BACKING_OFF_MAX_SPINS, BACKING_OFF_MAX_YIELDS), target)) {

            final TestScenarioGroup workerGroup = new TestScenarioGroup(process, target);

            workerGroup.start();

            workerGroup.join();
        }

        target.reach();
    }

    class TestTarget implements TestExecutor.Listener {
        private final int numberOfWorkers;
        private final int numberOfTestEntriesAPerWorker;
        private final int numberOfTestEntriesBPerWorker;
        private final int numberOfStartsPerWorker;
        private final int numberOfStopsPerWorker;
        private final int numberOfTestCommandsAPerWorker;
        private final int numberOfTestCommandsBPerWorker;

        private final int numberOfTestEntriesATotal;
        private final int numberOfTestEntriesBTotal;
        private final int numberOfStartsTotal;
        private final int numberOfStopsTotal;
        private final int numberOfTestCommandsATotal;
        private final int numberOfTestCommandsBTotal;

        private final CountDownLatch totalTestEntriesA;
        private final CountDownLatch totalTestEntriesB;
        private final CountDownLatch totalStarts;
        private final CountDownLatch totalStops;
        private final CountDownLatch totalTestCommandsA;
        private final CountDownLatch totalTestCommandsB;

        TestTarget(
                final int numberOfWorkers,
                final int numberOfTestEntriesAPerWorker,
                final int numberOfTestEntriesBPerWorker,
                final int numberOfStartsPerWorker,
                final int numberOfStopsPerWorker,
                final int numberOfTestCommandsAPerWorker,
                final int numberOfTestCommandsBPerWorker) {

            this.numberOfWorkers = numberOfWorkers;

            this.numberOfTestEntriesAPerWorker = numberOfTestEntriesAPerWorker;
            this.numberOfTestEntriesBPerWorker = numberOfTestEntriesBPerWorker;
            this.numberOfStartsPerWorker = numberOfStartsPerWorker;
            this.numberOfStopsPerWorker = numberOfStopsPerWorker;
            this.numberOfTestCommandsAPerWorker = numberOfTestCommandsAPerWorker;
            this.numberOfTestCommandsBPerWorker = numberOfTestCommandsBPerWorker;

            numberOfTestEntriesATotal = numberOfTestEntriesAPerWorker * numberOfWorkers;
            numberOfTestEntriesBTotal = numberOfTestEntriesBPerWorker * numberOfWorkers;
            numberOfStartsTotal = numberOfStartsPerWorker * numberOfWorkers;
            numberOfStopsTotal = numberOfStopsPerWorker * numberOfWorkers;
            numberOfTestCommandsATotal = numberOfTestCommandsAPerWorker * numberOfWorkers;
            numberOfTestCommandsBTotal = numberOfTestCommandsBPerWorker * numberOfWorkers;

            totalTestEntriesA = new CountDownLatch(numberOfTestEntriesATotal);
            totalTestEntriesB = new CountDownLatch(numberOfTestEntriesBTotal);
            totalStarts = new CountDownLatch(numberOfStartsTotal);
            totalStops = new CountDownLatch(numberOfStopsTotal);
            totalTestCommandsA = new CountDownLatch(numberOfTestCommandsATotal);
            totalTestCommandsB = new CountDownLatch(numberOfTestCommandsBTotal);
        }

        @Override
        public void onTestEntryAProcessed() {
            totalTestEntriesA.countDown();
        }

        @Override
        public void onTestEntryBProcessed() {
            totalTestEntriesB.countDown();
        }

        @Override
        public void onStartExecuted() {
            totalStarts.countDown();
        }

        @Override
        public void onStopExecuted() {
            totalStops.countDown();
        }

        @Override
        public void onTestCommandAExecuted() {
            totalTestCommandsA.countDown();
        }

        @Override
        public void onTestCommandBExecuted() {
            totalTestCommandsB.countDown();
        }

        void reach() throws InterruptedException {
            totalTestEntriesA.await();
            totalTestEntriesB.await();
            totalStarts.await();
            totalStops.await();
            totalTestCommandsA.await();
            totalTestCommandsB.await();
        }
    }

    class TestScenarioGroup {
        private final TestScenario[] testScenarios;

        TestScenarioGroup(final TestProcess process,
                          final TestTarget target) {

            testScenarios = new TestScenario[target.numberOfWorkers];

            final CountDownLatch beforeCommandSequence = new CountDownLatch(testScenarios.length);
            final CountDownLatch afterCommandSequence = new CountDownLatch(testScenarios.length);

            for (int i = 0; i < testScenarios.length; i++) {
                testScenarios[i] = new TestScenario(i, process, target, beforeCommandSequence, afterCommandSequence);
            }
        }

        void start() {
            for (final TestScenario testScenario : testScenarios) {
                testScenario.start();
            }
        }

        void join() throws InterruptedException {
            for (final TestScenario testScenario : testScenarios) {
                testScenario.join();
                testScenario.postCheck();
            }
        }
    }

    class TestScenario extends Thread implements TestProcessListener {
        private final int id;
        private final TestProcess process;
        private final TestTarget target;
        private final CountDownLatch beforeCommandSequence;
        private final CountDownLatch afterCommandSequence;

        private volatile Exception scenarioError;
        private volatile Exception executionError;

        private volatile int addMyListenerCount;
        private volatile int removeMyListenerCount;
        private volatile int testCommandACount;
        private volatile int testCommandBCount;
        private volatile int startCount;
        private volatile int stopCount;

        TestScenario(
                final int id,
                final TestProcess process,
                final TestTarget target,
                final CountDownLatch beforeCommandSequence,
                final CountDownLatch afterCommandSequence) {

            super(TestScenario.class.getSimpleName() + "#" + id);

            this.id = id;
            this.process = process;
            this.target = target;
            this.beforeCommandSequence = beforeCommandSequence;
            this.afterCommandSequence = afterCommandSequence;
        }

        @Override
        public void run() {
            try {
                process.addListener(this);

                // let's wait for all listeners
                // to count all the following signals
                beforeCommandSequence.countDown();
                beforeCommandSequence.await();

                int testEntriesA = 0;
                int testEntriesB = 0;
                int starts = 0;
                int stops = 0;
                int testCommandsA = 0;
                int testCommandsB = 0;

                final EntrySender<TestEntryA> entryASender = process.newEntrySender(TestEntryA.class);
                final EntrySender<TestEntryB> entryBSender = process.newEntrySender(TestEntryB.class);

                int i = 0;

                while (testEntriesA < target.numberOfTestEntriesAPerWorker ||
                        testEntriesB < target.numberOfTestEntriesBPerWorker ||
                        starts < target.numberOfStartsPerWorker ||
                        stops < target.numberOfStopsPerWorker ||
                        testCommandsA < target.numberOfTestCommandsAPerWorker ||
                        testCommandsB < target.numberOfTestCommandsBPerWorker) {

                    if (testEntriesA++ < target.numberOfTestEntriesAPerWorker) {
                        final EntryEnvelope<TestEntryA> envelope = entryASender.nextEnvelope();
                        envelope.entry().set(id, i);
                        envelope.send();
                    }

                    if (testEntriesB++ < target.numberOfTestEntriesBPerWorker) {
                        final EntryEnvelope<TestEntryB> envelope = entryBSender.nextEnvelope();
                        envelope.entry().set(id, i);
                        envelope.send();
                    }

                    if (starts++ < target.numberOfStartsPerWorker) {
                        process.start();
                    }

                    if (stops++ < target.numberOfStopsPerWorker) {
                        process.stop();
                    }

                    if (testCommandsA++ < target.numberOfTestCommandsAPerWorker) {
                        process.testCommandA(id, i);
                    }

                    if (testCommandsB++ < target.numberOfTestCommandsBPerWorker) {
                        process.testCommandB(id, i);
                    }

                    i++;
                }

                // let's wait for all commands
                // before a listener is removed
                afterCommandSequence.countDown();
                afterCommandSequence.await();

                process.removeListener(this);

            } catch (final Exception e) {
                e.printStackTrace(System.err);
                scenarioError = e;
            }
        }

        @Override
        public void onAddProcessListener(final TestExecutor executor, final ListenerResult result) {
            if (result.error() != null) {
                executionError = result.error();
            }
            if (result.listener() == this) {
                addMyListenerCount++; // OK, since happens in one single thread
            }
        }

        @Override
        public void onRemoveProcessListener(final TestExecutor executor, final ListenerResult result) {
            if (result.error() != null) {
                executionError = result.error();
            }
            if (result.listener() == this) {
                removeMyListenerCount++; // OK, since happens in one single thread
            }
        }

        @Override
        public void onTestCommandA(final TestExecutor executor, final TestResult result) {
            if (result.error() != null) {
                executionError = result.error();
            }
            testCommandACount++; // OK, since happens in one single thread
        }

        @Override
        public void onTestCommandB(final TestExecutor executor, final TestResult result) {
            if (result.error() != null) {
                executionError = result.error();
            }
            testCommandBCount++; // OK, since happens in one single thread
        }

        @Override
        public void onStart(final TestExecutor executor, final VoidResult result) {
            if (result.error() != null) {
                executionError = result.error();
            }
            startCount++; // OK, since happens in one single thread
        }

        @Override
        public void onStop(final TestExecutor executor, final VoidResult result) {
            if (result.error() != null) {
                executionError = result.error();
            }
            stopCount++; // OK, since happens in one single thread
        }

        void postCheck() {
            assertNull(scenarioError);
            assertNull(executionError);

            assertEquals(1, addMyListenerCount);
            assertEquals(1, removeMyListenerCount);
            assertEquals(target.numberOfTestCommandsATotal, testCommandACount);
            assertEquals(target.numberOfTestCommandsBTotal, testCommandBCount);
            assertEquals(target.numberOfStartsTotal, startCount);
            assertEquals(target.numberOfStopsTotal, stopCount);
        }
    }

    private TestTarget targetForOneWorker() {
        return new TestTarget(
                1,
                10_000_000 * TEST_MULTIPLIER,
                9_000_000 * TEST_MULTIPLIER,
                600_000 * TEST_MULTIPLIER,
                500_000 * TEST_MULTIPLIER,
                600_000 * TEST_MULTIPLIER,
                700_000 * TEST_MULTIPLIER);
    }

    private TestTarget targetForThreeWorkers() {
        return new TestTarget(
                3,
                4_000_000 * TEST_MULTIPLIER,
                5_000_000 * TEST_MULTIPLIER,
                400_000 * TEST_MULTIPLIER,
                500_000 * TEST_MULTIPLIER,
                600_000 * TEST_MULTIPLIER,
                700_000 * TEST_MULTIPLIER);
    }
}