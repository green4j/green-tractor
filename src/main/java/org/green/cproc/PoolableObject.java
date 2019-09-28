package org.green.cproc;

public abstract class PoolableObject {
    private ObjectPool owner;

    void setOwner(final ObjectPool owner) {
        this.owner = owner;
    }

    void release() {
        owner.release(this);
    }
}
