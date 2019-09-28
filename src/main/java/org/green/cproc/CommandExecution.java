package org.green.cproc;

public interface CommandExecution<C> extends Execution {
    C command();
}