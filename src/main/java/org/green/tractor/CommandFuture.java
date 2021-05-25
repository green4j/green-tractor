package org.green.tractor;

public interface CommandFuture<R extends Result> extends Future<R> {

    R result();

}