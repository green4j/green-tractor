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

import org.green.cab.Cab;

public class DefaultTractor
        <E extends Entry, X extends Executor<E>, L extends TractorListener<E, X>>
        extends AbstractTractor<E, X, L> {

    public DefaultTractor(final Cab<E, Future> cab, final Executor<E> executor) {
        super(cab, executor);
    }

    @Override
    public final Future<ListenerResult> addListener(final L listener)
            throws TractorClosedException, InterruptedException {

        final AddListener result = prepareCommand(AddListener.class);
        result.setListener(listener);
        return executeCommand(result);
    }

    @Override
    public final Future<ListenerResult> removeListener(final L listener)
            throws TractorClosedException, InterruptedException {

        final RemoveListener result = prepareCommand(RemoveListener.class);
        result.setListener(listener);
        return executeCommand(result);
    }

    @Override
    public final Future<VoidResult> start() throws TractorClosedException, InterruptedException {
        return executeCommand(prepareCommand(Start.class));
    }

    @Override
    public final Future<VoidResult> stop() throws TractorClosedException, InterruptedException {
        return executeCommand(prepareCommand(Stop.class));
    }
}