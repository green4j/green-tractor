package org.green;

public abstract class TestParameters {
    public static final boolean MAX_MODE = Boolean.getBoolean("org.green.test.max_mode");
    public static final int TEST_AMOUNT_OF_WORK_MULTIPLIER = MAX_MODE ? 20 : 1;
    public static final int CONCURRENCY_TEST_TIMEOUT_SECONDS = 20 * TEST_AMOUNT_OF_WORK_MULTIPLIER;
}
