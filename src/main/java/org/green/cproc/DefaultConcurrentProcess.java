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

import org.green.cab.Cab;

public class DefaultConcurrentProcess
        <E extends Entry, X extends Executor<E>, L extends ConcurrentProcessListener<E, X>>
        extends AbstractConcurrentProcess<E, X, L> {

    public DefaultConcurrentProcess(final Cab<E, Execution> cab, final Executor<E> executor) {
        super(cab, executor);
    }

    @Override
    public final Execution addListener(final L listener) {
        final CommandExecution<AddListener> result = prepareCommandExecution(AddListener.class);
        result.command().setListener(listener);
        return result;
    }

    @Override
    public final Execution removeListener(final L listener) {
        final CommandExecution<RemoveListener> result = prepareCommandExecution(RemoveListener.class);
        result.command().setListener(listener);
        return result;
    }

    @Override
    public final Execution start() {
        return prepareCommandExecution(Start.class);
    }

    @Override
    public final Execution stop() {
        return prepareCommandExecution(Stop.class);
    }
}