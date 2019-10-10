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

import java.util.ArrayList;
import java.util.List;

public class DefaultExecutor<E extends Entry, L extends ConcurrentProcessListener> implements Executor<E> {
    private final List<L> listeners = new ArrayList<>();

    private final String name;

    protected final ErrorHandler errorHandler;

    public DefaultExecutor(final String name) {
        this(name, new JulLoggingErrorHandler(DefaultExecutor.class));
    }

    public DefaultExecutor(final String name, final ErrorHandler errorHandler) {
        this.name = name;
        this.errorHandler = errorHandler;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void processEntry(final E entry) {
    }

    @Override
    public final void executeCommand(final Command command) {
        if (tryAddListener(command)) {
            return;
        }

        if (tryRemoveListener(command)) {
            return;
        }

        if (tryStart(command)) {
            return;
        }

        if (tryStop(command)) {
            return;
        }

        doCustom(command, listeners);
    }

    @SuppressWarnings("unchecked")
    private boolean tryAddListener(final Command command) {
        if (!(command instanceof AddListener)) {
            return false;
        }

        final AddListener addListener = (AddListener) command;
        final L listener = (L) addListener.listener();

        listeners.add(listener);

        for (int i = 0; i < listeners.size(); i++) {
            try {
                listeners.get(i).onAddProcessListener(this, addListener.result());
            } catch (final Exception e) {
                errorHandler.onError(this,
                        "An error while onAddProcessListener succeeded notification: " +
                                e.getLocalizedMessage(), e);
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean tryRemoveListener(final Command command) {
        if (!(command instanceof RemoveListener)) {
            return false;
        }

        final RemoveListener removeListener = (RemoveListener) command;
        final L listener = (L) removeListener.listener();

        for (int i = 0; i < listeners.size(); i++) {
            try {
                listeners.get(i).onRemoveProcessListener(this, removeListener.result());
            } catch (final Exception e) {
                errorHandler.onError(this,
                        "An error while tryRemoveListener succeeded notification: " +
                                e.getLocalizedMessage(), e);
            }
        }

        listeners.remove(listener);

        return true;
    }

    private boolean tryStart(final Command command) {
        if (!(command instanceof Start)) {
            return false;
        }

        final VoidResult result = ((Start) command).result();

        try {
            doStart();
        } catch (final Exception e) {
            applyError(result, e);
        }

        for (int i = 0; i < listeners.size(); i++) {
            try {
                listeners.get(i).onStart(this, result);
            } catch (final Exception e) {
                errorHandler.onError(this, "An error while onStart notification: " +
                        e.getLocalizedMessage(), e);
            }
        }

        return true;
    }

    private boolean tryStop(final Command command) {
        if (!(command instanceof Stop)) {
            return false;
        }

        final VoidResult result = ((Stop) command).result();

        try {
            doStop();
        } catch (final Exception e) {
            applyError(result, e);
        }

        for (int i = 0; i < listeners.size(); i++) {
            try {
                listeners.get(i).onStop(this, result);
            } catch (final Exception e) {
                errorHandler.onError(this, "An error while onStop notification: " +
                        e.getLocalizedMessage(), e);
            }
        }

        return true;
    }

    protected final void applyError(final ErrorableResult result, final Exception error) {
        result.setError(error);
    }

    protected void doStart() {
    }

    protected void doStop() {
    }

    protected void doCustom(final Command command, final List<L> listeners) {
    }
}