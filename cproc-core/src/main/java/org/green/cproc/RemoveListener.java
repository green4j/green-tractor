package org.green.cproc;

class RemoveListener extends Command {
    private ConcurrentProcessListener listener;

    RemoveListener() {
    }

    void setListener(final ConcurrentProcessListener listener) {
        this.listener = listener;
    }

    @Override
    void onReleased() {
        this.listener = null; // forget the listener to make it available for GC
        // while the command is still in the pool
        super.onReleased();
    }

    public ConcurrentProcessListener listener() {
        return listener;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "listener=" + listener + '}';
    }
}