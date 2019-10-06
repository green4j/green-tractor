package org.green.cproc;

public abstract class PoolableObject {
    private MbsrConsatantObjectPool owner;

    void setOwner(final MbsrConsatantObjectPool owner) {
        if (this.owner != null && this.owner != owner) {
            throw new IllegalArgumentException("Owner cannot be changed");
        }
        this.owner = owner;
    }

    MbsrConsatantObjectPool owner() {
        return owner;
    }

    void onReleased() {
    }
}