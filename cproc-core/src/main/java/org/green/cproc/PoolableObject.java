package org.green.cproc;

public abstract class PoolableObject {
    private ObjectPool owner;
    private long usageCounter;

    void setOwner(final ObjectPool owner) {
        if (this.owner != null && this.owner != owner) {
            throw new IllegalArgumentException("Owner cannot be changed");
        }
        this.owner = owner;
    }

    ObjectPool owner() {
        return owner;
    }

    void onBorrowed() {
        if (usageCounter != 0) {
            throw new IllegalStateException("Potential leak detected. The object "
                + this + " was not released correctly");
        }
        usageCounter++;
    }

    void onReleased() {
        usageCounter--;
        if (usageCounter != 0) {
            throw new IllegalStateException("Potential leak detected. The object "
                + this + " was not borrow correctly");
        }
    }
}