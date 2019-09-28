package org.green.cproc;

public interface Logger {

    boolean isTraceEnabled();

    boolean isErrorEnabled();

    void trace(CharSequence message);

    void trace(CharSequence message, Throwable t);

    void error(CharSequence message);

    void error(CharSequence message, Throwable t);

}
