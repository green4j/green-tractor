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
package org.green.samples.tractor.mytractor;

import org.green.cab.CabBlocking;
import org.green.tractor.TractorClosedException;
import org.green.tractor.DefaultTractor;
import org.green.tractor.EntryEnvelope;
import org.green.tractor.EntrySender;
import org.green.tractor.Future;
import org.green.tractor.ListenerResult;
import org.green.tractor.VoidResult;

public class MyTractor extends DefaultTractor<MyExecutor, MyTractorListener> {
    public MyTractor(final String name) {
        super(new CabBlocking<>(100), new MyExecutor(name));
    }

    public Future<MyResult> sum(final int a, final int b)
            throws TractorClosedException, InterruptedException {

        final MySum result = prepareCommand(MySum.class);
        result.setA(a);
        result.setB(b);
        return executeCommand(result);
    }

    public Future<MyResult> multiply(final int a, final int b)
            throws TractorClosedException, InterruptedException {

        final MyMultiply result = prepareCommand(MyMultiply.class);
        result.setA(a);
        result.setB(b);
        return executeCommand(result);
    }

    public static void main(final String[] args) throws Exception {
        final MyTractor tractor = new MyTractor("My process");

        final MyTractorListener listener = new MyTractorListener() {

            @Override
            public void onAddProcessListener(final MyExecutor executor, final ListenerResult result) {
                System.out.println("My Listener: onAddProcessListener with the result=" + result);
            }

            @Override
            public void onRemoveProcessListener(final MyExecutor executor, final ListenerResult result) {
                System.out.println("My Listener: onRemoveProcessListener with the result=" + result);
            }

            @Override
            public void onStart(final MyExecutor executor, final VoidResult result) {
                System.out.println("My Listener: onStart with the result=" + result);
            }

            @Override
            public void onStop(final MyExecutor executor, final VoidResult result) {
                System.out.println("My Listener: onStop with the result=" + result);
            }

            @Override
            public void onSum(final MyExecutor executor, final MyResult result) {
                System.out.println("My Listener: onSum with the result=" + result);
            }

            @Override
            public void onMultiply(final MyExecutor executor, final MyResult result) {
                System.out.println("My Listener: onMultiply with the result=" + result);
            }
        };

        tractor.addListener(listener).sync();

        tractor.start().sync();

        final EntrySender<MyEntry> entrySender = tractor.newEntrySender(MyEntry.class);

        EntryEnvelope<MyEntry> envelope;

        envelope = entrySender.nextEnvelope();
        envelope.entry().value = 100L;
        envelope.send();

        envelope = entrySender.nextEnvelope();
        envelope.entry().value = 200L;
        envelope.send();

        System.out.println("Sum=" + tractor.sum(1, 2).sync().value());

        System.out.println("Mul=" + tractor.multiply(3, 4).sync().value());

        tractor.stop().sync();

        tractor.removeListener(listener).sync();

        tractor.close();
    }
}