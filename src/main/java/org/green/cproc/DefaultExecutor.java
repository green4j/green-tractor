package org.green.cproc;

import java.util.ArrayList;
import java.util.List;

public class DefaultExecutor<E extends Entry, L extends ConcurrentProcessListener> implements Executor<E> {
    private final List<L> listeners = new ArrayList<>();

    private final String name;

    public DefaultExecutor(final String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public final void executeCommand(final long executionId, final Command command) {
        if (tryAddListener(executionId, command)) {
            return;
        }

        if (tryRemoveListener(executionId, command)) {
            return;
        }

        if (tryStart(executionId, command)) {
            return;
        }

        if (tryStop(executionId, command)) {
            return;
        }

        doCustom(executionId, command, listeners);
    }

    @SuppressWarnings("unchecked")
    private boolean tryAddListener(final long executionId, final Command command) {
        if (!(command instanceof AddListener)) {
            return false;
        }

        final AddListener addListener = (AddListener) command;
        final L listener = (L) addListener.listener();

        try {
            listeners.add(listener);

            for (int i = 0; i < listeners.size(); i++) {
                try {
                    listeners.get(i).onAddProcessListener(executionId, this, listener, null);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (final Exception e) {
            for (int i = 0; i < listeners.size(); i++) {
                try {
                    listeners.get(i).onAddProcessListener(executionId, this, listener, e);
                } catch (final Exception ee) {
                    ee.printStackTrace();
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean tryRemoveListener(final long executionId, final Command command) {
        if (!(command instanceof RemoveListener)) {
            return false;
        }

        final RemoveListener removeListener = (RemoveListener) command;
        final L listener = (L) removeListener.listener();

        try {
            listeners.remove(listener);

            for (int i = 0; i < listeners.size(); i++) {
                try {
                    listeners.get(i).onRemoveProcessListener(executionId, this, listener, null);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (final Exception e) {
            for (int i = 0; i < listeners.size(); i++) {
                try {
                    listeners.get(i).onRemoveProcessListener(executionId, this, listener, e);
                } catch (final Exception ee) {
                    ee.printStackTrace();
                }
            }
        }
        return true;
    }

    private boolean tryStart(final long executionId, final Command command) {
        if (!(command instanceof Start)) {
            return false;
        }

        try {
            doStart();

            for (int i = 0; i < listeners.size(); i++) {
                try {
                    listeners.get(i).onStart(executionId, this, null);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (final Exception e) {
            for (int i = 0; i < listeners.size(); i++) {
                try {
                    listeners.get(i).onStart(executionId, this, e);
                } catch (final Exception ee) {
                    ee.printStackTrace();
                }
            }
        }
        return true;
    }

    private boolean tryStop(final long executionId, final Command command) {
        if (!(command instanceof Stop)) {
            return false;
        }

        try {
            doStop();

            for (int i = 0; i < listeners.size(); i++) {
                try {
                    listeners.get(i).onStop(executionId, this, null);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (final Exception e) {
            for (int i = 0; i < listeners.size(); i++) {
                try {
                    listeners.get(i).onStop(executionId, this, e);
                } catch (final Exception ee) {
                    ee.printStackTrace();
                }
            }
        }
        return true;
    }

    @Override
    public void processEntry(final E entry) {
    }

    protected void doStart() {
    }

    protected void doStop() {
    }

    protected void doCustom(final long executionId, final Command command, final List<L> listeners) {
    }
}