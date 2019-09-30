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
                    errorHandler.onError(this, "An error while onAddProcessListener succeeded notification: "
                        + e.getLocalizedMessage(), e);
                }
            }
        } catch (final Exception e) {
            for (int i = 0; i < listeners.size(); i++) {
                try {
                    listeners.get(i).onAddProcessListener(executionId, this, listener, e);
                } catch (final Exception ee) {
                    errorHandler.onError(this, "An error while onAddProcessListener error notification: "
                        + e.getLocalizedMessage(), ee);
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
                    errorHandler.onError(this, "An error while tryRemoveListener succeeded notification: "
                        + e.getLocalizedMessage(), e);
                }
            }
        } catch (final Exception e) {
            for (int i = 0; i < listeners.size(); i++) {
                try {
                    listeners.get(i).onRemoveProcessListener(executionId, this, listener, e);
                } catch (final Exception ee) {
                    errorHandler.onError(this, "An error while tryRemoveListener error notification: "
                        + e.getLocalizedMessage(), ee);
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
                    errorHandler.onError(this, "An error while onStart succeeded notification: " + e.getLocalizedMessage(), e);
                }
            }
        } catch (final Exception e) {
            for (int i = 0; i < listeners.size(); i++) {
                try {
                    listeners.get(i).onStart(executionId, this, e);
                } catch (final Exception ee) {
                    errorHandler.onError(this, "An error while onStart error notification: " + e.getLocalizedMessage(), ee);
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
                    errorHandler.onError(this, "An error while onStop succeeded notification: " + e.getLocalizedMessage(), e);
                }
            }
        } catch (final Exception e) {
            for (int i = 0; i < listeners.size(); i++) {
                try {
                    listeners.get(i).onStop(executionId, this, e);
                } catch (final Exception ee) {
                    errorHandler.onError(this, "An error while onStop error notification: " + e.getLocalizedMessage(), ee);
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