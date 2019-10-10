package org.green.cproc;

public interface CommandExecution<R extends Result> extends Execution<R> {

    R result();

}