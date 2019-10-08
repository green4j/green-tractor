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
package org.green.samples.cproc.myproc;

import org.green.cab.CabBlocking;
import org.green.cproc.CommandExecution;
import org.green.cproc.ConcurrentProcessListener;
import org.green.cproc.DefaultConcurrentProcess;
import org.green.cproc.EntryEnvelope;
import org.green.cproc.EntrySender;
import org.green.cproc.Execution;

public class MyProcess extends DefaultConcurrentProcess<MyEntry, MyExecutor, MyProcessListener> {
    public MyProcess(final String name) {
        super(new CabBlocking<>(100), new MyExecutor(name));
    }

    public Execution sum(final int a, final int b) {
        final CommandExecution<MySumCommand> result = prepareCommandExecution(MySumCommand.class);
        result.command().setA(a);
        result.command().setB(b);
        return result;
    }

    public Execution multiply(final int a, final int b) {
        final CommandExecution<MyMultiplyCommand> result = prepareCommandExecution(MyMultiplyCommand.class);
        result.command().setA(a);
        result.command().setB(b);
        return result;
    }

    public static void main(final String[] args) throws Exception {
        final MyProcess process = new MyProcess("My process");

        final MyProcessListener listener = new MyProcessListener() {
            @Override
            public void onSum(
                    final long executionId,
                    final MyExecutor executor,
                    final int result,
                    final Exception errorIfHappened) {
                System.out.println("Sum result=" + result + ", error=" + errorIfHappened);
            }

            @Override
            public void onMultiply(
                    final long executionId,
                    final MyExecutor executor,
                    final int result,
                    final Exception errorIfHappened) {
                System.out.println("Multiply result=" + result + ", error=" + errorIfHappened);
            }

            @Override
            public void onAddProcessListener(
                    final long executionId,
                    final MyExecutor executor,
                    final ConcurrentProcessListener addedListener,
                    final Exception errorIfHappened) {
                System.out.println("A listener " + addedListener + " added, error=" + errorIfHappened);
            }

            @Override
            public void onRemoveProcessListener(
                    final long executionId,
                    final MyExecutor executor,
                    final ConcurrentProcessListener removedListener,
                    final Exception errorIfHappened) {
                System.out.println("A listener " + removedListener + " removed, error=" + errorIfHappened);
            }

            @Override
            public void onStart(
                    final long executionId,
                    final MyExecutor executor,
                    final Exception errorIfHappened) {
                System.out.println("Started");
            }

            @Override
            public void onStop(
                    final long executionId,
                    final MyExecutor executor,
                    final Exception errorIfHappened) {
                System.out.println("Stopped");
            }
        };

        process.addListener(listener).executeSync();

        process.start().executeSync();

        final EntrySender<MyEntry> entrySender = process.newEntrySender(MyEntry.class);

        EntryEnvelope<MyEntry> envelope;

        envelope = entrySender.nextEnvelope();
        envelope.entry().value = 100L;
        envelope.send();

        envelope = entrySender.nextEnvelope();
        envelope.entry().value = 200L;
        envelope.send();

        process.sum(1, 2).executeSync();

        process.multiply(3, 4).executeSync();

        process.stop().executeSync();

        process.removeListener(listener).executeSync();

        process.close();
    }
}