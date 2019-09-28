package org.green.cproc;

import java.util.logging.Level;

public class JulLogger implements Logger {
    private final java.util.logging.Logger logger;

    public JulLogger(final Class forClass) {
        this(java.util.logging.Logger.getLogger(forClass.getName()));
    }

    public JulLogger(final java.util.logging.Logger logger) {
        this.logger = logger;
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isLoggable(Level.FINEST);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isLoggable(Level.SEVERE);
    }

    @Override
    public void trace(final CharSequence message) {
        logger.finest(message.toString());
    }

    @Override
    public void trace(final CharSequence message, final Throwable t) {
        logger.log(Level.FINEST, message.toString(), t);
    }

    @Override
    public void error(final CharSequence message) {
        logger.severe(message.toString());
    }

    @Override
    public void error(final CharSequence message, final Throwable t) {
        logger.log(Level.SEVERE, message.toString(), t);
    }
}