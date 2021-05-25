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

public class TestTractor extends DefaultTractor<TestEntry, TestExecutor, TestTractorListener> {
    public TestTractor(final Cab<TestEntry, Future> cab, final TestExecutor.Listener listener) {
        super(cab, new TestExecutor(listener));
    }

    public Future<TestResult> testCommandA(final int id, final int value)
            throws TractorClosedException, InterruptedException {

        final TestCommandA result = prepareCommand(TestCommandA.class);
        result.set(id, value);
        executeCommand(result);
        return result;
    }

    public Future<TestResult> testCommandB(final int id, final int value)
            throws TractorClosedException, InterruptedException {

        final TestCommandB result = prepareCommand(TestCommandB.class);
        result.set(id, value);
        executeCommand(result);
        return result;
    }
}