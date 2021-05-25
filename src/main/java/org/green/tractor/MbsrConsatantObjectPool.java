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
package org.green.tractor;

import org.green.cab.Utils;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import static org.green.cab.Utils.CACHE_LINE_SIZE;

abstract class MbsrConsatantObjectPoolPad0 {
    protected long p01, p02, p03, p04, p05, p06, p07;
    protected long p08, p09, p010, p011, p012, p013, p014, p015;
}

abstract class LastAvailableObjectIndex extends MbsrConsatantObjectPoolPad0 {
    protected volatile int lastAvailableObjectIndex;
}

abstract class MbsrConsatantObjectPoolPad1 extends LastAvailableObjectIndex {
    protected long p11, p12, p13, p14, p15, p16, p17;
    protected long p18, p19, p110, p111, p112, p113, p114, p115;
}

public class MbsrConsatantObjectPool<O extends PoolableObject> extends MbsrConsatantObjectPoolPad1 {
    private static final Unsafe UNSAFE = Utils.getUnsafe();

    private static final int OBJECT_ARRAY_ELEMENT_SHIFT;
    private static final int OBJECT_ARRAY_PAD;
    private static final long OBJECT_ARRAY_BASE;

    private static final long LAST_AVAILABLE_OBJECT_INDEX_OFFSET;

    static {
        final int scale = UNSAFE.arrayIndexScale(Object[].class);
        if (4 == scale) {
            OBJECT_ARRAY_ELEMENT_SHIFT = 2;
        } else if (8 == scale) {
            OBJECT_ARRAY_ELEMENT_SHIFT = 3;
        } else {
            throw new IllegalStateException("Unexpected element's size");
        }
        OBJECT_ARRAY_PAD = CACHE_LINE_SIZE * 2 / scale;
        OBJECT_ARRAY_BASE = UNSAFE.arrayBaseOffset(Object[].class) + (OBJECT_ARRAY_PAD * scale);

        try {
            LAST_AVAILABLE_OBJECT_INDEX_OFFSET = UNSAFE.objectFieldOffset(
                    LastAvailableObjectIndex.class.getDeclaredField("lastAvailableObjectIndex"));
        } catch (final Exception e) {
            throw new Error(e);
        }
    }

    public static <O extends PoolableObject> MbsrConsatantObjectPool<O> constructorBasedPool(
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
        return new MbsrConsatantObjectPool(size, () -> {
            try {
                return (O) objectConstructor.invoke();
            } catch (final Throwable t) {
                throw new RuntimeException("Cannot create instance of " + objectClass, t);
            }
        });
    }

    private final int size;
    private final Object[] objects;

    public MbsrConsatantObjectPool(final int size, final Supplier<O> supplier) {
        this.size = size;

        this.objects = new Object[size + 2 * OBJECT_ARRAY_PAD];

        for (int i = 0; i < size; i++) {
            final O object = supplier.get();
            object.setOwner(this);
            UNSAFE.putObject(objects, objectAddress(i), object); // membars required to publish the object are below
        }

        UNSAFE.putIntVolatile(this, LAST_AVAILABLE_OBJECT_INDEX_OFFSET, size - 1); // volatile write
        // leads to <membar StoreStore|StoreLoad> (as well as the freeze action to LoadStore|StoreStore)
    }

    @SuppressWarnings("unchecked")
    public O borrow() {
        Object result;
        int v;

        do {
            v = UNSAFE.getIntVolatile(this, LAST_AVAILABLE_OBJECT_INDEX_OFFSET); // volatile read leads to
            // <membar LoadLoad|LoadStore>

            while (v == -1) { // the pool is empty, this is not typical
                LockSupport.parkNanos(1); // so, let's give a good chance to the releaser
                v = UNSAFE.getIntVolatile(this, LAST_AVAILABLE_OBJECT_INDEX_OFFSET);
            }

            result = UNSAFE.getObject(objects, objectAddress(v)); // normal read volatile read

        } while (!UNSAFE.compareAndSwapInt(this, LAST_AVAILABLE_OBJECT_INDEX_OFFSET, v, v - 1)); // strong CAS
        // leads to <membar StoreLoad|StoreStore>

        return (O) result;
    }

    public void release(final O object) {
        int v;

        object.onReleased(); // membars required to publish changes are below

        do {
            v = UNSAFE.getIntVolatile(this, LAST_AVAILABLE_OBJECT_INDEX_OFFSET) + 1; // volatile read leads to
            // <membar LoadLoad|LoadStore>

            if (v == size) {
                throw new IllegalStateException("The pool is full already");
            }

            UNSAFE.putObject(objects, objectAddress(v), object); // normal write before strong CAS

        } while (!UNSAFE.compareAndSwapInt(this, LAST_AVAILABLE_OBJECT_INDEX_OFFSET, v - 1, v)); // strong CAS
        // leads to <membar StoreLoad|StoreStore>
    }

    private long objectAddress(final int index) {
        return OBJECT_ARRAY_BASE + (index << OBJECT_ARRAY_ELEMENT_SHIFT);
    }
}
