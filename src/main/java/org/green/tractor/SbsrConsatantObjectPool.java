/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2023 Anatoly Gudkov
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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import static org.green.cab.Utils.ARRAY_PAD;
import static org.green.cab.Utils.OBJECT_ARRAY_HANDLE;

abstract class SbsrConsatantObjectPoolPad0 {
    protected long p01, p02, p03, p04, p05, p06, p07;
    protected long p08, p09, p010, p011, p012, p013, p014, p015;
}

abstract class LastAvailableObjectIndex extends SbsrConsatantObjectPoolPad0 {
    protected static final AtomicIntegerFieldUpdater<LastAvailableObjectIndex> LAST_AVAILABLE_OBJECT_INDEX_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(LastAvailableObjectIndex.class, "lastAvailableObjectIndex");

    protected volatile int lastAvailableObjectIndex;
}

abstract class SbsrConsatantObjectPoolPad1 extends LastAvailableObjectIndex {
    protected long p11, p12, p13, p14, p15, p16, p17;
    protected long p18, p19, p110, p111, p112, p113, p114, p115;
}

public class SbsrConsatantObjectPool<O extends PoolableObject> extends SbsrConsatantObjectPoolPad1 {

    @SuppressWarnings("unchecked")
    public static <O extends PoolableObject> SbsrConsatantObjectPool<O> constructorBasedPool(
            final Class<O> objectClass,
            final int size) {

        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        final MethodType constructorType = MethodType.methodType(void.class);
        final MethodHandle objectConstructor;
        try {
            objectConstructor = lookup.findConstructor(objectClass, constructorType);
        } catch (final Exception e) {
            throw new RuntimeException("Cannot find default constructor: " + constructorType, e);
        }
        return new SbsrConsatantObjectPool<>(size, () -> {
            try {
                return (O) objectConstructor.invoke(); // unchecked
            } catch (final Throwable t) {
                throw new RuntimeException("Cannot create instance of " + objectClass, t);
            }
        });
    }

    private final int size;
    private final Object[] objects;

    @SuppressWarnings("unchecked")
    public SbsrConsatantObjectPool(final int size, final Supplier<O> supplier) {
        this.size = size;

        this.objects = new Object[size + 2 * ARRAY_PAD];

        for (int i = 0; i < size; i++) {
            final O object = supplier.get();
            object.setOwner((SbsrConsatantObjectPool<PoolableObject>) this); // unchecked
            OBJECT_ARRAY_HANDLE.setVolatile(objects, objectIndex(i), object);
        }

        LAST_AVAILABLE_OBJECT_INDEX_UPDATER.set(this, size - 1);
    }

    public int size() {
        return size;
    }

    @SuppressWarnings("unchecked")
    public O borrow() throws InterruptedException {
        Object result;
        int v;

        do {
            v = LAST_AVAILABLE_OBJECT_INDEX_UPDATER.get(this);

            while (v == -1) { // the pool is empty, this is not typical
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                LockSupport.parkNanos(1); // so, let's give a good chance to the releaser
                v = LAST_AVAILABLE_OBJECT_INDEX_UPDATER.get(this);
            }

            result = OBJECT_ARRAY_HANDLE.get(objects, objectIndex(v));

        } while (!LAST_AVAILABLE_OBJECT_INDEX_UPDATER.compareAndSet(this, v, v - 1));

        return (O) result;
    }

    public void release(final O object) {
        try {
            object.onReleased();
        } finally {
            int v;

            do {
                v = LAST_AVAILABLE_OBJECT_INDEX_UPDATER.get(this) + 1;

                if (v == size) {
                    throw new IllegalStateException("The pool is full already");
                }

                OBJECT_ARRAY_HANDLE.setVolatile(objects, objectIndex(v), object);

            } while (!LAST_AVAILABLE_OBJECT_INDEX_UPDATER.compareAndSet(this, v - 1, v));
        }
    }

    private static int objectIndex(final int index) {
        return ARRAY_PAD + index;
    }
}
