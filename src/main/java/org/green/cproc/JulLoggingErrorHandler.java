package org.green.cproc;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JulLoggingErrorHandler implements ErrorHandler {
    private final Logger logger;

    public JulLoggingErrorHandler(Logger logger) {
        this.logger = logger;
    }

    public JulLoggingErrorHandler(Class forClass) {
        this.logger = Logger.getLogger(forClass.getName());
    }

    @Override
    public void onError(Object in, String message, Throwable t) {
        logger.log(Level.SEVERE, '[' + in.toString() + "] " + message, t);
    }
}