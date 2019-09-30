package org.green.cproc;

public interface ErrorHandler {

    void onError(Object in, String message, Throwable t);

}