package org.green.cproc;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JulLoggingErrorHandler implements ErrorHandler {
    private final Logger logger;

    public JulLoggingErrorHandler(final Logger logger) {
        this.logger = logger;
    }

    public JulLoggingErrorHandler(final Class forClass) {
        this.logger = Logger.getLogger(forClass.getName());
    }

    @Override
    public void onError(final Object in, final String message, final Throwable t) {
        logger.log(Level.SEVERE, '[' + in.toString() + "] " + message, t);
    }
}