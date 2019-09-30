package org.green.cproc.example;

import org.green.cab.CabBlocking;
import org.green.cproc.CommandExecution;
import org.green.cproc.DefaultConcurrentProcess;
import org.green.cproc.EntryEnvelope;
import org.green.cproc.EntrySender;
import org.green.cproc.Execution;

public class MyProcess extends DefaultConcurrentProcess<MyEntry, MyExecutor, MyProcessListener> {
    public MyProcess(final String name) {
        super(new CabBlocking(100), new MyExecutor(name));
    }

    public Execution sum(final int a, final int b) {
        final CommandExecution<MySumCommand> result = prepareCommandExecution(MySumCommand.class);
        result.command().setA(a);
        result.command().setB(b);
        return result;
    }

    public Execution multiply(final int a, final int b) {
        final CommandExecution<MyMultiplyCommand> result = prepareCommandExecution(MyMultiplyCommand.class);
        result.command().setA(a);
        result.command().setB(b);
        return result;
    }

    public static void main(final String[] args) throws Exception {
        final MyProcess process = new MyProcess("My process");

        process.addListener(new MyProcessListener()).executeSync();

        process.start().executeSync();

        final EntrySender<MyEntry> entrySender = process.newEntrySender(MyEntry.class);

        EntryEnvelope<MyEntry> envelope = entrySender.nextEnvelope();
        envelope.entry().value = 100L;
        envelope.send();

        envelope = entrySender.nextEnvelope();
        envelope.entry().value = 200L;
        envelope.send();

        process.sum(1, 2).executeSync();

        process.multiply(3, 4).executeSync();

        process.stop().executeSync();

        process.close();
    }
}