package org.green.cproc;

public abstract class PoolableObject {
    private ObjectPool owner;

    private int usageCounter = 0;

    void setOwner(final ObjectPool owner) {
        this.owner = owner;
    }

    void borrow() {
        if (usageCounter != 0) {
            throw new IllegalStateException("Potential leak detected. The object "
                + this + " was not released correctly");
        }
        usageCounter++;
    }

    void release() {
        usageCounter--;
        if (usageCounter != 0) {
            throw new IllegalStateException("Potential leak detected. The object "
                + this + " was not borrow correctly");
        }
        owner.release(this);
    }
}