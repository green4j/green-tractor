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

import org.green.cproc.Command;
import org.green.cproc.DefaultExecutor;

import java.util.List;

public class MyExecutor extends DefaultExecutor<MyEntry, MyProcessListener> {
    public MyExecutor(final String name) {
        super(name);
    }

    @Override
    public void processEntry(final MyEntry entry) {
        System.out.println("My Executor: Processing " + entry);
    }

    @Override
    protected void doStart() {
        System.out.println("My Executor: Start");
    }

    @Override
    protected void doStop() {
        System.out.println("My Executor: Stop");
    }

    @Override
    protected void doCustom(final long executionId, final Command command, final List<MyProcessListener> listeners) {
        System.out.println("My Executor: " + command);

        if (command instanceof MySumCommand) {
            final MySumCommand myCommand = (MySumCommand) command;
            final int result = myCommand.a() + myCommand.b();

            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).onSum(executionId, this, result, null);
            }
            return;
        }

        if (command instanceof MyMultiplyCommand) {
            final MyMultiplyCommand myCommand = (MyMultiplyCommand) command;
            final int result = myCommand.a() * myCommand.b();

            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).onMultiply(executionId, this, result, null);
            }
            return;
        }

        throw new UnsupportedOperationException("Unknown command: " + command);
    }
}