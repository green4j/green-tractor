package org.green.cproc;

public interface CommandFuture<R extends Result> extends Future<R> {

    R result();

}