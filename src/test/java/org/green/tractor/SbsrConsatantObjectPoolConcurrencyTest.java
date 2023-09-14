package org.green.tractor;

import org.green.TestParameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

class SbsrConsatantObjectPoolConcurrencyTest extends TestParameters {
    private static final long MINIMAL_TEST_TIME = 3_000;

    @Test
    void testPoolSize1() throws InterruptedException {
        testWithPoolSize(1, MINIMAL_TEST_TIME * TEST_AMOUNT_OF_WORK_MULTIPLIER);
    }

    @Test
    void testPoolSize2() throws InterruptedException {
        testWithPoolSize(2, MINIMAL_TEST_TIME * TEST_AMOUNT_OF_WORK_MULTIPLIER);
    }

    @Test
    void testPoolSize3() throws InterruptedException {
        testWithPoolSize(3, MINIMAL_TEST_TIME * TEST_AMOUNT_OF_WORK_MULTIPLIER);
    }

    @Test
    void testPoolSize100() throws InterruptedException {
        testWithPoolSize(100, MINIMAL_TEST_TIME * TEST_AMOUNT_OF_WORK_MULTIPLIER);
    }

    private void testWithPoolSize(final int poolSize, final long time) throws InterruptedException {
        final SbsrConsatantObjectPool<TestPoolableObject> pool
                = SbsrConsatantObjectPool.constructorBasedPool(TestPoolableObject.class, poolSize);

        final BlockingQueue<TestPoolableObject> queue = new ArrayBlockingQueue<>(pool.size() * 2);

        final Releaser r = new Releaser(queue);
        final Borrower b = new Borrower(pool, queue);

        r.start();
        b.start();

        Thread.sleep(time);

        b.interrupt();
        try {
            b.join();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
        r.interrupt();
        try {
            r.join();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (r.error != null) {
            r.error.printStackTrace();
        }
        Assertions.assertNull(r.error);

        if (b.error != null) {
            b.error.printStackTrace();
        }
        Assertions.assertNull(b.error);
    }

    public static class TestPoolableObject extends PoolableObject {
        private final AtomicLong refCounter = new AtomicLong();

        void onBorrow() {
            final long rc;
            if ((rc = refCounter.incrementAndGet()) > 1) {
                throw new IllegalStateException("Unexpected borrow (" + rc + ")");
            }
        }

        @Override
        void onReleased() {
            final long rc;
            if ((rc = refCounter.decrementAndGet()) < 0) {
                throw new IllegalStateException("Unexpected release (" + rc + ")");
            }
        }
    }

    private static final class Borrower extends Thread {
        private final SbsrConsatantObjectPool<TestPoolableObject> pool;
        private final BlockingQueue<TestPoolableObject> out;
        private volatile Exception error;

        private Borrower(final SbsrConsatantObjectPool<TestPoolableObject> pool,
                         final BlockingQueue<TestPoolableObject> out) {
            this.pool = pool;
            this.out = out;
        }

        public void run() {
            try {
                while (true) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    Thread.yield();
                    final TestPoolableObject o = pool.borrow();
                    o.onBorrow();
                    out.add(o);
                }
            } catch (final InterruptedException e) {
            } catch (final Exception e) {
                error = e;
            }
        }
    }

    private static final class Releaser extends Thread {
        private final BlockingQueue<TestPoolableObject> in;
        private volatile Exception error;

        private Releaser(final BlockingQueue<TestPoolableObject> in) {
            this.in = in;
        }

        public void run() {
            try {
                while (true) {
                    final TestPoolableObject o = in.take();
                    o.owner().release(o);
                    Thread.yield();
                }
            } catch (final InterruptedException e) {
            } catch (final Exception e) {
                error = e;
            }
        }
    }
}