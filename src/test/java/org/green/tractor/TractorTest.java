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
package org.green.tractor;

import org.green.cab.CabBackingOff;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TractorTest {
    private static final boolean MAX_MODE = Boolean.getBoolean("org.green.tractor.test.max_mode");

    private static final int TEST_MULTIPLIER = MAX_MODE ? 20 : 1;
    private static final int TEST_TIMEOUT = 20 * TEST_MULTIPLIER;

    private static final int CAB_SIZE = 10_000;
    private static final int BACKING_OFF_MAX_SPINS = 1_000;
    private static final int BACKING_OFF_MAX_YIELDS = 10_000;

    @Test
    public void testExecuteSync() throws Exception {
        assertTimeout(ofSeconds(TEST_TIMEOUT), () -> {
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

            try (TestTractor process =
                         new TestTractor(
                                 new CabBackingOff<>(CAB_SIZE,
                                         BACKING_OFF_MAX_SPINS,
                                         BACKING_OFF_MAX_YIELDS), listener)) {

                final long startTime = System.nanoTime();

                process.start().sync();

                final long spentTime = (System.nanoTime() - startTime) / 1_000_000;

                final long jitterThreshold = sleep / 5; // useful with single CPU core test environment

                assertTrue(spentTime >= (sleep - jitterThreshold));
            }
        });
    }

    @Test
    public void oneWorkerScenarioTest() throws Exception {
        assertTimeout(ofSeconds(TEST_TIMEOUT), () -> {
            nWorkersScenarioTest(targetForOneWorker());
        });
    }

    @Test
    public void threeWorkersScenarioTest() throws Exception {
        assertTimeout(ofSeconds(TEST_TIMEOUT), () -> {
            nWorkersScenarioTest(targetForThreeWorkers());
        });
    }

    private ExecutionTarget targetForOneWorker() {
        return new ExecutionTarget(
                1,
                10_000_000 * TEST_MULTIPLIER,
                9_000_000 * TEST_MULTIPLIER,
                600_000 * TEST_MULTIPLIER,
                500_000 * TEST_MULTIPLIER,
                600_000 * TEST_MULTIPLIER,
                700_000 * TEST_MULTIPLIER);
    }

    private ExecutionTarget targetForThreeWorkers() {
        return new ExecutionTarget(
                3,
                4_000_000 * TEST_MULTIPLIER,
                5_000_000 * TEST_MULTIPLIER,
                400_000 * TEST_MULTIPLIER,
                500_000 * TEST_MULTIPLIER,
                600_000 * TEST_MULTIPLIER,
                700_000 * TEST_MULTIPLIER);
    }

    private void nWorkersScenarioTest(final ExecutionTarget target) throws Exception {
        try (TestTractor process =
                     new TestTractor(
                             new CabBackingOff<>(CAB_SIZE, BACKING_OFF_MAX_SPINS, BACKING_OFF_MAX_YIELDS), target)) {

            final TestScenario[] testScenarios = new TestScenario[target.scenarioTargets.length];

            for (int i = 0; i < testScenarios.length; i++) {
                testScenarios[i] = new TestScenario(
                        i,
                        process,
                        target.scenarioTargets[i]);
            }

            for (final TestScenario testScenario : testScenarios) {
                testScenario.start();
            }

            for (final TestScenario testScenario : testScenarios) {
                testScenario.join();
            }

            for (final TestScenario testScenario : testScenarios) {
                assertNull(testScenario.target.stExecutionError);
                assertNull(testScenario.scenarioError);
            }
        }

        target.reach();
    }

    class ExecutionTarget implements TestExecutor.Listener {
        private final int numberOfTestEntriesAPerScenario;
        private final int numberOfTestEntriesBPerScenario;
        private final int numberOfStartsPerScenario;
        private final int numberOfStopsPerScenario;
        private final int numberOfTestCommandsAPerScenario;
        private final int numberOfTestCommandsBPerScenario;

        private final ScenarioTarget[] scenarioTargets;

        private final CountDownLatch tTotalTestEntriesA;
        private final CountDownLatch tTotalTestEntriesB;
        private final CountDownLatch tTotalStarts;
        private final CountDownLatch tTotalStops;
        private final CountDownLatch tTotalTestCommandsA;
        private final CountDownLatch tTotalTestCommandsB;

        private final CountDownLatch stAddProcessListener;
        private final CountDownLatch stRemoveProcessListener;
        private final CountDownLatch stTestCommandA;
        private final CountDownLatch stTestCommandB;
        private final CountDownLatch stStart;
        private final CountDownLatch stStop;

        ExecutionTarget(
                final int numberOfWorkers,
                final int numberOfTestEntriesAPerScenario,
                final int numberOfTestEntriesBPerScenario,
                final int numberOfStartsPerScenario,
                final int numberOfStopsPerScenario,
                final int numberOfTestCommandsAPerScenario,
                final int numberOfTestCommandsBPerScenario) {

            this.numberOfTestEntriesAPerScenario = numberOfTestEntriesAPerScenario;
            this.numberOfTestEntriesBPerScenario = numberOfTestEntriesBPerScenario;
            this.numberOfStartsPerScenario = numberOfStartsPerScenario;
            this.numberOfStopsPerScenario = numberOfStopsPerScenario;
            this.numberOfTestCommandsAPerScenario = numberOfTestCommandsAPerScenario;
            this.numberOfTestCommandsBPerScenario = numberOfTestCommandsBPerScenario;

            scenarioTargets = new ScenarioTarget[numberOfWorkers];
            for (int i = 0; i < numberOfWorkers; i++) {
                scenarioTargets[i] = new ScenarioTarget();
            }

            final int numberOfTestEntriesATotal = numberOfTestEntriesAPerScenario * numberOfWorkers;
            final int numberOfTestEntriesBTotal = numberOfTestEntriesBPerScenario * numberOfWorkers;
            final int numberOfStartsTotal = numberOfStartsPerScenario * numberOfWorkers;
            final int numberOfStopsTotal = numberOfStopsPerScenario * numberOfWorkers;
            final int numberOfTestCommandsATotal = numberOfTestCommandsAPerScenario * numberOfWorkers;
            final int numberOfTestCommandsBTotal = numberOfTestCommandsBPerScenario * numberOfWorkers;

            tTotalTestEntriesA = new CountDownLatch(numberOfTestEntriesATotal);
            tTotalTestEntriesB = new CountDownLatch(numberOfTestEntriesBTotal);
            tTotalStarts = new CountDownLatch(numberOfStartsTotal);
            tTotalStops = new CountDownLatch(numberOfStopsTotal);
            tTotalTestCommandsA = new CountDownLatch(numberOfTestCommandsATotal);
            tTotalTestCommandsB = new CountDownLatch(numberOfTestCommandsBTotal);

            stAddProcessListener = new CountDownLatch((numberOfWorkers * (numberOfWorkers + 1)) / 2);
            stRemoveProcessListener = new CountDownLatch((int) stAddProcessListener.getCount());
            stTestCommandA = new CountDownLatch(numberOfTestCommandsATotal);
            stTestCommandB = new CountDownLatch(numberOfTestCommandsBTotal);
            stStart = new CountDownLatch(numberOfStartsTotal);
            stStop = new CountDownLatch(numberOfStopsTotal);
        }

        @Override
        public void onTestEntryAProcessed() {
            tTotalTestEntriesA.countDown();
        }

        @Override
        public void onTestEntryBProcessed() {
            tTotalTestEntriesB.countDown();
        }

        @Override
        public void onStartExecuted() {
            tTotalStarts.countDown();
        }

        @Override
        public void onStopExecuted() {
            tTotalStops.countDown();
        }

        @Override
        public void onTestCommandAExecuted() {
            tTotalTestCommandsA.countDown();
        }

        @Override
        public void onTestCommandBExecuted() {
            tTotalTestCommandsB.countDown();
        }

        void reach() throws InterruptedException {
            tTotalTestEntriesA.await();
            tTotalTestEntriesB.await();
            tTotalStarts.await();
            tTotalStops.await();
            tTotalTestCommandsA.await();
            tTotalTestCommandsB.await();
        }

        class ScenarioTarget implements TestTractorListener {
            private volatile Exception stExecutionError;

            public int numberOfTestEntriesA() {
                return numberOfTestEntriesAPerScenario;
            }

            public int numberOfTestEntriesB() {
                return numberOfTestEntriesBPerScenario;
            }

            public int numberOfStarts() {
                return numberOfStartsPerScenario;
            }

            public int numberOfStops() {
                return numberOfStopsPerScenario;
            }

            public int numberOfTestCommandsA() {
                return numberOfTestCommandsAPerScenario;
            }

            public int numberOfTestCommandsB() {
                return numberOfTestCommandsBPerScenario;
            }

            public void awaitAddProcessListener() throws InterruptedException {
                stAddProcessListener.await();
            }

            public void awaitAllCommands() throws InterruptedException {
                stTestCommandA.await();
                stTestCommandB.await();
                stStart.await();
                stStop.await();
            }

            public void awaitRemoveProcessListener() throws InterruptedException {
                stRemoveProcessListener.await();
            }

            @Override
            public void onAddProcessListener(final TestExecutor executor, final ListenerResult result) {
                if (result.error() != null) {
                    stExecutionError = result.error();
                }
                stAddProcessListener.countDown();
            }

            @Override
            public void onRemoveProcessListener(final TestExecutor executor, final ListenerResult result) {
                if (result.error() != null) {
                    stExecutionError = result.error();
                }
                stRemoveProcessListener.countDown();
            }

            @Override
            public void onTestCommandA(final TestExecutor executor, final TestResult result) {
                if (result.error() != null) {
                    stExecutionError = result.error();
                }
                stTestCommandA.countDown();
            }

            @Override
            public void onTestCommandB(final TestExecutor executor, final TestResult result) {
                if (result.error() != null) {
                    stExecutionError = result.error();
                }
                stTestCommandB.countDown();
            }

            @Override
            public void onStart(final TestExecutor executor, final VoidResult result) {
                if (result.error() != null) {
                    stExecutionError = result.error();
                }
                stStart.countDown();
            }

            @Override
            public void onStop(final TestExecutor executor, final VoidResult result) {
                if (result.error() != null) {
                    stExecutionError = result.error();
                }
                stStop.countDown();
            }
        }
    }

    class TestScenario extends Thread {
        private final int id;
        private final TestTractor process;
        private final ExecutionTarget.ScenarioTarget target;

        private volatile Exception scenarioError;

        TestScenario(
                final int id,
                final TestTractor process,
                final ExecutionTarget.ScenarioTarget target) {

            super(TestScenario.class.getSimpleName() + "#" + id);

            this.id = id;
            this.process = process;
            this.target = target;
        }

        @Override
        public void run() {
            try {
                process.addListener(target);

                target.awaitAddProcessListener();

                int testEntriesA = 0;
                int testEntriesB = 0;
                int starts = 0;
                int stops = 0;
                int testCommandsA = 0;
                int testCommandsB = 0;

                final EntrySender<TestEntryA> entryASender = process.newEntrySender(TestEntryA.class);
                final EntrySender<TestEntryB> entryBSender = process.newEntrySender(TestEntryB.class);

                int i = 0;

                while (testEntriesA < target.numberOfTestEntriesA() ||
                        testEntriesB < target.numberOfTestEntriesB() ||
                        starts < target.numberOfStarts() ||
                        stops < target.numberOfStops() ||
                        testCommandsA < target.numberOfTestCommandsA() ||
                        testCommandsB < target.numberOfTestCommandsB()) {

                    if (testEntriesA++ < target.numberOfTestEntriesA()) {
                        final EntryEnvelope<TestEntryA> envelope = entryASender.nextEnvelope();
                        envelope.entry().set(id, i);
                        envelope.send();
                    }

                    if (testEntriesB++ < target.numberOfTestEntriesB()) {
                        final EntryEnvelope<TestEntryB> envelope = entryBSender.nextEnvelope();
                        envelope.entry().set(id, i);
                        envelope.send();
                    }

                    if (starts++ < target.numberOfStarts()) {
                        process.start();
                    }

                    if (stops++ < target.numberOfStops()) {
                        process.stop();
                    }

                    if (testCommandsA++ < target.numberOfTestCommandsA()) {
                        process.testCommandA(id, i);
                    }

                    if (testCommandsB++ < target.numberOfTestCommandsB()) {
                        process.testCommandB(id, i);
                    }

                    i++;
                }

                target.awaitAllCommands();

                process.removeListener(target);

                target.awaitRemoveProcessListener();

            } catch (final Exception e) {
                e.printStackTrace(System.err);
                scenarioError = e;
            }
        }
    }
}