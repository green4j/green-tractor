package org.green.cproc;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ObjectPool<O extends PoolableObject> {

    public static <O extends PoolableObject> ObjectPool<O> constructorBasedPool(final Class<O> objectClass) {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        final MethodType constructorType = MethodType.methodType(void.class);
        final MethodHandle objectConstructor;
        try {
            objectConstructor = lookup.findConstructor(objectClass, constructorType);
        } catch (final Exception e) {
            throw new RuntimeException("Cannot find default constructor: " + constructorType, e);
        }
        return new ObjectPool(() -> {
            try {
                return (O) objectConstructor.invoke();
            } catch (final Throwable t) {
                throw new RuntimeException("Cannot create instance of " + objectClass, t);
            }
        });
    }

    private final List<O> availableObjects = new ArrayList<>();

    private final Supplier<O> objectSupplier;

    private int lastAvailableObjectIndex;

    public ObjectPool(final Supplier<O> objectSupplier) {
        this(objectSupplier, 2);
    }

    public ObjectPool(final Supplier<O> objectSupplier, final int initialSize) {
        this.objectSupplier = objectSupplier;

        for (int i = 0; i < initialSize; i++) {
            availableObjects.add(newObject());
        }

        lastAvailableObjectIndex = availableObjects.size() - 1;
    }

    public O borrow() {
        final O result;
        if (lastAvailableObjectIndex < 0) {
            result = newObject();
        } else {
            result = availableObjects.get(lastAvailableObjectIndex--);
        }
        result.borrow();
        return result;
    }

    public void release(final O command) {
        if (++lastAvailableObjectIndex >= availableObjects.size()) {
            availableObjects.add(command);
            return;
        }
        availableObjects.set(lastAvailableObjectIndex, command);
    }

    private O newObject() {
        final O result = objectSupplier.get();
        result.setOwner(this);
        return result;
    }
}